(ns hostage.github
  (:require
   [babashka.process :refer [shell]]
   [cheshire.core :as json]))

(defn search-issues [search-query]
  (-> (shell {:out :string}
             "gh issue list"
             "--search" search-query
             "--json" "number,labels,title,body")
      :out
      (json/parse-string true)))
