(ns hostage.state)

(defonce ^:dynamic *debug* false)
(defonce ^:dynamic *dry-run?* false)
(defonce ^:dynamic *env* nil)
(defonce ^:dynamic *task-stack* nil)
