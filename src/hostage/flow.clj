(ns hostage.flow
  (:require [babashka.process :as process]))

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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-step [name-or-opts & body]
  (let [opts (if (map? name-or-opts)
               name-or-opts
               {:name name-or-opts})]
    ; TODO: Support skipping by :tag
    `(if (or (not *dry-run?*)
             ~(:always-run? opts))
       (do (println "[run]" ~(:name opts))
           ~@body)
       (println "[skip - dry run]" ~(:name opts)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-step-always [step-name & body]
  `(with-step {:name ~step-name :always-run? true}
     ~@body))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn shell [command & args]
  (apply process/shell command args))
