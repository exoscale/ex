(ns exoscale.ex.http
  (:require [exoscale.ex :as ex]))

(defmulti response->ex-info!
  "Throws the matching ex exception for status HTTP response.
  Clients are expected to have already handled success status codes."
  :status)

(defn- ex!
  [type message data]
  (throw (ex/ex-info message
                     [:exoscale.ex.http/response [type]]
                     (assoc data
                            ;; backward compat
                            :response data))))

(defmacro def-response->ex [status type message]
  `(defmethod response->ex-info! ~status
     [response#]
     (ex! ~type ~message response#)))

(def-response->ex :default :exoscale.ex/fault "HTTP Error")
(def-response->ex 400 :exoscale.ex/incorrect "Bad Request")
(def-response->ex 401 :exoscale.ex/forbidden "Unauthorized")
(def-response->ex 403 :exoscale.ex/forbidden "Forbidden")
(def-response->ex 404 :exoscale.ex/not-found "Not Found")
(def-response->ex 405 :exoscale.ex/unsupported "Method Not Allowed")
(def-response->ex 409 :exoscale.ex/conflict "Conflict")
(def-response->ex 429 :exoscale.ex/busy "Too Many Requests")
(def-response->ex 500 :exoscale.ex/fault "Internal Server Error")
(def-response->ex 501 :exoscale.ex/unsupported "Not Implemented")
(def-response->ex 503 :exoscale.ex/busy "Service Unavailable")
(def-response->ex 502 :exoscale.ex/unavailable "Bad Gateway")
(def-response->ex 504 :exoscale.ex/unavailable "Gateway Timeout")
