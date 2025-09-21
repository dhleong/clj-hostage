(ns hostage.util.base64
  (:import (java.util Base64)))

(defn str->base64 [^String s]
  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder (.getBytes s "utf-8"))))
