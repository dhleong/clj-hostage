(ns hostage.expect
  (:require
   [hostage.state :refer [*env*]]))

(defn assertion-error
  ([message] (assertion-error message {}))
  ([message data]
   (ex-info message (merge data {::isa? :assertion-error}))))

(defn assertion-error? [v]
  (= (::isa? (ex-data v))
     :assertion-error))

(defn assertion-message [v]
  (when (assertion-error? v)
    (ex-message v)))

(defmacro ^:private maybe-warn [& body]
  `(try
     ~@body
     (catch clojure.lang.ExceptionInfo e
       (if (and (:warn-on-expect? *env*)
                (assertion-error? e))
         (println "[warn]" (assertion-message e))
         (throw e)))))

(defn >some? [v error-message]
  (maybe-warn
   (if (some? v)
     v

     (throw (assertion-error error-message {:value v})))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn >>some? [error-message v]
  (>some? v error-message))

(defn truthy? [v error-message]
  (maybe-warn
   (when-not v
     (throw (assertion-error error-message)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn falsey? [v error-message]
  (truthy? (not v) error-message))
