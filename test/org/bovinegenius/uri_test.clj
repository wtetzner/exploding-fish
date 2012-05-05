(ns org.bovinegenius.uri_test
  (:use (org.bovinegenius uri)
        (org.bovinegenius.uri query-string)
        (clojure test))
  (:import (java.net URI URL)))

(deftest uri-test
  (let [uri-string "http://www.fred.net/"]
    (is (= (into {} (uri uri-string))
           (into {} (uri (URI. uri-string)))
           (into {} (uri (URL. uri-string)))
           (into {} (uri {:host "www.fred.net",
                          :path "/",
                          :authority "www.fred.net",
                          :scheme "http",
                          :scheme-specific-part "//www.fred.net/"}))
           {:host "www.fred.net",
            :path "/",
            :authority "www.fred.net",
            :scheme "http",
            :scheme-specific-part "//www.fred.net/"})))
  (let [uri-string "http://www.domain.net/with?query=and#fragment"]
    (is (= (into {} (uri uri-string))
           (into {} (uri (URI. uri-string)))
           (into {} (uri (URL. uri-string)))
           (into {} (uri {:host "www.domain.net",
                          :query "query=and",
                          :path "/with",
                          :authority "www.domain.net",
                          :scheme "http",
                          :scheme-specific-part "//www.domain.net/with?query=and",
                          :fragment "fragment"}))           
           {:host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net",
            :scheme "http",
            :scheme-specific-part "//www.domain.net/with?query=and",
            :fragment "fragment"})))
  (let [uri-string "http://www.domain.net:8080/with?query=and#fragment"]
    (is (= (into {} (uri uri-string))
           (into {} (uri (URI. uri-string)))
           (into {} (uri (URL. uri-string)))
           (into {} (uri {:port 8080,
                          :host "www.domain.net",
                          :query "query=and",
                          :path "/with",
                          :authority "www.domain.net:8080",
                          :scheme "http",
                          :scheme-specific-part "//www.domain.net:8080/with?query=and",
                          :fragment "fragment"}))
           {:port 8080,
            :host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net:8080",
            :scheme "http",
            :scheme-specific-part "//www.domain.net:8080/with?query=and",
            :fragment "fragment"}))))

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


