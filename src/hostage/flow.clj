(ns hostage.flow
  (:require
   [babashka.process :as process]
   [clojure.string :as str]))

(defonce ^:dynamic *debug* false)
(defonce ^:dynamic *disabled-tags* #{})
(defonce ^:dynamic *dry-run?* false)

(defn extract-disabled-tags [args]
  (let [prefix "--skip-"]
    (->> args
         (filter #(str/starts-with? % prefix))
         (map #(keyword (subs % (count prefix))))
         (into #{}))))

(defmacro execute [opts & body]
  `(binding [*debug* (:debug ~opts)
             *disabled-tags* (:disabled-tags ~opts)
             *dry-run?* (:dry-run? ~opts)]
     (try
       ~@body
       (catch Throwable e#
         (let [msg# (ex-message e#)]
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
                      :disabled-tags (extract-disabled-tags args#)}]
           (execute opts# ~@body)))

       (when (= *file* (System/getProperty "babashka.file"))
         (apply ~'-main *command-line-args*))))

(defn run-step [{:keys [always-run? name tag]} f]
  (let [skip-reason (or (when (and *dry-run?* (not always-run?))
                          "dry-run")
                        (*disabled-tags* tag))]
    (binding [*dry-run?* skip-reason]
      (if-not skip-reason
        (println "[run]" name)
        (println (str "[skip - " skip-reason "]") name))
      (f))))

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
  (println
   (if *dry-run?*
     (str "[skip - " *dry-run?* "]")
     "[run]")
   "shell: " (->> (into [command] args)
                  (remove map?)))
  (when-not *dry-run?*
    (apply process/shell command args)))
