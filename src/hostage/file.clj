(ns hostage.file
  (:require
   [babashka.process :refer [shell]]
   [clojure.java.io :refer [file]]
   [clojure.string :as str]))

(defn named [n]
  (file n))

(defn simple-name [f]
  (.getName (file f)))

(defn content [f]
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

    (when (and ensure-created?
               (not (exists? f)))
      (throw (ex-info (str "Aborted due to empty " (simple-name f)) {})))))
