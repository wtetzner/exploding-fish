(ns org.bovinegenius.uri_test
  (:use (org.bovinegenius exploding-fish)
        (org.bovinegenius.exploding-fish query-string)
        (clojure test))
  (:import (java.net URI URL)))

(deftest uri-test
  (let [uri-string "http://www.fred.net/"]
    (is (= (into {} (seq (uri uri-string)))
           (into {} (seq (uri (URI. uri-string))))
           (into {} (seq (uri (URL. uri-string))))
           (into {} (seq (uri {:host "www.fred.net",
                               :path "/",
                               :authority "www.fred.net",
                               :scheme "http",
                               :scheme-specific-part "//www.fred.net/"})))
           {:host "www.fred.net",
            :path "/",
            :authority "www.fred.net",
            :scheme "http",
            :scheme-specific-part "//www.fred.net/"})))
  (let [uri-string "http://www.domain.net/with?query=and#fragment"]
    (is (= (into {} (seq (uri uri-string)))
           (into {} (seq (uri (URI. uri-string))))
           (into {} (seq (uri (URL. uri-string))))
           (into {} (seq (uri {:host "www.domain.net",
                               :query "query=and",
                               :path "/with",
                               :authority "www.domain.net",
                               :scheme "http",
                               :scheme-specific-part "//www.domain.net/with?query=and",
                               :fragment "fragment"})))           
           {:host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net",
            :scheme "http",
            :scheme-specific-part "//www.domain.net/with?query=and",
            :fragment "fragment"})))
  (let [uri-string "http://www.domain.net:8080/with?query=and#fragment"]
    (is (= (into {} (seq (uri uri-string)))
           (into {} (seq (uri (URI. uri-string))))
           (into {} (seq (uri (URL. uri-string))))
           (into {} (seq (uri {:port 8080,
                               :host "www.domain.net",
                               :query "query=and",
                               :path "/with",
                               :authority "www.domain.net:8080",
                               :scheme "http",
                               :scheme-specific-part "//www.domain.net:8080/with?query=and",
                               :fragment "fragment"})))
           {:port 8080,
            :host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net:8080",
            :scheme "http",
            :scheme-specific-part "//www.domain.net:8080/with?query=and",
            :fragment "fragment"}))))

(deftest protocol-fns
  (let [string "http://someone@www.pureawesomeness.org:8088/some/path?x=y&a=b#awesomeness"
        java-uri (URI. string)
        java-url (URL. string)
        uri-obj (uri string)]
    (is (= (scheme string)
           (scheme java-uri)
           (scheme java-url)
           (scheme uri-obj)
           "http"))
    (is (= (scheme-specific-part string)
           (scheme-specific-part java-uri)
           (scheme-specific-part java-url)
           (scheme-specific-part uri-obj)
           "//someone@www.pureawesomeness.org:8088/some/path?x=y&a=b"))
    (is (= (authority string)
           (authority java-uri)
           (authority java-url)
           (authority uri-obj)
           "someone@www.pureawesomeness.org:8088"))
    (is (= (user-info string)
           (user-info java-uri)
           (user-info java-url)
           (user-info uri-obj)
           "someone"))
    (is (= (host string)
           (host java-uri)
           (host java-url)
           (host uri-obj)
           "www.pureawesomeness.org"))
    (is (= (port string)
           (port java-uri)
           (port java-url)
           (port uri-obj)
           8088))
    (is (= (path string)
           (path java-uri)
           (path java-url)
           (path uri-obj)
           "/some/path"))
    (is (= (query string)
           (query java-uri)
           (query java-url)
           (query uri-obj)
           "x=y&a=b"))
    (is (= (fragment string)
           (fragment java-uri)
           (fragment java-url)
           (fragment uri-obj)
           "awesomeness"))))

(deftest uri->map-test
  (is (= (uri->map (URI. "http://www.test.com:8080/some/stuff.html#frag"))
         {:path "/some/stuff.html",
          :scheme "http",
          :authority "www.test.com:8080",
          :host "www.test.com",
          :port 8080,
          :scheme-specific-part "//www.test.com:8080/some/stuff.html",
          :fragment "frag"})))

(deftest query-list-test
  (is (= (query-list (uri "http://www.some-thing.com/a/path?one=1&two=2&x=&y"))
         '("one=1" "two=2" "x=" "y")))
  (is (= (query-list (uri "http://www.some-thing.com/a/path?one=1&two=2&x=&y"))
         (query-list "http://www.some-thing.com/a/path?one=1&two=2&x=&y")
         (query-list (URI. "http://www.some-thing.com/a/path?one=1&two=2&x=&y"))
         (query-list (URL. "http://www.some-thing.com/a/path?one=1&two=2&x=&y")))))

(deftest query-pairs-test
  (is (= (query-pairs (uri "http://www.some-thing.com/a/path?one=1&two=2&x=&y"))
         '(["one" "1"] ["two" "2"] ["x" ""] ["y" nil])))
  (is (= (query-pairs (uri "http://www.some-thing.com/a/path?one=1&two=2&x=&y&&=&=a"))
         '(["one" "1"] ["two" "2"] ["x" ""] ["y" nil] ["" nil] ["" ""] ["" "a"])))
  (is (= (alist->query-string
          (query-pairs (uri "http://www.some-thing.com/a/path?one=1&two=2&x=&y&&=&=a")))
         "one=1&two=2&x=&y&&=&=a"))
  (is (= (alist->query-string
          (query-pairs "http://www.some-thing.com/a/path?one=1&two=2&x=&y&&=&=a"))
         "one=1&two=2&x=&y&&=&=a")))
