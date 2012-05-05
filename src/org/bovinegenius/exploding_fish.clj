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

(ns org.bovinegenius.exploding-fish
  (:use (org.bovinegenius.exploding-fish query-string parser constructor))
  (:require (org.bovinegenius.exploding-fish [path :as path]))
  (:import (java.net URI URL URLDecoder URLEncoder)))

(def ^:dynamic *default-encoding*
  "Default encoding for query params."
  (ref "UTF-8"))

(defn default-encoding
  "Get the default encoding for query params."
  []
  (if (string? *default-encoding*)
    *default-encoding*
    (deref *default-encoding*)))

(defmacro with-encoding
  "Run body with the given encoding as the default-encoding."
  [encoding & body]
  `(binding [*default-encoding* ~encoding]
     ~@body))

(defprotocol UniformResourceIdentifier
  "Protocol for dealing with URIs."
  (scheme
    [self] [self new-scheme]
    "Get or set the scheme component of the URI.")
  (scheme-specific-part
    [self] [self new-scheme-specific-part]
    "Get or set the scheme specific part of the URI.")
  (authority
    [self] [self new-authority]
    "Get or set the authority of the URI.")
  (user-info
    [self] [self new-user-info]
    "Get or set the user-info of the URI.")
  (host
    [self] [self new-host]
    "Get or set the host of the URI.")
  (port
    [self] [self new-port]
    "Get or set the port of the URI.")
  (path
    [self] [self new-path]
    "Get or set the path of the URI.")
  (query
    [self] [self new-query]
    "Get or set the query string of the URI.")
  (fragment
    [self] [self new-fragment]
    "Get or set the fragment of the URI."))

(def ^{:dynamic true, :private true}
  *uri-keys* #{:scheme :scheme-specific-part :authority
               :user-info :host :port :path :query
               :fragment})

(deftype Uri [data metadata]
  UniformResourceIdentifier
  (scheme [self] (:scheme data))
  (scheme [self new-scheme] (-> (assoc data :scheme new-scheme)
                                (clean-map *uri-keys*)
                                (Uri. metadata)))
  (scheme-specific-part [self] (:scheme-specific-part data))
  (scheme-specific-part [self ssp] (let [new-data (assoc data :scheme-specific-part ssp)
                                         ssp-data (parse-scheme-specific ssp)
                                         auth-data (parse-authority (:authority ssp-data))]
                                     (-> (merge new-data ssp-data auth-data)
                                         (clean-map *uri-keys*)
                                         (Uri. metadata))))
  (authority [self] (:authority data))
  (authority [self authority] (let [new-data (assoc data :authority authority)
                                    auth-data (parse-authority authority)]
                                (-> (merge new-data auth-data)
                                    (clean-map *uri-keys*)
                                    (Uri. metadata))))
  (user-info [self] (:user-info data))
  (user-info [self user-info] (let [new-data (assoc data :user-info user-info)
                                    authority (build-authority new-data)
                                    new-data (assoc new-data :authority authority)
                                    ssp (build-scheme-specific new-data)
                                    new-data (assoc new-data :scheme-specific-part ssp)]
                                (-> new-data
                                    (clean-map *uri-keys*)
                                    (Uri. metadata))))
  (host [self] (:host self))
  (host [self host] (let [new-data (assoc data :host host)
                          authority (build-authority new-data)
                          new-data (assoc new-data :authority authority)
                          ssp (build-scheme-specific new-data)
                          new-data (assoc new-data :scheme-specific-part ssp)]
                      (-> new-data
                          (clean-map *uri-keys*)
                          (Uri. metadata))))
  (port [self] (:port self))
  (port [self port] (let [new-data (if (and port (>= port 0))
                                     (assoc data :port port)
                                     (dissoc data :port))
                          authority (build-authority new-data)
                          new-data (assoc new-data :authority authority)
                          ssp (build-scheme-specific new-data)
                          new-data (assoc new-data :scheme-specific-part ssp)]
                      (-> new-data
                          (clean-map *uri-keys*)
                          (Uri. metadata))))
  (path [self] (:path self))
  (path [self path] (let [new-data (assoc data :path path)
                          ssp (build-scheme-specific new-data)
                          new-data (assoc new-data :scheme-specific-part ssp)]
                      (-> new-data
                          (clean-map *uri-keys*)
                          (Uri. metadata))))
  (query [self] (:query self))
  (query [self query] (let [new-data (assoc data :query query)
                            ssp (build-scheme-specific new-data)
                            new-data (assoc new-data :scheme-specific-part ssp)]
                        (-> new-data
                            (clean-map *uri-keys*)
                            (Uri. metadata))))
  (fragment [self] (:fragment self))
  (fragment [self fragment] (-> (assoc data :fragment fragment)
                                (clean-map *uri-keys*)
                                (Uri. metadata)))
  
  clojure.lang.Associative
  (containsKey [self key] (contains? data key))
  (assoc [self key value] (condp = key
                              :scheme (scheme self value)
                              :scheme-specific-part (scheme-specific-part self value)
                              :authority (authority self value)
                              :user-info (user-info self value)
                              :host (host self value)
                              :port (port self value)
                              :path (path self value)
                              :query (query self value)
                              :fragment (fragment self value)
                              (Uri. (assoc data key value) metadata)))
  (entryAt [self key] (data key))

  clojure.lang.ILookup
  (valAt [self key] (get data key))
  (valAt [self key value] (get data key value))

  clojure.lang.Seqable
  (seq [self] (seq data))

  clojure.lang.IPersistentMap
  
  Object
  (toString [self] (build-uri-string data))
  (equals [self other] (if (and (instance? (class self) other)
                                (= (into {} other) data))
                         true
                         false))
  (hashCode [self] (int (/ (+ (Integer. (.hashCode data))
                              (Integer. (.hashCode (str self)))) 2)))

  clojure.lang.IPersistentCollection
  (equiv [self other] (.equals self other))
  
  clojure.lang.IObj
  (withMeta [self mdata] (Uri. data mdata))
  
  clojure.lang.IMeta
  (meta [self] metadata))

(defmethod print-method Uri [obj writer]
  (.write writer (format "#<Uri %s>"
                         (str obj))))

(defn uri
  "Builds a Uri object. Takes a map of URI keys, a URI string, or
another Uri object."
  [uri]
  (condp instance? uri
    Uri uri
    clojure.lang.IPersistentMap (Uri. uri (meta uri))
    java.lang.String (Uri. (parse-uri uri) nil)
    (Uri. (-> uri str str parse-uri) nil)))

(extend java.net.URI
  UniformResourceIdentifier
  {:scheme (fn ([^URI self] (.getScheme self))
             ([^URI self ^String new-scheme]
                (-> self uri (scheme new-scheme) str URI.)))
   :scheme-specific-part (fn ([^URI self] (.getRawSchemeSpecificPart self))
                           ([^URI self ^String ssp]
                              (-> self uri (scheme-specific-part ssp) str URI.)))
   :authority (fn ([^URI self] (.getRawAuthority self))
                ([^URI self ^String new-authority]
                   (-> self uri (authority new-authority) str URI.)))
   :user-info (fn ([^URI self] (.getRawUserInfo self))
                ([^URI self ^String new-user-info]
                   (-> self uri (user-info new-user-info) str URI.)))
   :host (fn ([^URI self] (.getHost self))
           ([^URI self ^String new-host]
              (-> self uri (host new-host) str URI.)))
   :port (fn ([^URI self] (let [port (.getPort self)]
                            (if (= port -1) nil port)))
           ([^URI self ^Integer new-port]
              (-> self uri (port new-port) str URI.)))
   :path (fn ([^URI self] (.getRawPath self))
           ([^URI self ^String new-path]
              (-> self uri (path new-path) str URI.)))
   :query (fn ([^URI self] (.getRawQuery self))
            ([^URI self ^String new-query]
               (-> self uri (query new-query) str URI.)))
   :fragment (fn ([^URI self] (.getRawFragment self))
               ([^URI self ^String new-fragment]
                  (-> self uri (fragment new-fragment) str URI.)))})

(extend String
  UniformResourceIdentifier
  {:scheme (fn ([^String self] (-> self uri scheme))
             ([^String self ^String new-scheme]
                (-> self uri (scheme new-scheme) str str)))
   :scheme-specific-part (fn ([^String self]
                                (-> self uri scheme-specific-part))
                           ([^String self ^String ssp]
                              (-> self uri (scheme-specific-part ssp) str str)))
   :authority (fn ([^String self] (-> self uri authority))
                ([^String self ^String new-authority]
                   (-> self uri (authority new-authority) str str)))
   :user-info (fn ([^String self] (-> self uri user-info))
                ([^String self ^String new-user-info]
                   (-> self uri (user-info new-user-info) str str)))
   :host (fn ([^String self] (-> self uri host))
           ([^String self ^String new-host]
              (-> self uri (host new-host) str str)))
   :port (fn ([^String self] (-> self uri port))
           ([^String self ^Integer new-port]
              (-> self uri (port new-port) str str)))
   :path (fn ([^String self] (-> self uri path))
           ([^String self ^String new-path]
              (-> self uri (path new-path) str str)))
   :query (fn ([^String self] (-> self uri query))
            ([^String self ^String new-query]
               (-> self uri (query new-query) str str)))
   :fragment (fn ([^String self] (-> self uri fragment))
               ([^String self ^String new-fragment]
                  (-> self uri (fragment new-fragment) str str)))})

(extend java.net.URL
  UniformResourceIdentifier
  {:scheme (fn ([^URL self] (-> self uri scheme))
             ([^URL self ^String new-scheme]
                (-> self uri (scheme new-scheme) str URL.)))
   :scheme-specific-part (fn ([^URL self]
                                (-> self uri scheme-specific-part))
                           ([^URL self ^String ssp]
                              (-> self uri (scheme-specific-part ssp) str URL.)))
   :authority (fn ([^URL self] (-> self uri authority))
                ([^URL self ^String new-authority]
                   (-> self uri (authority new-authority) str URL.)))
   :user-info (fn ([^URL self] (-> self uri user-info))
                ([^URL self ^String new-user-info]
                   (-> self uri (user-info new-user-info) str URL.)))
   :host (fn ([^URL self] (-> self uri host))
           ([^URL self ^String new-host]
              (-> self uri (host new-host) str URL.)))
   :port (fn ([^URL self] (-> self uri port))
           ([^URL self ^Integer new-port]
              (-> self uri (port new-port) str URL.)))
   :path (fn ([^URL self] (-> self uri path))
           ([^URL self ^String new-path]
              (-> self uri (path new-path) str URL.)))
   :query (fn ([^URL self] (-> self uri query))
            ([^URL self ^String new-query]
               (-> self uri (query new-query) str URL.)))
   :fragment (fn ([^URL self] (-> self uri fragment))
               ([^URL self ^String new-fragment]
                  (-> self uri (fragment new-fragment) str URL.)))})

(defn uri->map
  "Convert a java.net.URI into a hash-map."
  [^URI uri]
  (->> {:scheme (scheme uri)
        :scheme-specific-part (scheme-specific-part uri)
        :authority (authority uri)
        :user-info (user-info uri)
        :host (host uri)
        :port (port uri)
        :path (path uri)
        :query (query uri)
        :fragment (fragment uri)}
       (filter val)
       (into {})))

(defn ^URI map->uri
  "Convert a hash-map into a java.net.URI."
  [uri]
  (cond (-> (:scheme-specific-part uri) empty? not)
        (URI. (:scheme uri) (:scheme-specific-part uri)
              (:fragment uri))
        
        (:user-info uri)
        (URI. (:scheme uri) (:user-info uri)
              (:host uri) (or (:port uri) -1)
              (:path uri) (:query uri)
              (:fragment uri))
        
        (:host uri)
        (URI. (:scheme uri) (:host uri) (:path uri)
              (:fragment uri))
        
        :else (URI. (:scheme uri) (:authority uri)
                    (:path uri) (:query uri)
                    (:fragment uri))))

(defn string->map
  "Convert a URI in string form to a map."
  [^String string]
  (-> string URI. uri->map))

(defn ^String map->string
  "Convert a URI in map form to a string."
  [uri]
  (-> uri map->uri str str))

(defn query-list
  "Returns a list from the query string of the given URI."
  [uri]
  (-> uri query query-string->list))

(defn query-pairs
  "Returns an alist of the query params matching the given key. If no
key is given, an alist of all the query params is returned."
  ([uri key]
     (->> uri query-pairs
          (filter (fn [[param-key value]]
                    (= param-key key)))
          vec))
  ([uri]
     (-> uri query query-string->alist)))

(defn query-params
  "Returns an alist of the query values whose key matches the given key. If no
key is given, all values are returned."
  ([uri key]
     (->> uri query-pairs
          (filter (fn [[param-key value]]
                    (= param-key key)))
          (map second)
          vec))
  ([uri]
     (->> uri query query-string->alist (map second) vec)))

(defn query-param-raw
  "Get the last param value that matches the given key. If 3 args are
given, set the first param value that matches the given key, and
remove the remaining params that match the given key. If 4 args are
given, set the nth param value that matches the given key."
  ([uri key]
     (-> uri (query-params key) last))
  ([uri key value]
     (query uri (-> uri query-pairs
                    (alist-replace key value)
                    alist->query-string)))
  ([uri key value index]
     (query uri (-> uri query-pairs
                    (alist-replace key value index)
                    alist->query-string))))

(defn- encode-param
  "Encode a query key or param. If the value is nil, just return nil."
  [text]
  (if (nil? text)
    nil
    (URLEncoder/encode text (default-encoding))))

(defn- decode-param
  "Decode a query key or param. If the value is nil, just return nil."
  [text]
  (if (nil? text)
    nil
    (URLDecoder/decode text (default-encoding))))

(defn query-param
  "Get the last param value that matches the given key. If 3 args are
given, set the first param value that matches the given key, and
remove the remaining params that match the given key. If 4 args are
given, set the nth param value that matches the given key."
  ([uri key]
     (let [param (-> uri (query-params key) last)]
       (decode-param param)))
  ([uri key value]
     (query uri (-> uri query-pairs
                    (alist-replace (encode-param key) (encode-param value))
                    alist->query-string)))
  ([uri key value index]
     (query uri (-> uri query-pairs
                    (alist-replace (encode-param key) (encode-param value) index)
                    alist->query-string))))

(defn query-map
  "Returns a map of the query string parameters for the given URI."
  [uri]
  (->> uri query-pairs (into {})))

(defn query-keys
  "Returns a list of the query string keys of the given URI."
  [uri]
  (->> uri query-pairs (map first) vec))

(defn normalize-path
  "Normalize the path of the given uri."
  [uri]
  (->> (path uri) path/normalize (path uri)))

(defn resolve-path
  "Resolve the given path against the given uri."
  [uri the-path]
  (path uri (-> (path uri) (path/resolve-path the-path))))

(defn absolute?
  "Returns true if the given uri is absolute."
  [uri]
  (-> uri path path/absolute?))
