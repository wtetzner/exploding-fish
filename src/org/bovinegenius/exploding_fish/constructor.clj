;; Copyright (c) 2011,2012 Walter Tetzner

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

(ns org.bovinegenius.exploding-fish.constructor)

(defn ^String authority
  "Takes a map with :user-info, :host, and :port keys, and returns a
string."
  [{:keys [user-info host port]}]
  (let [user-info (when user-info (format "%s@" user-info))
        port (when port (format ":%s" port))]
    (str user-info host port)))

(defn ^String scheme-specific
  "Takes a map with :authority, :path, and :query keys, and returns a
string."
  [{:keys [authority path query]}]
  (let [query (when query (format "?%s" query))
        authority (when-not (empty? authority)
                    (format "//%s" authority))]
    (str authority path query)))

(defn ^String uri-string
  "Takes a map representing a URI, and returns a string."
  [{:keys [scheme scheme-relative fragment] :as uri-data}]
  (let [scheme (when scheme (format "%s:" scheme))
        fragment (when fragment (format "#%s" fragment))
        auth (or (:authority uri-data) (authority uri-data))
        ssp (or scheme-relative
                (scheme-specific
                 (assoc uri-data :authority authority)))]
    (str scheme ssp fragment)))
