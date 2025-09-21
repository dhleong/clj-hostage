(ns hostage.github
  (:require
   [babashka.process :refer [shell]]
   [cheshire.core :as json]
   [clojure.string :as str]
   [hostage.flow :as flow]
   [hostage.util.base64 :refer [str->base64]])
  (:import
   (java.net URLEncoder))
  (:refer-clojure :exclude [slurp spit]))

(def ^:private ^:dynamic *repo* nil)

(defn- gh-repo []
  (when *repo*
    (str "--repo " *repo*)))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defprotocol IGHFile
  (slurp [this])
  (spit
    [this contents]
    [this opts contents]))

(defn- ghfile->api-path [f]
  (str "/repos/" (or (.-repo f)
                     "{owner}/{repo}")
       "/contents/"
       (URLEncoder/encode (.-path f))))

(defn- ghfile->sha [f]
  (-> (shell {:out :string}
             "gh api"
             (ghfile->api-path f)
             "--jq" ".sha")
      :out
      (str/trim)))

(defrecord GHFile [repo path]
  IGHFile
  (slurp [this]
    (-> (shell
         {:out :string}
         "gh api"
         "-H" "Accept: application/vnd.github.raw+json"
         (ghfile->api-path this))
        :out))
  (spit [this contents] (spit this nil contents))
  (spit [this opts contents]
    (let [sha (ghfile->sha this)
          message (or (:message opts)
                      (str "Edit " (.-path this)))]
      (-> (flow/shell
           {:out :string}
           "gh api"
           "--method" "PUT"
           "-H" "Accept: application/vnd.github+json"
           (ghfile->api-path this)
           "-f" "message=" message
           "-f" "content=" (str->base64 (str contents))
           "-f" "sha=" sha)
          :out))))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn file [path]
  (->GHFile *repo* path))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn release [version-name]
  {:version-name version-name})

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn release-create [release {:keys [body]}]
  (flow/shell {:out :string
               :in (or body "")}
              "gh release create"
              (gh-repo)
              (:version-name release)
              "--notes-file" "-"))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn release-upload [release & files]
  (apply flow/shell {:out :string}
         "gh release upload"
         (gh-repo)
         (:version-name release)
         (flatten files)))

(defn search-issues [search-query]
  (-> (shell {:out :string}
             "gh issue list"
             (gh-repo)
             "--search" search-query
             "--json" "number,labels,title,body")
      :out
      (json/parse-string true)))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-repo [repo & body]
  `(binding [*repo* ~repo]
     ~@body))
