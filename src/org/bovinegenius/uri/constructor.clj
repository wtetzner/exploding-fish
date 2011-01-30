(ns org.bovinegenius.uri.constructor)

(defn ^String build-authority
  "Takes a map with :user-info, :host, and :port keys, and returns a
string."
  [{:keys [user-info host port]}]
  (let [user-info (if user-info (format "%s@" user-info) nil)
        port (if port (format ":%s" port) nil)]
    (str user-info host port)))

(defn ^String build-scheme-specific
  "Takes a map with :authority, :path, and :query keys, and returns a
string."
  [{:keys [authority path query]}]
  (let [query (if query (format "?%s" query) nil)]
    (str "//" authority path query)))

(defn ^String build-uri-string
  "Takes a map representing a URI, and returns a string."
  [{:keys [scheme scheme-specific-part fragment]}]
  (let [scheme (if scheme (format "%s:" scheme) nil)
        fragment (if fragment (format "#%s" fragment) nil)]
    (str scheme scheme-specific-part fragment)))
