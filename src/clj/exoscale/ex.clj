(ns exoscale.ex
  (:require [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [derive underive ancestors descendants isa? parents
                            set-validator!]))

(defonce hierarchy (atom (make-hierarchy)))

(defn derive
  "Like clojure.core/derive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/derive tag parent))

(defn underive
  "Like clojure.core/underive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/underive tag parent))

(defn ancestors
  "Like clojure.core/ancestors but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/ancestors @hierarchy tag))

(defn descendants
  "Like clojure.core/descendants but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/descendants @hierarchy tag))

(defn parents
  "Like clojure.core/parents but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/parents @hierarchy tag))

(defn isa?
  "Like clojure.core/isa? but scoped on our ex-info type hierarchy"
  [child parent]
  (clojure.core/isa? @hierarchy child parent))

(defmulti ex-data-spec :type)
(s/def ::type qualified-keyword?)
(s/def ::ex-data (s/multi-spec ex-data-spec :type))
(defmethod ex-data-spec :default [_] (s/keys :opt-un [::type]))

(defn set-ex-data-spec!
  [type spec]
  (defmethod ex-data-spec type [_] spec))

(defn assert-ex-data-valid
  "ex-data Validator function"
  [ex-data]
  (s/assert ::ex-data ex-data))

(defn set-validator!
  "Sets validation failure handler globally"
  [f]
  (alter-var-root #'assert-ex-data-valid (constantly f)))

(defn ^:no-doc find-clause-fn
  [pred]
  (fn [x]
    (and (seq? x)
         (pred (first x)))))

(def ^:no-doc catch-clause? (find-clause-fn #{'catch 'finally}))
(def ^:no-doc catch-data-clause? (find-clause-fn #{'catch-data}))
(defn ^:no-doc data+ex
  [d ex]
  (vary-meta d assoc ::exception ex))

(defmacro try+
  "Like try but with support for ex-info/ex-data.

  If you pass a `catch-data` form it will try to match an
  ex-info :type key, or it's potential ancestors in the local hierarchy.

  ex-info clauses are checked first, in the order they were specified.
  catch-data will take as arguments a :type key, and a binding for
  the ex-data of the ex-info instance.

  (try
    [...]
    (catch-data ::something my-ex-data
      (do-something my-ex-info))
    (catch-data ::something-else {:as my-ex-data :keys [foo bar]}
      (do-something foo bar))
    (catch Exception e
      (do-something e))
    (catch OtherException e
      (do-something e))
    (finally :and-done))

  You can specify normal catch clauses for regular java errors and/or
  finally these are left untouched."
  {:style/indent 2}
  [& xs]
  (let [[body mixed-clauses]
        (split-with (complement (some-fn catch-clause? catch-data-clause?))
                    xs)
        clauses (filter catch-clause? mixed-clauses)
        ex-info-clauses (filter catch-data-clause? mixed-clauses)
        type-sym (gensym "ex-type-")
        data-sym (gensym "ex-data-")
        ex-sym (gensym "ex")]
    `(try
       ~@body
       ~@(cond-> clauses
           (seq ex-info-clauses)
           (conj `(catch clojure.lang.ExceptionInfo ~ex-sym
                    (let [~data-sym (ex-data ~ex-sym)
                          ~type-sym (:type ~data-sym)]
                      (cond
                        ~@(mapcat (fn [[_ type binding & body]]
                                    `[(isa? ~type-sym ~type)
                                      (do
                                        (assert-ex-data-valid ~data-sym)
                                        (let [~binding (data+ex ~data-sym ~ex-sym)]
                                          ~@body))])
                                  ex-info-clauses)
                        :else
                        ;; rethrow ex-info with other clauses since we
                        ;; have no match
                        (try (throw ~ex-sym)
                             ~@clauses)))))))))
