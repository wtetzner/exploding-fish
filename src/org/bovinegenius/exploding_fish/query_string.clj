;; Copyright (c) 2011,2012 Walter Tetzner

;; Permission is hereby granted, free of charge, to any person obtaining
;; a copy of this software and associated documentation files (the
;; "Software"), to deal in the Software without restriction, including
;; without limitation the rights to use, copy, modify, merge, publish,
;; distribute, sublicense, and/or sell copies of the Software, and to
;; permit persons to whom the Software is furnished to do so, subject to
;; the following conditions:

;; The above copyright notice and this permission notice shall be included
;; in all copies or substantial portions of the Software.

;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
;; EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
;; MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
;; IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
;; CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
;; TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
;; SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(ns org.bovinegenius.exploding-fish.query-string
  (:require (clojure [string :as str]))
  (:import (java.net URLDecoder URLEncoder)))

(defn query-string->list
  "Convert a query string into a list by separating on &'s."
  [^String query-string]
  (if query-string
    (str/split query-string #"&")
    []))

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

(defn alist-replace
  "Replace a value in an alist. If there is more than one pair with
the same key, the first value will be set, and the following values
will be removed. If an index is given, it will replace the nth value
whose key matches the given key, and other pairs with that key will be
unmodified."
  ([alist key value]
     (let [pair-matcher (fn [[param-key value]]
                          (not= param-key key))
           front (take-while pair-matcher
                             alist)
           item (->> alist
                     (drop-while pair-matcher)
                     first)
           back (->> alist
                     (drop-while pair-matcher)
                     rest
                     (filter pair-matcher))]
       (with-meta (vec (concat front [(with-meta [key value] (meta item))] back))
         (meta alist))))
  ([alist key value index]
     (let [pair-matcher (fn [[param-key value]]
                          (not= param-key key))
           items (->> alist
                      (map-indexed (fn [index pair]
                                     (vary-meta pair assoc :index index)))
                      (remove pair-matcher)
                      vec)
           item (if (>= index (count items)) nil (items index))
           index (or (-> item meta :index) (count alist))
           front (take index alist)
           back (drop (inc index) alist)]
       (with-meta (vec (concat front (with-meta [[key value]] (meta item)) back))
         (meta alist)))))
