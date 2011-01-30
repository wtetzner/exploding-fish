(ns org.bovinegenius.uri.protocol)

(defprotocol UniversalResourceIdentifier
  "Protocol for dealing with URIs."
  (scheme
    [self] [self new-scheme]
    "Get or set the scheme component of the URI.")
  (scheme-specific-part
    [self] [self new-scheme-specific-part]
    "Get or set the scheme specific part of the URI.")
  (authority
    [self] [self new-authority]
    "Get or set the authority of the URI.")
  (user-info
    [self] [self new-user-info]
    "Get or set the user-info of the URI.")
  (host
    [self] [self new-host]
    "Get or set the host of the URI.")
  (port
    [self] [self new-port]
    "Get or set the port of the URI.")
  (path
    [self] [self new-path]
    "Get or set the path of the URI.")
  (query
    [self] [self new-query]
    "Get or set the query string of the URI.")
  (fragment
    [self] [self new-fragment]
    "Get or set the fragment of the URI."))
