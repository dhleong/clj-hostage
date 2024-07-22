(ns hostage.state)

(defonce ^:dynamic *debug* false)
(defonce ^:dynamic *disabled-tags* #{})
(defonce ^:dynamic *dry-run?* false)
(defonce ^:dynamic *execution-state* nil)
(defonce ^:dynamic *reporter* nil)

