(ns hostage.expect)

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

(defn >some? [v error-message]
  (if (some? v)
    v

    (throw (assertion-error error-message {:value v}))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn >>some? [error-message v]
  (>some? v error-message))

(defn truthy? [v error-message]
  (when-not v
    (throw (assertion-error error-message))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn falsey? [v error-message]
  (truthy? (not v) error-message))
