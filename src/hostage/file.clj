(ns hostage.file
  (:require
   [babashka.process :refer [shell]]
   [clojure.java.io :refer [file]]
   [clojure.string :as str]
   [hostage.expect :as expect]
   [hostage.flow :as flow]))

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

(defn- perform-edit [f {:keys [initial-content
                               delete-before-editing?
                               ensure-created?]}]
  (when delete-before-editing?
    (delete f))

  (edit-with {:editor (System/getenv "EDITOR")
              :f f
              :content initial-content})

  (expect/truthy?
   (or (not ensure-created?)
       (exists? f))
   (str "Aborted due to empty " (simple-name f))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn edit [f {:keys [build-initial-content
                      always-edit?
                      _delete-before-editing?
                      cleanup?
                      _ensure-created?]
               :or {cleanup? true}
               :as params}]
  (let [existing-content (content f)]
    (if (or always-edit?
            (not existing-content)
            (empty? existing-content))
      (perform-edit f (assoc params
                             :initial-content
                             (or existing-content
                                 (when build-initial-content
                                   (build-initial-content))
                                 "")))
      (println "Reusing existing content in " (simple-name f)))

    ; Automatically add a cleanup step to delete the file
    (when cleanup?
      (flow/enqueue-cleanup-task
       #(delete f)))))
