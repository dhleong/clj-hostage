(ns hostage.flow
  (:require
   [babashka.process :as process]
   [clojure.string :as str]))

(defonce ^:dynamic *dry-run?* false)
(defonce ^:dynamic *debug* false)

(defmacro execute [opts & body]
  `(binding [*dry-run?* (:dry-run? ~opts)
             *debug* (:debug ~opts)]
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
         (let [opts# {:dry-run? (some #{"--dry-run"} args#)
                      :debug (some #{"--debug"} args#)}]
           (execute opts# ~@body)))

       (when (= *file* (System/getProperty "babashka.file"))
         (apply ~'-main *command-line-args*))))

(defn run-step [{:keys [always-run? name]} f]
  ; TODO: Support skipping by :tag
  (let [should-run? (or (not *dry-run?*)
                        always-run?)]
    (binding [*dry-run?* (not should-run?)]
      (if should-run?
        (println "[run]" name)
        (println "[skip - dry run]" name))
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
     "[skip - dry-run]"
     "[run]")
   "shell: " (->> (into [command] args)
                  (remove map?)))
  (when-not *dry-run?*
    (apply process/shell command args)))
