(ns hostage.coll)

(defn lazier-map [f coll]
  ; NOTE: clojure.core/map does some batching based on chunked-seq?
  ; Use this if your f is doing something very expensive and you don't
  ; expect to mostly be doing (first) on the resulting sequence
  (lazy-seq
   (when-let [s (seq coll)]
     (cons (f (first s))
           (lazier-map f (rest s))))))
