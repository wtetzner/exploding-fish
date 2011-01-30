(ns org.bovinegenius.uri.parser)

(defn parse-generic
  "Takes a URI string and parses it into scheme, scheme-specific-part,
and fragment parts."
  [uri]
  (let [[_ scheme ssp fragment] (re-find #"^(.*?):([^#]+)#?(.*)$" uri)
        fragment (if (.contains uri "#") fragment nil)]
    {:scheme scheme :scheme-specific-part ssp :fragment fragment}))

(defn parse-scheme-specific
  "Parse the scheme specific part into authority, path, and query
parts."
  [ssp]
  (let [[_ authority path query] (re-find #"^//([^/]+)([^\?]+)\??(.*)$" ssp)
        query (if (.contains ssp "?") query nil)]
    {:authority authority :path path :query query}))

(defn parse-authority
  "Parse the authority part into user-info, hostname, and port parts."
  [authority]
  (let [[_ user-info host port] (re-find #"^([^@]+(?=@))?@?([^:]+):?(.*)$" authority)
        port (if (empty? port) nil (Integer/parseInt port))]
    {:user-info user-info :host host :port port}))

(defn parse-uri
  "Parse a URI string."
  [uri]
  (let [generic (parse-generic uri)
        scheme-specific (parse-scheme-specific (:scheme-specific-part generic))
        authority (parse-authority (:authority scheme-specific))]
    (->> (merge generic scheme-specific authority)
         (filter (fn [[key value]] (not (nil? value))))
         (into {}))))
