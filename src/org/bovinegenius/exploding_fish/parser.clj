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

(ns org.bovinegenius.exploding-fish.parser
  (:require (clojure [string :as str])))

(defn generic
  "Takes a URI string and parses it into scheme, scheme-specific-part,
and fragment parts."
  [uri]
  (if uri
    (let [[_ scheme ssp fragment] (re-find #"^(.*?):([^#]+)#?(.*)$" uri)
          fragment (if (.contains uri "#") fragment nil)]
      {:scheme scheme :scheme-specific-part ssp :fragment fragment})
    {:scheme nil :scheme-specific-part nil :fragment nil}))

(defn scheme-specific
  "Parse the scheme specific part into authority, path, and query
parts."
  [ssp]
  (or
   (when ssp
     (let [[front & query] (str/split ssp #"\?")
           query (apply str query)
           query (if (.contains ssp "?") query nil)]
       (when-let [auth-path (last (re-find #"^//(.*)$" front))]
         (let [[_ authority path] (re-find #"^(.*?)(?=\./|\.\./|/|$)(.*)" auth-path)
               path (if (empty? path) nil path)]
           {:authority authority :path path :query query}))))
   {:authority nil :path nil :query nil}))

(defn authority
  "Parse the authority part into user-info, hostname, and port parts."
  [authority]
  (if authority
    (let [[_ user-info host port] (re-find #"^([^@]+(?=@))?@?([^:]+):?(.*)$" authority)
          port (if (empty? port) nil (Integer/parseInt port))]
      {:user-info user-info :host host :port port})
    {:user-info nil :host nil :port nil}))

(defn clean-map
  "Remove keys that have a nil value, and that are in the given set."
  ([data keys]
     (reduce (fn [data key]
               (if (not (nil? (data key)))
                 data
                 (dissoc data key)))
             data keys))
  ([data]
     (->> (filter (fn [[key value]] (not (nil? value))) data)
          (into (empty data)))))

(defn uri
  "Parse a URI string."
  [uri]
  (let [gen (generic uri)]
    (if (empty? (clean-map gen))
      {:path uri}
      (let [ss (scheme-specific (:scheme-specific-part gen))
            auth (authority (:authority ss))]
        (->> (merge gen ss auth)
             clean-map
             (into {}))))))
