(ns hostage.date)

(defn as-format [date format-str]
  (.format (java.time.format.DateTimeFormatter/ofPattern format-str)
           date))

(defn as-iso-string [date]
  (as-format date "yyyy-MM-dd'T'hh:mm:ssZ"))
