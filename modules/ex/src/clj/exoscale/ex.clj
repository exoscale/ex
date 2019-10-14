(ns exoscale.ex
  (:refer-clojure :exclude [ex-info derive underive ancestors descendants
                            parents isa? set-validator!])
  (:require [clojure.spec.alpha :as s]
            [clojure.core.specs.alpha :as cs]
            [clojure.string :as str]))

(defonce hierarchy (atom (make-hierarchy)))

(s/fdef derive
        :args (s/cat :tag ::type
                     :parent ::type))
(defn derive
  "Like clojure.core/derive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/derive tag parent))

(s/fdef underive
        :args (s/cat :tag ::type
                     :parent ::type))
(defn underive
  "Like clojure.core/underive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/underive tag parent))

(s/fdef ancestors
        :args (s/cat :tag ::type))
(defn ancestors
  "Like clojure.core/ancestors but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/ancestors @hierarchy tag))

(s/fdef descendants
        :args (s/cat :tag ::type))
(defn descendants
  "Like clojure.core/descendants but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/descendants @hierarchy tag))

(s/fdef parents
        :args (s/cat :tag ::type))
(defn parents
  "Like clojure.core/parents but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/parents @hierarchy tag))

(s/fdef isa?
  :args (s/cat :child ::type
               :parent ::type))
(defn isa?
  "Like clojure.core/isa? but scoped on our ex-info type hierarchy"
  [child parent]
  (clojure.core/isa? @hierarchy child parent))

(defmulti ex-data-spec :type)
(defmethod ex-data-spec :default [_] (s/keys :opt-un [::type]))

(s/def ::type qualified-keyword?)
(s/def ::ex-data (s/multi-spec ex-data-spec :type))
(s/def ::exception #(instance? Exception %))

(s/fdef set-ex-data-spec!
  :args (s/cat :type ::type
               :spec (s/or :kw qualified-keyword?
                           :spec-obj s/spec?)))
(defn set-ex-data-spec!
  [type spec]
  (defmethod ex-data-spec type [_] spec))

(defn ^:no-doc assert-ex-data-valid
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
  (vary-meta d assoc ::exception ex ::message (ex-message ex)))

(s/fdef catch-data*
  :args (s/cat :exception ::exception
               :type-key ::type
               :handler ifn?
               :continue ifn?))
(defn catch-data*
  "catch-data as a function, takes an exception, tries to match it
  against `type-key` from its ex-data.type, on match returns call to
  `handler` with the ex-data, otherwise `continue` with the original
  exception."
  [e type-key handler continue]
  (let [d (ex-data e)]
    (if (and d (isa? (:type d) type-key))
      (do (assert-ex-data-valid d)
          (handler (data+ex d e)))
      (continue e))))

(s/def ::try$catch
  (s/cat :clause #{'catch}
         :class symbol?
         :binding symbol?
         :body (s/* any?)))

(s/def ::try$catch-data
  (s/cat :clause #{'catch-data}
         :type ::type
         :binding ::cs/binding-form
         :body (s/* any?)))

(s/def ::try$finally
  (s/cat :clause #{'finally}
         :body (s/* any?)))

(s/def ::try$body
  (s/+ #(not (and (coll? %)
                  (#{'catch 'catch-data 'finally}
                   (first %))))))

(s/fdef try+
  :args (s/cat
          :body ::try$body
          :clauses (s/+
                    (s/or
                     :catch ::try$catch
                     :catch-data ::try$catch-data
                     :finally ::try$finally))))
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
  {:style/indent 0}
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

(def types
  #{::unavailable ::interrupted ::incorrect ::forbidden ::unsupported
    ::not-found ::conflict ::fault ::busy})

(s/fdef ex-info
  :args (s/cat :msg string?
               :type+deriving (s/or :type ::type
                                    :type+deriving (s/cat :type ::type
                                                          :deriving (s/? (s/coll-of ::type))))
               :data (s/? (s/nilable ::ex-data))
               :cause (s/? (s/nilable ::exception))))
(defn ex-info
  "Like `clojure.core/ex-info` but adds validation of the ex-data,
  automatic setting of the data `:type` from argument and potential
  derivation. You can specify type as either a keyword or a tuple of
  `[<type> [<derivation>+]]`."
  ([msg type]
   (ex-info msg type nil))
  ([msg type data]
   (ex-info msg type data nil))
  ([msg type data cause]
   (let [coll-type? (coll? type)
         type' (cond-> type
                 coll-type?
                 first)
         deriving (when coll-type? (second type))
         data' (assoc data :type type')]
     (assert-ex-data-valid data')
     (run! #(derive type' %) deriving)
     (clojure.core/ex-info msg
                           data'
                           cause))))

;;; Sugar for common exceptions

(defmacro ^:no-doc gen-ex-fn-for-type
  [type]
  (let [sym (symbol (str "ex-" (name type)))
        msg 'msg
        data 'data
        cause 'cause]
    `(do
       (s/fdef ~sym
         :args (s/cat :msg (s/and string? (complement str/blank?))
                      :data (s/? (s/nilable ::ex-data))
                      :cause (s/? (s/nilable ::exception))))
       (defn ~sym
        ~(format (str "Returns an ex-info with ex-data `:type` set to %s. Rest of "
                      "the arguments match `ex-info`")
                 type)
        ([~msg ~data]
         (~sym ~msg ~data nil))
        ([~msg ~data ~cause]
         (let [~data (assoc ~data :type ~type)]
           (ex-info ~msg ~type ~data ~cause)))))))
(run! (fn [t] (eval `(gen-ex-fn-for-type ~t))) types)

(s/fdef invalid-spec
  :args (s/cat :spec qualified-keyword?
               :x any?))
(defn invalid-spec
  "Returns an ex-info when value `x` does not conform to spec `spex`"
  [spec x]
  (exoscale.ex/ex-info (s/explain-str spec x)
                       [::invalid-spec [::incorrect]]
                       {:explain-data (s/explain-data spec x)}))
