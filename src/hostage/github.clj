(ns hostage.github
  (:require
   [babashka.process :refer [shell]]
   [cheshire.core :as json]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn release [version-name]
  {:version-name version-name})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn release-create [release {:keys [body]}]
  (shell {:out :string
          :in (or body "")}
         "gh release create"
         (:version-name release)
         "--notes" "-"))

(defn search-issues [search-query]
  (-> (shell {:out :string}
             "gh issue list"
             "--search" search-query
             "--json" "number,labels,title,body")
      :out
      (json/parse-string true)))
