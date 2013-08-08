(ns org.bovinegenius.uri_test
  (:use (org.bovinegenius exploding-fish)
        (org.bovinegenius.exploding-fish query-string)
        (clojure test))
  (:require (org.bovinegenius.exploding-fish [parser :as parse]))
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
                               :scheme-relative "//www.fred.net/"})))
           {:host "www.fred.net",
            :path "/",
            :authority "www.fred.net",
            :scheme "http",
            :scheme-relative "//www.fred.net/"})))
  (let [uri-string "http://www.domain.net/with?query=and#fragment"]
    (is (= (into {} (seq (uri uri-string)))
           (into {} (seq (uri (URI. uri-string))))
           (into {} (seq (uri (URL. uri-string))))
           (into {} (seq (uri {:host "www.domain.net",
                               :query "query=and",
                               :path "/with",
                               :authority "www.domain.net",
                               :scheme "http",
                               :scheme-relative "//www.domain.net/with?query=and",
                               :fragment "fragment"})))           
           {:host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net",
            :scheme "http",
            :scheme-relative "//www.domain.net/with?query=and",
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
                               :scheme-relative "//www.domain.net:8080/with?query=and",
                               :fragment "fragment"})))
           {:port 8080,
            :host "www.domain.net",
            :query "query=and",
            :path "/with",
            :authority "www.domain.net:8080",
            :scheme "http",
            :scheme-relative "//www.domain.net:8080/with?query=and",
            :fragment "fragment"}))))

(deftest partial-test
  (is (= (uri->map (uri "/some/path"))
         {:path "/some/path" :scheme-relative "/some/path"}))
  (is (= (uri->map (uri "http:/stuff?"))
         {:scheme "http", :scheme-relative "/stuff?" :path "/stuff" :query ""}))
  (is (= (uri->map (uri "http://stuff?"))
         {:scheme "http", :authority "stuff", :host "stuff",
          :scheme-relative "//stuff?", :query ""}))
  (is (= (uri->map (uri "file:/some/path"))
         {:scheme "file", :scheme-relative "/some/path" :path "/some/path"}))
  (is (= (uri->map (uri "file:///some/path"))
         {:scheme "file", :scheme-relative "///some/path" :path "/some/path"})))

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
    (is (= (scheme-relative string)
           (scheme-relative java-uri)
           (scheme-relative java-url)
           (scheme-relative uri-obj)
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
          :scheme-relative "//www.test.com:8080/some/stuff.html",
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

(deftest param-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= "http://www.test.net/some/path?x=y&a=7&d+x=m%3Df&m=2"
           (param url "a" 7)))
    (is (= "http://www.test.net/some/path?x=y&a=w&d+x=m%3Df&a=x&m=2&a=7"
           (param url "a" 7 2)))
    (is (= (param "/some/path/stuff?thing=value" "thing" "new-value")
           "/some/path/stuff?thing=new-value"))
    (is (= "http://www.test.net/some/path?x=y&a=w&d+x=7&a=x&m=2"
           (param url "d x" 7)))
    (is (= "http://www.test.net/some/path?x=y&a=w&d+x=m%3Df&a=7&m=2"
           (param url "a" 7 1)))))

(deftest param-raw-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= "http://www.test.net/some/path?x=y&a=7&d%20x=m%3Df&m=2"
           (param-raw url "a" 7)))
    (is (= "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2&a=7"
           (param-raw url "a" 7 2)))
    (is (= "http://www.test.net/some/path?x=y&a=w&d%20x=7&a=x&m=2"
           (param-raw url "d%20x" 7)))
    (is (= "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=7&m=2"
           (param-raw url "a" 7 1)))))

(deftest params-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= ["w" "x"]
           (params url "a")))
    (is (= ["m=f"]
           (params url "d x")))))

(deftest params-raw-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= ["w" "x"]
           (params-raw url "a")))
    (is (= ["m%3Df"]
           (params-raw url "d%20x")))
    (is (= []
           (params-raw url "d x")))))

(deftest query-map-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= {"x" "y"
            "a" "x"
            "d x" "m=f"
            "m" "2"}
           (query-map url)))))

(deftest raw-keys-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= ["x" "a" "d%20x" "a" "m"]
           (raw-keys url)))))

(deftest query-keys-test
  (let [url "http://www.test.net/some/path?x=y&a=w&d%20x=m%3Df&a=x&m=2"]
    (is (= ["x" "a" "d x" "a" "m"]
           (query-keys url)))))

(deftest normalize-path-test
  (let [uri "http://www.example.com/some/path/../awesome/./thing/../path/here?x=y&a=b#a-fragment"]
    (is (= "http://www.example.com/some/awesome/path/here?x=y&a=b#a-fragment"
           (normalize-path uri)))))

(deftest resolve-path-test
  (let [the-uri "http://www.example.com/some/path/../awesome/./thing/../path/here?x=y&a=b#a-fragment"]
    (is (= "http://www.example.com/some/awesome/path/new/path/stuff?x=y&a=b#a-fragment"
           (resolve-path the-uri "new/path/stuff")))
    (is (= "http://www.example.com/some/awesome/path/new/path/stuff?x=y&a=b#a-fragment"
           (normalize-path (resolve-path the-uri "new/path/stuff"))))
    (is (= "http://www.example.com/new/path/stuff?x=y&a=b#a-fragment"
           (resolve-path the-uri "/new/path/stuff")))
    (is (= "http://www.example.com/some/awesome/path/here/new/path/stuff?x=y&a=b#a-fragment"
           (resolve-path "http://www.example.com/some/awesome/path/here/?x=y&a=b#a-fragment"
                         "new/path/stuff")))
    (is (= (str (resolve-path (uri "http://www.example.com/asdf/x") "y/asdf"))
           "http://www.example.com/asdf/y/asdf"))
    (is (= (str (resolve-path (uri "http://www.example.com/asdf/x") (uri "y/asdf")))
           "http://www.example.com/asdf/y/asdf"))))

(deftest absolute?-test
  (is (absolute? "http://www.test.net/new/path?x=y&a=w"))
  (is (not (absolute? "/new/path?x=y&a=w"))))

(deftest resolve-uri-test
  (let [the-base-uri "http://a/b/c/d;p?q=1/2"]
    (is (= (resolve-uri the-base-uri "g")
           "http://a/b/c/g"))
    (is (= (resolve-uri the-base-uri "./g")
           "http://a/b/c/g"))
    (is (= (resolve-uri the-base-uri "g/")
           "http://a/b/c/g/"))
    (is (= (resolve-uri the-base-uri "/g")
           "http://a/g"))
    (is (= (resolve-uri the-base-uri "//g")
           "http://g"))
    (is (= (resolve-uri the-base-uri (URI. "//g"))
           (URI. "http://g")))
    (is (= (resolve-uri the-base-uri "g?y")
           "http://a/b/c/g?y"))
    (is (= (resolve-uri (URI. the-base-uri) "g?y")
           (URI. "http://a/b/c/g?y")))
    (is (let [resolved-uri (resolve-uri the-base-uri "g?y/./x")]
          (or (= resolved-uri "http://a/b/c/g?y/./x")
             (= resolved-uri "http://a/b/c/g?y/x"))))
    (is (let [resolved-uri (resolve-uri the-base-uri "g?y/../x")]
          (or (= resolved-uri "http://a/b/c/g?y/../x")
             (= resolved-uri "http://a/b/c/x"))))
    (is (= (resolve-uri the-base-uri "g#s")
           "http://a/b/c/g#s"))
    (is (let [resolved-uri (resolve-uri the-base-uri "g#s/./x")]
          (or (= resolved-uri "http://a/b/c/g#s/./x")
             (= resolved-uri "http://a/b/c/g#s/x"))))
    (is (let [resolved-uri (resolve-uri the-base-uri "g#s/../x")]
          (or (= resolved-uri "http://a/b/c/g#s/../x")
             (= resolved-uri "http://a/b/c/x"))))
    (is (= (resolve-uri the-base-uri "./")
           "http://a/b/c/"))
    (is (= (resolve-uri the-base-uri "../")
           "http://a/b/"))
    (is (= (resolve-uri the-base-uri "../g")
           "http://a/b/g"))
    (is (= (resolve-uri the-base-uri "../../")
           "http://a/"))
    (is (= (resolve-uri the-base-uri "../../g")
           "http://a/g"))))