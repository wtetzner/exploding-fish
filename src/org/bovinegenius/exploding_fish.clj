;; Copyright (c) 2011,2012,2013 Walter Tetzner

;; Permission is hereby granted, free of charge, to any person
;; obtaining a copy of this software and associated documentation
;; files (the "Software"), to deal in the Software without
;; restriction, including without limitation the rights to use, copy,
;; modify, merge, publish, distribute, sublicense, and/or sell copies
;; of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:

;; The above copyright notice and this permission notice shall be
;; included in all copies or substantial portions of the Software.

;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
;; EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
;; MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
;; NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
;; HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
;; WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
;; DEALINGS IN THE SOFTWARE.

(ns org.bovinegenius.exploding-fish
  (:require (org.bovinegenius.exploding-fish
             [path :as path]
             [query-string :as qs]
             [parser :as parse]
             [constructor :as build]))
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
  (scheme-relative
    [self] [self new-scheme-relative]
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
  *uri-keys* #{:scheme :scheme-relative :authority
               :user-info :host :port :path :query
               :fragment})

(deftype Uri [data metadata]
  UniformResourceIdentifier
  (scheme [self] (:scheme data))
  (scheme [self new-scheme] (-> (assoc data :scheme new-scheme)
                                (parse/clean-map *uri-keys*)
                                (Uri. metadata)))
  (scheme-relative [self] (:scheme-relative data))
  (scheme-relative [self ssp]
    (let [new-data (assoc data :scheme-relative ssp)
          ssp-data (parse/scheme-specific ssp)
          auth-data (parse/authority (:authority ssp-data))]
      (-> (merge new-data ssp-data auth-data)
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (authority [self] (:authority data))
  (authority [self authority]
    (let [new-data (assoc data :authority authority)
          auth-data (parse/authority authority)]
      (-> (merge new-data auth-data)
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (user-info [self] (:user-info data))
  (user-info [self user-info]
    (let [new-data (assoc data :user-info user-info)
          authority (build/authority new-data)
          new-data (assoc new-data :authority authority)
          ssp (build/scheme-specific new-data)
          new-data (assoc new-data :scheme-relative ssp)]
      (-> new-data
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (host [self] (:host self))
  (host [self host]
    (let [new-data (assoc data :host host)
          authority (build/authority new-data)
          new-data (assoc new-data :authority authority)
          ssp (build/scheme-specific new-data)
          new-data (assoc new-data :scheme-relative ssp)]
      (-> new-data
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (port [self] (:port self))
  (port [self port]
    (let [new-data (if (and port (>= port 0))
                     (assoc data :port port)
                     (dissoc data :port))
          authority (build/authority new-data)
          new-data (assoc new-data :authority authority)
          ssp (build/scheme-specific new-data)
          new-data (assoc new-data :scheme-relative ssp)]
      (-> new-data
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (path [self] (:path self))
  (path [self path]
    (let [new-data (assoc data :path path)
          ssp (build/scheme-specific new-data)
          new-data (assoc new-data :scheme-relative ssp)]
      (-> new-data
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (query [self] (:query self))
  (query [self query]
    (let [new-data (assoc data :query query)
          ssp (build/scheme-specific new-data)
          new-data (assoc new-data :scheme-relative ssp)]
      (-> new-data
          (parse/clean-map *uri-keys*)
          (Uri. metadata))))
  (fragment [self] (:fragment self))
  (fragment [self fragment]
    (-> (assoc data :fragment fragment)
        (parse/clean-map *uri-keys*)
        (Uri. metadata)))
  
  clojure.lang.Associative
  (containsKey [self key] (contains? data key))
  (assoc [self key value]
    (condp = key
      :scheme (scheme self value)
      :scheme-relative (scheme-relative self value)
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

  clojure.lang.IFn
  (invoke [self key]
    (get self key))
  (invoke [self key value]
    (get self key value))
  
  Object
  (toString [self] (build/uri-string data))
  (equals [self other]
    (if (and (instance? (class self) other)
             (= (into {} other) data))
      true
      false))
  (hashCode [self]
    (int (/ (+ (Integer. (.hashCode data))
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
    java.lang.String (Uri. (parse/uri uri) nil)
    (Uri. (-> uri str str parse/uri) nil)))

(extend java.net.URI
  UniformResourceIdentifier
  {:scheme (fn ([^URI self] (.getScheme self))
             ([^URI self ^String new-scheme]
                (-> self uri (scheme new-scheme) str URI.)))
   :scheme-relative (fn ([^URI self]
                           (.getRawSchemeSpecificPart self))
                      ([^URI self ^String ssp]
                         (-> self uri
                             (scheme-relative ssp) str URI.)))
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
   :scheme-relative (fn ([^String self]
                                (-> self uri scheme-relative))
                           ([^String self ^String ssp]
                              (-> self uri
                                  (scheme-relative ssp) str str)))
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
   :scheme-relative (fn ([^URL self]
                                (-> self uri scheme-relative))
                           ([^URL self ^String ssp]
                              (-> self uri
                                  (scheme-relative ssp) str URL.)))
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
        :scheme-relative (scheme-relative uri)
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
  (cond (-> (:scheme-relative uri) empty? not)
        (URI. (:scheme uri) (:scheme-relative uri)
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
  (-> uri query qs/query-string->list))

(defn raw-pairs
  "Returns an alist of the query params matching the given key. If no
key is given, an alist of all the query params is returned."
  ([uri key]
     (->> uri qs/query-string->alist
          (filter (fn [[param-key value]]
                    (= param-key key)))
          vec))
  ([uri]
     (-> uri query qs/query-string->alist)))

(defn- encode-param
  "Encode a query key or param. If the value is nil, just return nil."
  [text]
  (if (string? text)
    (URLEncoder/encode text (default-encoding))
    text))

(defn- decode-param
  "Decode a query key or param. If the value is nil, just return nil."
  [text]
  (if (string? text)
    (URLDecoder/decode text (default-encoding))
    text))

(defn query-pairs
  "Returns an alist of the query params matching the given key. If no
key is given, an alist of all the query params is returned. The keys
and values of the pairs are URL decoded."
  ([uri key]
     (->> uri raw-pairs
          (map (fn [[key val]]
                 [(decode-param key) (decode-param val)]))
          (filter (fn [[param-key value]]
                    (= param-key key)))
          vec))
  ([uri]
     (->> uri raw-pairs
          (map (fn [[key val]]
                 [(decode-param key) (decode-param val)]))
          vec)))

(defn params-raw
  "Returns an alist of the query values whose key matches the given
key. If no key is given, all values are returned."
  ([uri key]
     (->> uri raw-pairs
          (filter (fn [[param-key value]]
                    (= param-key key)))
          (map second)
          vec))
  ([uri]
     (->> uri raw-pairs (map second) vec)))

(defn params
  "Returns an alist of the query values whose key matches the given
key. If no key is given, all values are returned."
  ([uri key]
     (->> uri query-pairs
          (filter (fn [[param-key value]]
                    (= param-key key)))
          (map second)
          vec))
  ([uri]
     (->> uri query-pairs (map second) vec)))

(defn param-raw
  "Get the last param value that matches the given key. If 3 args are
given, set the first param value that matches the given key, and
remove the remaining params that match the given key. If 4 args are
given, set the nth param value that matches the given key."
  ([uri key]
     (-> uri (params-raw key) last))
  ([uri key value]
     (query uri (-> uri raw-pairs
                    (qs/alist-replace key value)
                    qs/alist->query-string)))
  ([uri key value index]
     (query uri (-> uri raw-pairs
                    (qs/alist-replace key value index)
                    qs/alist->query-string))))

(defn param
  "Get the last param value that matches the given key. If 3 args are
given, set the first param value that matches the given key, and
remove the remaining params that match the given key. If 4 args are
given, set the nth param value that matches the given key."
  ([uri key]
     (-> uri (params key) last))
  ([uri key value]
     (query uri (->> (qs/alist-replace (query-pairs uri) key value)
                     (map (fn [[key val]]
                            [(encode-param key) (encode-param val)]))
                     qs/alist->query-string)))
  ([uri key value index]
     (query uri (->> (qs/alist-replace (query-pairs uri) key value index)
                     (map (fn [[key val]]
                            [(encode-param key) (encode-param val)]))
                     qs/alist->query-string))))

(defn query-map
  "Returns a map of the query string parameters for the given URI."
  [uri]
  (->> uri query-pairs (into {})))

(defn raw-keys
  "Returns a list of the query string keys of the given URI."
  [uri]
  (->> uri raw-pairs (map first) vec))

(defn query-keys
  "Returns a list of the query string keys of the given URI."
  [uri]
  (->> uri query-pairs (map first) vec))

(defn normalize-path
  "Normalize the path of the given uri."
  [uri]
  (->> (path uri) path/normalize (path uri)))

(defn- as-path
  "Ensure path"
  [p] 
  (path (uri p)))

(defn resolve-path
  "Resolve the given path against the given uri."
  [the-uri the-path]
    (path the-uri (-> (path the-uri) (path/resolve-path (as-path the-path)))))

(defn absolute-path?
  "Returns true if the given uri path is absolute."
  [uri]
  (-> uri path path/absolute?))

(defn absolute?
  "Returns true if the given URI is absolute."
  [uri]
  (boolean (authority uri)))

(defn resolve-uri
  "Resolves a target uri from a source uri.
Essentially this is an implementation of:
-> RFC 2396
-> RFC 1808
This implementation and the associated tests based on
this document: http://www.ics.uci.edu/~fielding/url/test2.html"
  [src-uri target-uri]
  (let [target-uri-query       (query target-uri)
        target-uri-fragment    (fragment target-uri)]

    ;; need to handle this case separately:
    ;; resolve-uri "http://a/b/c" "//c"
    ;; since path is nil, the resolve fails
    (if (re-find #"^//" target-uri)
      (scheme target-uri
              (scheme src-uri))
      (-> src-uri
         (resolve-path target-uri)
         (query target-uri-query)
         (fragment target-uri-fragment)))))