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

(ns org.bovinegenius.exploding-fish.constructor
  (:require (org.bovinegenius.exploding-fish
             [parser :as parse])))

(defn ^String authority
  "Takes a map with :user-info, :host, and :port keys, and returns a
string."
  [{:keys [user-info host port]}]
  (let [user-info (if user-info (format "%s@" user-info) nil)
        port (if port (format ":%s" port) nil)]
    (str user-info host port)))

(defn ^String scheme-specific
  "Takes a map with :authority, :path, and :query keys, and returns a
string."
  [{:keys [authority path query] :as m}]
  (let [query (if query (format "?%s" query) nil)
        authority (if (empty? authority)
                    nil
                    (format "//%s" authority))]
    (str authority path query)))

(defn ^String uri-string
  "Takes a map representing a URI, and returns a string."
  [{:keys [scheme scheme-relative fragment] :as uri-data}]
  (let [scheme (if scheme (format "%s:" scheme) nil)
        fragment (if fragment (format "#%s" fragment) nil)
        auth (or (:authority uri-data) (authority uri-data))
        ssp (or scheme-relative
                (scheme-specific
                 (assoc uri-data :authority auth)))]
    (str scheme ssp fragment)))

(defn prepare-map
  "Takes a map representing a URI, and populates missing data."
  [{:keys [scheme scheme-relative fragment] :as uri-data}]
  (let [auth (or (:authority uri-data) (authority uri-data))
        auth (if (empty? auth) nil auth)
        ssp (or scheme-relative
                (scheme-specific
                 (assoc uri-data :authority auth)))
        ssp (if (empty? ssp) nil ssp)]
    (assoc uri-data
           :authority auth
           :scheme-relative ssp)))
