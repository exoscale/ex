(ns exoscale.ex.exception
  (:require [exoscale.ex :as ex]))

(defmacro gen-ex-fn-for-type
  [type]
  (let [sym (symbol (name type))]
    `(defn ~sym
       ~(format (str "Returns an ex-info with ex-data `:type` set to %s. Rest of"
                     "the arguments match `ex-info`")
                type)
       ([msg# data#]
        (~sym msg# data# nil))
       ([msg# data# cause#]
        (let [data# (assoc data# :type ~type)]
          (ex/assert-ex-data-valid data#)
          (ex-info msg# data# cause#))))))

(defmacro gen-base-types
  []
  `(do
     ~@(map (fn [t] `(gen-ex-fn-for-type ~t))
            ex/types)))

(gen-base-types)
