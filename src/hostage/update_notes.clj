(ns hostage.update-notes
  (:require
   [babashka.process :refer [shell]]
   [clojure.string :as str]
   [hostage.date :as date]
   [hostage.git :as git]
   [hostage.github :as github]))

(def default-labels-map
  {"feature" "New Features"
   "enhancement" "Enhancements"
   "bug" "Bug Fixes"
   :default "Other Resolved Tickets"})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro build [& forms]
  `(str/trim
    (with-out-str
      ~@forms)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro with-header [header & body]
  `(let [content# (str/trim-newline
                   (with-out-str
                     ~@body))]
     (when (seq (str/trim content#))
       (println (str "\n" ~header))
       (println content#))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn github-closed-issues-by-label [{:keys [labels since-tag] :as params}]
  (when-not (contains? params :since-tag)
    (throw (ex-info "Params did not include :since-tag" params)))

  (let [labels (or labels default-labels-map)
        issues (github/search-issues
                (str "state:closed "
                     (when since-tag
                       (str "closed:>"
                            (date/as-iso-string
                             (git/tag-created-date since-tag))))))
        issues-by-label (->> issues
                             (group-by (fn [issue]
                                         ; Find one of the named labels
                                         (let [my-labels (->> (:labels issue)
                                                              (map :name)
                                                              (into #{}))]
                                           (or (some my-labels (keys labels))
                                               :default)))))]
    (doseq [[label title] labels]
      (when-let [label-issues (get issues-by-label label)]
        (with-header (str "**" title ":**")
          (doseq [issue label-issues]
            (println " - " (:title issue) (str "(#" (:number issue) ")")))))
      issues-by-label)))

(defn- clean-grep-arg [p]
  (str/replace p #"[#]" "\\#"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn git-messages-matching [{:keys [grep since-tag invert-grep] :as params}]
  (when (and grep invert-grep)
    (throw (ex-info "Cannot provide both grep and invert-grep" {:params params})))

  (when since-tag
    (let [to-grep (or grep invert-grep)
          params (->> [(str (:name since-tag) "..HEAD")
                       (when (string? to-grep)
                         (str "--grep=" (clean-grep-arg grep)))

                       (when (coll? to-grep)
                         (map #(str "--grep=" (clean-grep-arg %)) grep))

                       "--pretty=format:- %s"

                       (when invert-grep
                         "--invert-grep")]
                      flatten
                      (keep identity))]
      (-> (apply shell {:out :string}
                 "git log"
                 params)
          :out))))
