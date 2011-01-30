(ns org.bovinegenius.uri.parser)

(defn parse-generic
  "Takes a URI string and parses it into scheme, scheme-specific-part,
and fragment parts."
  [uri]
  (let [[_ scheme ssp fragment] (re-find #"^(.*?):([^#]+)#?(.*)$" uri)
        fragment (if (.contains uri "#") fragment nil)]
    {:scheme scheme :scheme-specific-part ssp :fragment fragment}))
