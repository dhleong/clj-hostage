(ns hostage.git
  (:require
   [babashka.process :refer [shell]]
   [clojure.string :as str]))

(defn tag-latest [params]
  {:name "1.0.0"})

(defn- parse-tag-date [date-str]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss Z")]
    (.parse formatter date-str)))

(defn tag-created-date [tag]
  (try
    (-> (shell {:out :string} "git log -1"
               "--format=%ai"
               (:name tag))
        :out
        (str/trim)
        (parse-tag-date))
    (catch Exception _
      nil)))

(defn log [params]
  [])
