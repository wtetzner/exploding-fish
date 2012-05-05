;; Copyright (c) 2012 Walter Tetzner

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

(ns org.bovinegenius.exploding-fish.path
  (:require (clojure [string :as str])))

(defn split-path
  "Split a path into its parts."
  [path]
  (->> (range (count path))
       (map (fn [index]
              (String. (into-array Integer/TYPE [(.codePointAt path index)])
                       0 1)))
       (reduce (fn [parts char]
                 (if (= "/" char)
                   (conj parts "")
                   (assoc parts (-> parts count dec) (str (last parts) char))))
          [""])))

(defn ^String normalize
  "Normalize the given path."
  [path]
  (->> (str/replace path #"/+" "/")
       split-path
       (remove (partial = "."))
       (reduce (fn [pieces element]
                 (cond (and (= element "..") (or (empty? pieces)
                                                 (= (last pieces) "..")))
                       (conj pieces element)

                       (= element "..") (pop pieces)
                       :else (conj pieces element))) [])
       (str/join "/")))

(defn absolute?
  "Returns true if the given path is absolute, false otherwise."
  [path]
  (.startsWith path "/"))

(defn ^String resolve-path
  "Resolve one path against a base path."
  [base path]
  (if (absolute? path)
    (normalize path)
    (let [base-parts (split-path base)
          path-parts (split-path path)]
      (->> (concat (butlast base-parts) path-parts)
           (str/join "/")
           normalize))))
