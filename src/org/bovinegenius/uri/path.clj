(ns org.bovinegenius.uri.path
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

(defn normalize
  "Normalize the given path."
  [path]
  (->> (split-path path)
       (remove (partial = "."))
       (reduce (fn [pieces element]
                 (if (= element "..")
                   (pop pieces)
                   (conj pieces element))) [])
       (str/join "/")))

(defn absolute?
  "Returns true if the given path is absolute, false otherwise."
  [path]
  (.startsWith path "/"))

(defn resolve-path
  "Resolve one path against a base path."
  [base path]
  (if (absolute? path)
    (normalize path)
    (let [base-parts (split-path base)
          path-parts (split-path path)]
      (->> (concat (butlast base-parts) path-parts)
           (str/join "/")
           normalize))))
