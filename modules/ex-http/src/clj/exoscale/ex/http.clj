(ns exoscale.ex.http
  (:require [exoscale.ex :as ex]))

(defmulti response->ex-info!
  "Throws the matching ex exception for status HTTP response.
  Clients are expected to have already handled success status codes."
  :status)

(defmethod response->ex-info!
  :default
  [resp] (ex/ex-fault! "HTTP Error" {:response resp}))

(defmethod response->ex-info!
  404
  [resp] (ex/ex-not-found! "Not Found" {:response resp}))

(defmethod response->ex-info!
  403
  [resp] (ex/ex-forbidden! "Forbidden" {:response resp}))

(defmethod response->ex-info!
  401
  [resp] (ex/ex-forbidden! "Unauthorized" {:response resp}))

(defmethod response->ex-info!
  400
  [resp] (ex/ex-incorrect! "Bad Request" {:response resp}))

(defmethod response->ex-info!
  409
  [resp] (ex/ex-conflict! "Conflict" {:response resp}))

(defmethod response->ex-info!
  405
  [resp] (ex/ex-unsupported! "Method Not Allowed" {:response resp}))

(defmethod response->ex-info!
  429
  [resp] (ex/ex-busy! "Too Many Requests" {:response resp}))

(defmethod response->ex-info!
  500
  [resp] (ex/ex-fault! "Internal Server Error" {:response resp}))

(defmethod response->ex-info!
  501
  [resp] (ex/ex-unsupported! "Not Implemented" {:response resp}))

(defmethod response->ex-info!
  503
  [resp] (ex/ex-busy! "Service Unavailable" {:response resp}))

(defmethod response->ex-info!
  502
  [resp] (ex/ex-unavailable! "Bad Gateway" {:response resp}))

(defmethod response->ex-info!
  504
  [resp] (ex/ex-unavailable! "Gateway Timeout" {:response resp}))

