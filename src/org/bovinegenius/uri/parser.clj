(ns org.bovinegenius.uri.parser)

(defn parse-generic
  "Takes a URI string and parses it into scheme, scheme-specific-part,
and fragment parts."
  [uri]
  (if uri
    (let [[_ scheme ssp fragment] (re-find #"^(.*?):([^#]+)#?(.*)$" uri)
          fragment (if (.contains uri "#") fragment nil)]
      {:scheme scheme :scheme-specific-part ssp :fragment fragment})
    {:scheme nil :scheme-specific-part nil :fragment nil}))

(defn parse-scheme-specific
  "Parse the scheme specific part into authority, path, and query
parts."
  [ssp]
  (if ssp
    (let [[_ authority path query] (re-find #"^//(.*?(?=\.\.|\./|/))([^\?]+)\??(.*)$" ssp)
          query (if (.contains ssp "?") query nil)]
      {:authority authority :path path :query query})
    {:authority nil :path nil :query nil}))

(defn parse-authority
  "Parse the authority part into user-info, hostname, and port parts."
  [authority]
  (if authority
    (let [[_ user-info host port] (re-find #"^([^@]+(?=@))?@?([^:]+):?(.*)$" authority)
          port (if (empty? port) nil (Integer/parseInt port))]
      {:user-info user-info :host host :port port})
    {:user-info nil :host nil :port nil}))

(defn clean-map
  "Remove keys that have a nil value, and that are in the given set."
  [data keys]
  (reduce (fn [data key]
            (if (data key)
              data
              (dissoc data key)))
          data keys))

(defn parse-uri
  "Parse a URI string."
  [uri]
  (let [generic (parse-generic uri)
        scheme-specific (parse-scheme-specific (:scheme-specific-part generic))
        authority (parse-authority (:authority scheme-specific))]
    (->> (merge generic scheme-specific authority)
         (filter (fn [[key value]] (not (nil? value))))
         (into {}))))
