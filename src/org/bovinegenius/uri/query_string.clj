(ns org.bovinegenius.uri.query-string
  (:require (clojure [string :as str])))

(defn query-string->list
  "Convert a query string into a list by separating on &'s."
  [^String query-string]
  (str/split query-string #"&"))

(defn list->alist
  "Convert a query string list into an alist."
  [list]
  (vec (map (fn [text]
              (let [[key value] (or (next (re-find #"^([^=]*)=(.*)$" text))
                                    [text])]
                [key value]))
            list)))

(defn query-string->alist
  "Convert a query string into an alist."
  [^String query-string]
  (-> query-string query-string->list list->alist))

(defn alist->list
  "Convert a query string alist into a list."
  [alist]
  (vec (map (fn [[key value]]
              (if value
                (if key
                  (format "%s=%s" key value)
                  value)
                (if key
                  key
                  "")))
            alist)))

(defn list->query-string
  "Convert a query string list into a query string."
  [list]
  (str/join "&" list))

(defn alist->query-string
  "Convert a query string alist into a query string."
  [alist]
  (-> alist alist->list list->query-string))
