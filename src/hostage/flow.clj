(ns hostage.flow
  (:require
   [babashka.process :as process]
   [clojure.string :as str]
   [hostage.expect :refer [assertion-message]]
   [hostage.reporter.append-only :as append-only]
   [hostage.reporter.proto :as reporter]
   [hostage.state :refer [*debug* *disabled-tags* *dry-run?*
                          *execution-state* *reporter*]]))

(declare run-step)

(def default-execution-state {::cleanup-tasks []})

(defn enqueue-cleanup-task [f]
  (when-let [state-atom *execution-state*]
    (swap! state-atom update ::cleanup-tasks conj f)))

(defn perform-cleanup [execution-state]
  (swap! execution-state
         (fn [{::keys [cleanup-tasks] :as state}]
           (when (seq cleanup-tasks)
             (run-step {:name "Cleanup!"}
                       (fn []
                         (doseq [task cleanup-tasks]
                           (task)))))

           (update state dissoc ::cleanup-tasks))))

(defn extract-disabled-tags [args]
  (let [prefix "--skip-"]
    (->> args
         (filter #(str/starts-with? % prefix))
         (map #(keyword (subs % (count prefix))))
         (into #{}))))

(defn extract-reporter [_args]
  ; TODO: Other reporters?
  (append-only/create *out* *err*))

(defmacro execute [opts & body]
  `(binding [*debug* (:debug ~opts)
             *disabled-tags* (:disabled-tags ~opts)
             *dry-run?* (:dry-run? ~opts)
             *execution-state* (atom default-execution-state)
             *reporter* (:reporter ~opts)]
     (try
       (reporter/begin *reporter*)

       ~@body

       (reporter/end *reporter*)

       (perform-cleanup *execution-state*)
       (catch Throwable e#
         (let [msg# (assertion-message e#)]
           (if (and msg# (not *debug*))
             (println "[ERROR] " msg#)
             (.printStackTrace e#))
           (System/exit 1))))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro main [& body]
  `(do (defn ~'-main [& args#]
         (let [opts# {:debug (some #{"--debug"} args#)
                      :dry-run? (when (some #{"--dry-run"} args#)
                                  "dry-run")
                      :disabled-tags (extract-disabled-tags args#)
                      :reporter (extract-reporter args#)}]
           (execute opts# ~@body)))

       (when (= *file* (System/getProperty "babashka.file"))
         (apply ~'-main *command-line-args*))))

(defn run-step [{:keys [always-run? tag] :as opts} f]
  (let [skip-reason (or (when (and *dry-run?* (not always-run?))
                          "dry-run")
                        (*disabled-tags* tag))]
    (binding [*dry-run?* skip-reason]
      (reporter/begin-step *reporter* opts)
      (f)
      (reporter/end-step *reporter* opts))))

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
