(ns hostage.reporter.append-only
  (:require
   [clojure.string :as str]
   [hostage.reporter.proto :refer [Reporter]]
   [hostage.state :refer [*dry-run?*]]))

(defn- indent [amount s]
  (str (str/join " " (repeat amount " ")) s))

(deftype AppendOnlyReporter [out err state]
  Reporter
  (begin [_])

  (begin-step [_ {:keys [name]}]
    (let [{:keys [depth]} @state
          skip-reason *dry-run?*]
      (if-not skip-reason
        (println (indent depth "[run]") name)
        (println (indent depth (str "[skip - " skip-reason "]")) name)))
    (swap! state update :depth inc))

  (end-step [_ _]
    (swap! state update :depth dec))
  (end [_]))

(defn create [out err]
  (->AppendOnlyReporter out err (atom {:depth 0})))
