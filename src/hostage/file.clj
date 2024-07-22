(ns hostage.file
  (:require
   [babashka.process :refer [shell]]
   [clojure.java.io :refer [file]]
   [clojure.string :as str]
   [hostage.expect :as expect]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn named [n]
  (file n))

(defn simple-name [f]
  (.getName (file f)))

(defn content [f]
  ; TODO: Might be nice to implement deref on this File as a convenience
  (try (slurp (file f))
       (catch java.io.FileNotFoundException _
         nil)))

(defn delete [f]
  (.delete (file f)))

(defn exists? [f]
  (.exists (file f)))

(defn- simplify-editor [e]
  (cond
    (str/ends-with? e "vim")
    :vim

    :else e))

(defmulti edit-with (fn [params] (simplify-editor (:editor params))))
(defmethod edit-with :vim
  [{:keys [editor f content]}]
  (shell {:extra-env {"NOTES_CONTENT" content}}
         editor "-c" "silent put =$NOTES_CONTENT"
         (.getAbsolutePath (file f))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn edit [f {:keys [build-initial-content
                      delete-before-editing?
                      ensure-created?]}]
  (let [initial-content (or (content f)
                            (when build-initial-content
                              (build-initial-content))
                            "")]
    (when delete-before-editing?
      (delete f))

    (edit-with {:editor (System/getenv "EDITOR")
                :f f
                :content initial-content})

    ; TODO: Could we automatically add a cleanup step to delete the file?

    (expect/truthy?
     (or (not ensure-created?)
         (exists? f))
     (str "Aborted due to empty " (simple-name f)))))
