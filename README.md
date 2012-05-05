Exploding Fish
--------------

Exploding Fish is a library for manipulating URIs. It defines a
UniformResourceIdentifier protocol, and implements it on a custom Uri
class, as well as java.net.URI, java.net.URL, and java.lang.String.

Usage
-----

To use with Leiningen, add

    :dependencies [[org.bovinegenius/exploding-fish "0.1.0"]]

to your project.clj.

You can use it in a source file like this:

    (:use (org.bovinegenius exploding-fish))

or

    (:require (org.bovinegenius [exploding-fish :as uri]))

The functions in Exploding Fish that are used to access URI values
work on objects of type Uri, java.net.URI, java.net.URL, and
java.lang.String.

### Creating a Uri

Exploding Fish comes with a Uri class, which behaves like a map,
except that it ensures that it's values are always consistent.

For example,

    user> (:host (uri "http://www.example.com/"))
    "www.example.com"
    user> (assoc (uri "http://www.example.com/") :port 8080)
    #<Uri http://www.example.com:8080/>
    user> (def the-uri (uri "http://www.example.com/"))
    #'user/the-uri
    user> (:scheme-specific-part the-uri)
    "//www.example.com/"
    user> (:scheme-specific-part (assoc the-uri :port 8080))
    "//www.example.com:8080/"

### Accessor Functions

While a benifit of using the Uri class is that you can treat it as a
map, the benefit of the accessor functions is that they work on
different types.

* scheme
* scheme-specific-part
* authority
* user-info
* host
* port
* path
* query
* fragment

#### Example Usage

    user> (scheme "http://www.example.com/")
    "http"
    user> (scheme (URI. "http://www.example.com/"))
    "http"
    user> (fragment (URL. "http://www.example.com/#fragment"))
    "fragment"
    user> (fragment (uri "http://www.example.com/#fragment"))
    "fragment"

#### Updating URIs

The accessor functions can be used to update URIs as well, by
providing an additional argument.

    user> (fragment (uri "http://www.example.com/#fragment") nil)
    #<Uri http://www.example.com/>
    user> (host (uri "http://www.example.com/#fragment") "www.bovinegenius.org")
    #<Uri http://www.bovinegenius.org/#fragment>
    user> (host "http://www.example.com/#fragment" "www.bovinegenius.org")
    "http://www.bovinegenius.org/#fragment"
    user> (fragment (URI. "http://www.example.com/#fragment") "it-works-on-java-uris")
    #<URI http://www.example.com/#it-works-on-java-uris>

Other Namespaces
----------------

There are functions for dealing with query strings and paths in
org.bovinegenius.exploding-fish.query-string and
org.bovinegenius.exploding-fish.path, respectively.

### Query String

    user> (query-string->alist (query "http://www.example.com/?x=y&stuff=fred"))
    [["x" "y"] ["stuff" "fred"]]

You can take a query string apart into an alist, and put it back together as the exact same query string.

    user> (query-string->alist (query "http://www.example.com/?stuff=fred&&x=&=&=y"))
    [["stuff" "fred"] ["" nil] ["x" ""] ["" ""] ["" "y"]]
    user> (alist->query-string (query-string->alist (query "http://www.example.com/?stuff=fred&&x=&=&=y")))
    "stuff=fred&&x=&=&=y"

### Path

    user> (split-path "/some/path")
    ["" "some" "path"]
    user> (path/split-path "some/path")
    ["some" "path"]
    user> (resolve-path "/a/path/" "some/other/path")
    "/a/path/some/other/path"
    user> (resolve-path "/a/path/" "/some/other/path")
    "/some/other/path"
    user> (normalize "/some/path/../other/./stuff")
    "/some/other/stuff"

License
-------

Exploding Fish is under the MIT License.

Copyright (c) 2012 Walter Tetzner

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

