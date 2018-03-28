[![Build Status](https://travis-ci.org/wtetzner/exploding-fish.png?branch=master)](https://travis-ci.org/wtetzner/exploding-fish)

Exploding Fish
--------------

Exploding Fish is a library for manipulating URIs. It defines a
UniformResourceIdentifier protocol, and implements it on a custom Uri
class, as well as java.net.URI, java.net.URL, and java.lang.String.

Usage
-----

To use with Leiningen, add

    :dependencies [[org.bovinegenius/exploding-fish "0.3.5"]]

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
except that it ensures that its values are always consistent.

For example,

    user> (:host (uri "http://www.example.com/"))
    "www.example.com"
    user> (assoc (uri "http://www.example.com/") :port 8080)
    #<Uri http://www.example.com:8080/>
    user> (def the-uri (uri "http://www.example.com/"))
    #'user/the-uri
    user> (:scheme-relative the-uri)
    "//www.example.com/"
    user> (:scheme-relative (assoc the-uri :port 8080))
    "//www.example.com:8080/"

### Accessor Functions

While a benifit of using the Uri class is that you can treat it as a
map, the benefit of the accessor functions is that they work on
different types.

* scheme
* scheme-relative
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


#### Query Params

There are several functions meant to make dealing with query string parameters easier.

`query-pairs` returns the query string as an alist, decoded using
`org.bovinegenius.exploding-fish/*default-encoding*`. The encoding can
be bound using the `default-encoding` macro. If not specifically
bound, the value of `*default-encoding*` is "UTF-8".

    user> (query-pairs "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2")
    [["x" "y"] ["a" "w"] ["d x" "m=f"] ["a" "x"] ["m" "2"]]

`params` will lookup the value of all params with the given key. If no
key is given, all param values are returned.

    user> (params "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "a")
    ["w" "x"]
    user> (params "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "d x")
    ["m=f"]

`param` will lookup the *last* value with the given key. If a value is
given, it will "set" the value of the given key. In the case where
there are multiple values for the given key, it will set the first
one, and remove the remaining. If a key, value, and index are given,
it will set the value at the given index. If the index is larger than
any value with that key, it will append a key-value pair.

    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "d x")
    "m=f"
    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "a")
    "x"
    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "d x" "new-value")
    "http://www.test.net/some/path?x=y&a=w&d+x=new-value&a=x&m=2"
    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "a" "new-value")
    "http://www.test.net/some/path?x=y&a=new-value&d+x=m%3Df&m=2"
    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "a" "new-value" 1)
    "http://www.test.net/some/path?x=y&a=w&d+x=m%3Df&a=new-value&m=2"
    user> (param "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2" "a" "new-value" 2)
    "http://www.test.net/some/path?x=y&a=w&d+x=m%3Df&a=x&m=2&a=new-value"

`query-map` will build a map from the query string. The last value for
a given key will "win."

    user> (query-map "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2")
    {"x" "y", "a" "x", "d x" "m=f", "m" "2"}

`query-keys` will return all of the keys (not distinct) in the query
string.

    user> (query-keys "http://www.test.net/some/path?x=y&a=w&d%20x=m%3df&a=x&m=2")
    ["x" "a" "d x" "a" "m"]

`raw-pairs`, `params-raw`, `param-raw`, and `raw-keys` are versions of the above functions that will not automatically URL encode and decode the parameters.

#### Paths

There are some path functions to make working with URIs easier.

`normalize-path` will normalize a path, collapsing things like
'/some/url/../path' to '/some/path'.

    user> (normalize-path "http://www.test.net/some/uri/../path/./here?x=y&a=w")
    "http://www.test.net/some/path/here?x=y&a=w"

`resolve-path` will resolve a path against the given URI.

    user> (resolve-path "http://www.test.net/some/uri/../path/./here?x=y&a=w" "new/path")
    "http://www.test.net/some/path/new/path?x=y&a=w"
    user> (resolve-path "http://www.test.net/some/uri/../path/./here?x=y&a=w" "/new/path")
    "http://www.test.net/new/path?x=y&a=w"

`absolute?` returns true if the URI is absolute.

    user> (absolute? (uri "http://www.test.net/new/path?x=y&a=w"))
    true
    user> (absolute? (uri "/new/path?x=y&a=w"))
    false

#### Resolving URIs

`resolve-uri` allows you to resolve a partial URI against a full URI.

    user=> (resolve-uri "http://a/b/c/d;p?q=1/2" "http://e/f")
    "http://e/f"
    user=> (resolve-uri "http://a/b/c/d;p?q=1/2" "g")
    "http://a/b/c/g"
    user=> (resolve-uri "http://a/b/c/d;p?q=1/2" "./g")
    "http://a/b/c/g"
    user=> (resolve-uri "http://a/b/c/d;p?q=1/2" "//g")
    "http://g"
    user=> (resolve-uri "http://a/b/c/d;p?q=1/2" "g?y/../x")
    "http://a/b/c/g?y/../x"

License
-------

Exploding Fish is under the MIT License.

Copyright (c) 2011,2012,2013 Walter Tetzner

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

