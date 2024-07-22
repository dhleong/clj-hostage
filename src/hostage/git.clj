(ns hostage.git
  (:require
   [babashka.process :refer [sh shell]]
   [clojure.string :as str]
   [hostage.coll :refer [lazier-map]]))

(defn tags-on-branch [{:keys [branch search-depth]
                       :or {branch "main"
                            search-depth 100}}]
  (try
    (->> (shell {:out :string} "git rev-list"
                branch
                "--tags"
                (str "--max-count=" search-depth))
         :out
         (str/split-lines)
         (lazier-map
          (fn [commit-hash]
            {:name (-> (sh "git describe --tags --exact-match"
                           commit-hash)
                       :out
                       (str/trim))
             :hash commit-hash})))
    (catch Exception _
      nil)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag-latest [params]
  (first (tags-on-branch params)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag [name]
  {:name name})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag-create [tag]
  (shell {:out :string}
         "git tag" (:name tag)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag-exists? [tag]
  (-> (shell {:out :string}
             "git tag -l" (:name tag))
      :out
      (seq)
      (some?)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag-push [tag remote]
  (shell {:out :string}
         "git push" remote (:name tag)))

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
