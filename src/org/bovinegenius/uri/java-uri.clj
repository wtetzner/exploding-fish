(ns org.bovinegenius.uri.java-uri
  (:import (java.net URI)))

(extend java.net.URI
  UniversalResourceIdentifier
  {:scheme (fn ([^URI self] (.getScheme self))
             ([^URI self ^String new-scheme]
                (cond (-> (.getRawSchemeSpecificPart self) empty? not)
                      (URI. new-scheme (.getRawSchemeSpecificPart self)
                            (.getRawFragment self))
                      
                      (.getRawUserInfo self)
                      (URI. new-scheme (.getRawUserInfo self)
                            (.getHost self) (.getPort self)
                            (.getRawPath self) (.getRawQuery self)
                            (.getRawFragment self))

                      (.getHost self)
                      (URI. new-scheme (.getHost self) (.getRawPath self)
                            (.getRawFragment self))

                      :else (URI. new-scheme (.getRawAuthority self)
                                  (.getRawPath self) (.getRawQuery self)
                                  (.getRawFragment self)))))})
