(ns hostage.flow
  (:require
   [babashka.cli :as cli]
   [babashka.process :as process]
   [clojure.string :as str]
   [hostage.expect :refer [assertion-message]]
   [hostage.reporter.append-only :as append-only]
   [hostage.reporter.proto :as reporter]
   [hostage.state :refer [*debug* *dry-run?* *env*]]))

(declare run-step)

(def default-execution-state {::cleanup-tasks []
                              ::summaries []})

(defn enqueue-cleanup-task [f]
  (when-let [state-atom (::state *env*)]
    (swap! state-atom update ::cleanup-tasks conj f)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn summary [& parts]
  (when-let [state-atom (::state *env*)]
    (swap! state-atom update ::summaries conj parts)))

(defn perform-cleanup
  ([] (perform-cleanup (::state *env*)))
  ([execution-state]
   (swap! execution-state
          (fn [{::keys [cleanup-tasks] :as state}]
            (when (seq cleanup-tasks)
              (run-step {:name "Cleanup!"}
                        (fn []
                          (doseq [task cleanup-tasks]
                            (task)))))

            (update state dissoc ::cleanup-tasks)))))

(defn perform-summary
  ([] (perform-summary (::state *env*)))
  ([execution-state]
   (swap! execution-state
          (fn [{::keys [summaries] :as state}]
            (when (seq summaries)
              (doseq [summary summaries]
                (apply println summary)))

            (update state dissoc ::summaries)))))

(defn extract-disabled-tags [args]
  (let [prefix "--skip-"]
    (->> args
         (filter #(str/starts-with? % prefix))
         (map #(keyword (subs % (count prefix))))
         (into #{}))))

(defn extract-allowed-steps [args]
  (let [{:keys [allow]} (cli/parse-opts args {:coerce {:allow []}})]
    (when (seq allow)
      (into #{} allow))))

(defn extract-reporter [_args]
  ; TODO: Other reporters?
  (append-only/create *out* *err*))

(defmacro execute [opts & body]
  `(binding [*env* ~opts
             *debug* (:debug ~opts)
             *dry-run?* (:dry-run? ~opts)]
     (try
       (reporter/begin (:reporter *env*))

       ~@body

       (perform-cleanup)
       (reporter/end (:reporter *env*))

       (perform-summary)
       (catch Throwable e#
         (let [msg# (assertion-message e#)]
           (if (and msg# (not *debug*))
             (println "[ERROR] " msg#)
             (.printStackTrace e#))
           (System/exit 1))))))

(defn build-env-from-args [args]
  {:debug (some #{"--debug"} args)
   :dry-run? (when (some #{"--dry-run"} args)
               "dry-run")
   :disabled-tags (extract-disabled-tags args)
   :allowed-steps (extract-allowed-steps args)
   :reporter (extract-reporter args)
   :warn-on-expect? (some #{"--warn-on-expect"} args)
   ::state (atom default-execution-state)})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro main [& body]
  `(do (defn ~'-main [& args#]
         (let [env# (build-env-from-args args#)]
           (execute env# ~@body)))

       (when (= *file* (System/getProperty "babashka.file"))
         (apply ~'-main *command-line-args*))))

(defn run-step [{:keys [always-run? tag] :as opts} f]
  (let [skip-reason (or (when (and *dry-run?* (not always-run?))
                          "dry-run")
                        ((:disabled-tags *env*) tag)
                        (when (:allowed-steps *env*)
                          (when-not ((:allowed-steps *env*) (:name tag))
                            (str "Not in " (:allowed-steps *env*)))))]
    (binding [*dry-run?* skip-reason]
      (reporter/begin-step (:reporter *env*) opts)
      (f)
      (reporter/end-step (:reporter *env*) opts))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-step [name-or-opts & body]
  (let [opts (if (map? name-or-opts)
               name-or-opts
               {:name name-or-opts})]
    `(run-step ~opts (fn [] ~@body))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-step-always [step-name & body]
  `(with-step {:name ~step-name :always-run? true}
     ~@body))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn shell [command & args]
  (let [opts {:name (str "shell: " (->> (into [command] args)
                                        (remove map?)
                                        (seq)))}]
    (run-step opts
              #(when-not *dry-run?*
                 (apply process/shell command args)))))
