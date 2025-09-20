(ns hostage.github
  (:require
   [babashka.process :refer [shell]]
   [cheshire.core :as json]
   [hostage.flow :as flow]))

(defn release [version-name]
  {:version-name version-name})

(defn release-create [release {:keys [body]}]
  (flow/shell {:out :string
               :in (or body "")}
              "gh release create"
              (:version-name release)
              "--notes-file" "-"))

(defn release-upload [release & files]
  (apply flow/shell {:out :string}
         "gh release upload"
         (:version-name release)
         files))

(defn search-issues [search-query]
  (-> (shell {:out :string}
             "gh issue list"
             "--search" search-query
             "--json" "number,labels,title,body")
      :out
      (json/parse-string true)))
