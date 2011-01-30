(ns org.bovinegenius.uri
  (:use (org.bovinegenius.uri query-string)
        (clojure.contrib def))
  (:import (java.net URI URL URLDecoder URLEncoder)))

(defvar *default-encoding* (ref "UTF-8")
  "Default encoding for query params.")

(defn default-encoding
  "Get the default encoding for query params."
  []
  (if (string? *default-encoding*)
    *default-encoding*
    (deref *default-encoding*)))

(extend java.net.URI
  UniversalResourceIdentifier
  {:scheme (fn ([^URI self] (.getScheme self))
             ([^URI self ^String new-scheme]
                (cond (-> (.getRawSchemeSpecificPart self) empty? not)
                      (URI. (str new-scheme ":" (.getRawSchemeSpecificPart self)
                                 (if (.getRawFragment self)
                                   (str "#" (.getRawFragment self)) nil)))
                      
                      (.getRawUserInfo self)
                      (URI. (str new-scheme "://"
                                 (if (.getRawUserInfo self)
                                   (str (.getRawUserInfo self) "@") "") (.getHost self)
                                   (if (< (.getPort self) 0) "" (str ":" (.getPort self)))
                                   (.getRawPath self)
                                   (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                                   (if (.getRawFragment self) (str "#" (.getRawFragment self)))))

                      (.getHost self)
                      (URI. (str new-scheme "://" (.getHost self) (.getRawPath self)
                                 (if (.getRawFragment self)
                                   (str "#" (.getRawFragment self)) nil)))

                      :else (URI. (str new-scheme "://"
                                       (.getRawAuthority self) (.getRawPath self)
                                       (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                                       (if (.getRawFragment self) (str "#" (.getRawFragment self)) nil))))))
   :scheme-specific-part (fn ([^URI self] (.getRawSchemeSpecificPart self))
                           ([^URI self ^String ssp]
                              (URI. (str (.getScheme self) ssp
                                         (if (.getRawFragment self)
                                           (str "#" (.getRawFragment self)) nil)))))
   :authority (fn ([^URI self] (.getRawAuthority self))
                ([^URI self ^String authority]
                   (URI. (str (.getScheme self) "://"
                              authority (.getRawPath self)
                              (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                              (if (.getRawFragment self) (str "#" (.getRawFragment self)) nil)))))
   :user-info (fn ([^URI self] (.getRawUserInfo self))
                ([^URI self ^String user-info]
                   (URI. (str (.getScheme self) "://"
                              (if user-info (str user-info "@") "") (.getHost self)
                              (if (< (.getPort self) 0) "" (str ":" (.getPort self)))
                              (.getRawPath self)
                              (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                              (if (.getRawFragment self) (str "#" (.getRawFragment self)))))))
   :host (fn ([^URI self] (.getHost self))
           ([^URI self ^String host]
              (URI. (str (.getScheme self) "://"
                         (if (.getRawUserInfo self)
                           (str (.getRawUserInfo self) "@") "") host
                         (if (< (.getPort self) 0) "" (str ":" (.getPort self)))
                         (.getRawPath self)
                         (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                         (if (.getRawFragment self) (str "#" (.getRawFragment self)))))))
   :port (fn ([^URI self] (let [port (.getPort self)]
                            (if (= port -1) nil port)))
           ([^URI self ^Integer port]
              (URI. (str (.getScheme self) "://"
                         (if (.getRawUserInfo self)
                           (str (.getRawUserInfo self) "@") "") (.getHost self)
                         (if (< port 0) "" (if port (str ":" port) "")) (.getRawPath self)
                         (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                         (if (.getRawFragment self) (str "#" (.getRawFragment self)))))))
   :path (fn ([^URI self] (.getRawPath self))
           ([^URI self ^String path]
              (URI. (str (.getScheme self) "://"
                          (.getRawAuthority self) path
                          (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                          (if (.getRawFragment self) (str "#" (.getRawFragment self)) nil)))))
   :query (fn ([^URI self] (.getRawQuery self))
            ([^URI self ^String query]
               (URI. (str (.getScheme self) "://"
                          (.getRawAuthority self) (.getRawPath self)
                          (if query (str "?" query) nil)
                          (if (.getRawFragment self) (str "#" (.getRawFragment self)) nil)))))
   :fragment (fn ([^URI self] (.getRawFragment self))
               ([^URI self ^String fragment]
                  (URI. (str (.getScheme self) "://"
                             (.getRawAuthority self) (.getRawPath self)
                             (if (.getRawQuery self) (str "?" (.getRawQuery self)) nil)
                             (if fragment (str "#" fragment) nil)))))})

(extend String
  UniversalResourceIdentifier
  {:scheme (fn ([^String self] (-> self URI. scheme))
             ([^String self ^String new-scheme]
                (-> self URI. (scheme new-scheme) str str)))
   :scheme-specific-part (fn ([^String self]
                                (-> self URI. scheme-specific-part))
                           ([^String self ^String ssp]
                              (-> self URI. (scheme-specific-part ssp) str str)))
   :authority (fn ([^String self] (-> self URI. authority))
                ([^String self ^String new-authority]
                   (-> self URI. (authority new-authority) str str)))
   :user-info (fn ([^String self] (-> self URI. user-info))
                ([^String self ^String new-user-info]
                   (-> self URI. (user-info new-user-info) str str)))
   :host (fn ([^String self] (-> self URI. host))
           ([^String self ^String new-host]
              (-> self URI. (host new-host) str str)))
   :port (fn ([^String self] (-> self URI. port))
           ([^String self ^Integer new-port]
              (-> self URI. (port new-port) str str)))
   :path (fn ([^String self] (-> self URI. path))
           ([^String self ^String new-path]
              (-> self URI. (path new-path) str str)))
   :query (fn ([^String self] (-> self URI. query))
            ([^String self ^String new-query]
               (-> self URI. (query new-query) str str)))
   :fragment (fn ([^String self] (-> self URI. fragment))
               ([^String self ^String new-fragment]
                  (-> self URI. (fragment new-fragment) str str)))})

(extend java.net.URL
  UniversalResourceIdentifier
  {:scheme (fn ([^URL self] (-> self .toURI scheme))
             ([^URL self ^String new-scheme]
                (-> self .toURI ^URI (scheme new-scheme) .toURL)))
   :scheme-specific-part (fn ([^URL self]
                                (-> self .toURI scheme-specific-part))
                           ([^URL self ^String ssp]
                              (-> self .toURI ^URI (scheme-specific-part ssp) .toURL)))
   :authority (fn ([^URL self] (-> self .toURI authority))
                ([^URL self ^String new-authority]
                   (-> self .toURI ^URI (authority new-authority) .toURL)))
   :user-info (fn ([^URL self] (-> self .toURI user-info))
                ([^URL self ^String new-user-info]
                   (-> self .toURI ^URI (user-info new-user-info) .toURL)))
   :host (fn ([^URL self] (-> self .toURI host))
           ([^URL self ^String new-host]
              (-> self .toURI ^URI (host new-host) .toURL)))
   :port (fn ([^URL self] (-> self .toURI port))
           ([^URL self ^Integer new-port]
              (-> self .toURI ^URI (port new-port) .toURL)))
   :path (fn ([^URL self] (-> self .toURI path))
           ([^URL self ^String new-path]
              (-> self .toURI ^URI  (path new-path) .toURL)))
   :query (fn ([^URL self] (-> self .toURI query))
            ([^URL self ^String new-query]
               (-> self .toURI ^URI (query new-query) .toURL)))
   :fragment (fn ([^URL self] (-> self .toURI fragment))
               ([^URL self ^String new-fragment]
                  (-> self .toURI ^URI (fragment new-fragment) .toURL)))})

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

(extend clojure.lang.IPersistentMap
  UniversalResourceIdentifier
  {:scheme (fn ([self] (-> self map->uri scheme))
             ([self new-scheme]
                (-> self map->uri (scheme new-scheme) uri->map
                    (with-meta (meta self)))))
   :scheme-specific-part (fn ([self]
                                (-> self map->uri scheme-specific-part))
                           ([self ssp]
                              (-> self map->uri (scheme-specific-part ssp) uri->map
                                  (with-meta (meta self)))))
   :authority (fn ([self] (-> self map->uri authority))
                ([self new-authority]
                   (-> self map->uri (authority new-authority) uri->map
                       (with-meta (meta self)))))
   :user-info (fn ([self] (-> self map->uri user-info))
                ([self new-user-info]
                   (-> self map->uri (user-info new-user-info) uri->map
                       (with-meta (meta self)))))
   :host (fn ([self] (-> self map->uri host))
           ([self new-host]
              (-> self map->uri (host new-host) uri->map
                  (with-meta (meta self)))))
   :port (fn ([self] (-> self map->uri port))
           ([self new-port]
              (-> self map->uri (port new-port) uri->map
                  (with-meta (meta self)))))
   :path (fn ([self] (-> self map->uri path))
           ([self new-path]
              (-> self map->uri (path new-path) uri->map
                  (with-meta (meta self)))))
   :query (fn ([self] (-> self map->uri query))
            ([self new-query]
               (-> self map->uri (query new-query) uri->map
                   (with-meta (meta self)))))
   :fragment (fn ([self] (-> self map->uri fragment))
               ([self new-fragment]
                  (-> self uri->map (fragment new-fragment) map->uri
                      (with-meta (meta self)))))})

(defn query-list
  "Returns a list from the query string of the given URI."
  [^UniversalResourceIdentifier uri]
  (-> uri query query-string->list))

(defn query-pairs
  "Returns an alist of the query params matching the given key. If no
key is given, an alist of all the query params is returned."
  ([^UniversalResourceIdentifier uri key]
     (->> uri query-params
          (filter (fn [[param-key value]]
                    (= param-key key)))
          vec))
  ([^UniversalResourceIdentifier uri]
     (-> uri query query-string->alist)))

(defn query-params
  "Returns an alist of the query values whose key matches the given key. If no
key is given, all values are returned."
  ([^UniversalResourceIdentifier uri key]
     (->> uri query-pairs
          (filter (fn [[param-key value]]
                    (= param-key key)))
          (map second)
          vec))
  ([^UniversalResourceIdentifier uri]
     (->> uri query query-string->alist (map second) vec)))

(defn query-param
  "Get the last param value that matches the given key. If 3 args are
given, set the first param value that matches the given key, and
remove the remaining params that match the given key. If 4 args are
given, set the nth param value that matches the given key."
  ([^UniversalResourceIdentifier uri key]
     (-> uri (query-params key) last (URLDecoder/decode (default-encoding))))
  ([^UniversalResourceIdentifier uri key value]
     (query uri (-> uri query-pairs
                    (alist-replace key (URLEncoder/encode value (default-encoding)))
                    alist->query-string)))
  ([^UniversalResourceIdentifier uri key value index]
     (query uri (-> uri query-pairs
                    (alist-replace key (URLEncoder/encode value (default-encoding)) index)
                    alist->query-string))))

(defn query-map
  "Returns a map of the query string parameters for the given URI."
  [^UniversalResourceIdentifier uri]
  (->> uri query-pairs (into {})))

(defn query-keys
  "Returns a list of the query string keys of the given URI."
  [^UniversalResourceIdentifier uri]
  (->> uri query-pairs (map first) vec))
