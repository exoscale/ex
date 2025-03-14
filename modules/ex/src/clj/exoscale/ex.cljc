(ns exoscale.ex
  (:refer-clojure :exclude [ex-info derive underive ancestors descendants
                            parents isa? type])
  (:require #?(:cljs [cljs.repl :as r])
            [clojure.core.protocols :as p]
            [clojure.core.specs.alpha :as cs]
            [clojure.spec.alpha :as s]
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

(s/fdef type
  :args (s/cat :ex-data (s/nilable map?)))
(defn type
  "Returns `type` value from ex-data map (not the ex-info instance, use
  `ex-type` for this"
  [d]
  (or (::type d)
      ;; backward compatibility
      (:type d)))

(s/fdef ex-type
  :args (s/cat :ex ::exception))
(defn ex-type
  "Returns the `type` of the ex-info if possible"
  [ex]
  (type (ex-data ex)))

(defmulti ^:no-doc ex-data-spec type)
(defmethod ex-data-spec :default [_]
  (s/keys
   :opt [::type]
   ;; backward compat
   :opt-un [::type]))

(s/def ::message string?)
(s/def ::type qualified-keyword?)
(s/def ::ex-data (s/multi-spec ex-data-spec ::type))
(s/def ::exception #(instance? #?(:clj Exception
                                  :cljs js/Error) %))

(def ^:no-doc catch-sym? #{'catch})

(defn ^:no-doc catch-expr?
  [expr]
  (and (coll? expr)
       (catch-sym? (first expr))))

(defn ^:no-doc catch-data-expr?
  [expr]
  (and (catch-expr? expr)
       (qualified-keyword? (second expr))))

(defn ^:no-doc catch-exception-expr?
  [expr]
  (and (catch-expr? expr)
       (symbol? (second expr))))

(defn ^:no-doc finally-expr?
  [expr]
  (and (coll? expr)
       (= (first expr) 'finally)))

(def ^:no-doc reg-expr? (some-fn finally-expr? catch-exception-expr?))
(def ^:no-doc try-sub-clause (some-fn catch-exception-expr? catch-data-expr?))

(defn ^:no-doc data+ex
  [d ex]
  (vary-meta d assoc ::exception ex))

(s/fdef type?
  :args (s/cat :x any?
               :type ::type))
(defn type?
  "Returns true if `ex` is an ex-info with descendant/type of `type`"
  [ex t]
  (some-> ex ex-type (isa? t)))

(s/fdef catch*
  :args (s/cat :exception ::exception
               :type-key ::type
               :handler ifn?
               :continue ifn?))
(defn catch
  "catch-data as a function, takes an exception, tries to match it
  against `type-key` from its ex-data.type, on match returns call to
  `handler` with the ex-data, otherwise `continue` with the original
  exception."
  [e type-key handler continue]
  (if (type? e type-key)
    (let [d (ex-data e)]
      (handler (data+ex d e)))
    (continue e)))

(s/def ::try$catch-exception
  (s/cat :clause catch-sym?
         :class symbol?
         :binding symbol?
         :body (s/* any?)))

(s/def ::try$catch-data
  (s/cat :clause catch-sym?
         :type ::type
         :binding ::cs/binding-form
         :body (s/* any?)))

(s/def ::try$finally
  (s/cat :clause #{'finally}
         :body (s/* any?)))

(s/def ::try$body
  (s/+ #(not (and (coll? %)
                  (try-sub-clause (first %))))))

(s/fdef try+
  :args (s/cat :body ::try$body
               :clauses (s/+
                         (s/or
                          :catch-exception ::try$catch-exception
                          :catch-data ::try$catch-data
                          :finally ::try$finally))))
(defmacro try+
  "Like try but with support for ex-info/ex-data.

  If you pass a `catch-data` form it will try to match an
  ex-info :exoscale.ex/type key (or just :type), or it's potential
  ancestors in the local hierarchy.

  ex-info clauses are checked first, in the order they were specified.
  catch-data will take as arguments a type key, and a binding for the
  ex-data of the ex-info instance.

  (try
    [...]
    (catch ::something my-ex-data
      (do-something my-ex-info))
    (catch ::something-else {:as my-ex-data :keys [foo bar]}
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
  (let [[body mixed-clauses] (split-with (complement try-sub-clause) xs)
        regular-clauses (filter reg-expr? mixed-clauses)
        catch-data-clauses (filter catch-data-expr? mixed-clauses)
        type-sym (gensym "ex-type-")
        data-sym (gensym "ex-data-")
        ex-sym '&ex]
    `(try
       ~@body
       ~@(cond-> regular-clauses
           (seq catch-data-clauses)
           (conj `(catch #?(:cljs ExceptionInfo :clj clojure.lang.ExceptionInfo) ~ex-sym
                    (let [~data-sym (ex-data ~ex-sym)
                          ~type-sym (type ~data-sym)]
                      (cond
                        ~@(mapcat (fn [[_ type binding & body]]
                                    `[(isa? ~type-sym ~type)
                                      (let [~binding (data+ex ~data-sym ~ex-sym)]
                                        ~@body)])
                                  catch-data-clauses)
                        :else
                        ;; rethrow ex-info with other clauses since we
                        ;; have no match
                        (try (throw ~ex-sym)
                             ~@regular-clauses)))))))))

(def types
  #{::unavailable ::interrupted ::incorrect ::forbidden ::unsupported
    ::not-found ::conflict ::fault ::busy})

(def ^:private ex-info*
  #?(:clj clojure.core/ex-info
     :cljs cljs.core/ex-info))

(s/fdef ex-info
  :args (s/cat :msg string?
               :type+deriving (s/or :type ::type
                                    :type+deriving (s/cat :type ::type
                                                          :deriving (s/? ::deriving)))
               :data (s/? (s/nilable ::ex-data))
               :cause (s/? (s/nilable ::exception))))
(s/def ::deriving (s/coll-of ::type))
(defn ^:deprecated ex-info
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
         ;; TODO depreciate this, we shouldn't do auto-derivation here
         deriving (when coll-type? (second type))
         data' (assoc data
                      :type type' ; backward compatibility
                      ::type type')]
     (run! #(derive type' %) deriving)
     (ex-info* msg
               data'
               cause))))

;;; Sugar for common exceptions

(declare ex-unavailable
         ex-unavailable!
         ex-interrupted
         ex-interrupted!
         ex-incorrect
         ex-incorrect!
         ex-forbidden
         ex-forbidden!
         ex-unsupported
         ex-unsupported!
         ex-not-found
         ex-not-found!
         ex-conflict
         ex-conflict!
         ex-fault
         ex-fault!
         ex-busy
         ex-busy!)

(defmacro ^:no-doc gen-ex-fn-for-type
  [type]
  (let [sym (symbol (str "ex-" (name type)))
        bangsym (symbol (str "ex-" (name type) "!"))
        msg 'msg
        data 'data
        cause 'cause]
    `(do
       (s/fdef ~sym
         :args (s/cat :msg (s/and string? (complement str/blank?))
                      :data (s/? (s/nilable ::ex-data))
                      :cause (s/? (s/nilable ::exception))))
       (s/fdef ~bangsym
         :args (s/cat :msg (s/and string? (complement str/blank?))
                      :data (s/? (s/nilable ::ex-data))
                      :cause (s/? (s/nilable ::exception))))

       (defn ~sym
         ~(str "Returns an ex-info with ex-data `:type` set to `"
               type "`. Rest of the arguments match `ex-info`")
         ([~msg]
          (~sym ~msg nil nil))
         ([~msg ~data]
          (~sym ~msg ~data nil))
         ([~msg ~data ~cause]
          (ex-info ~msg ~type ~data ~cause)))

       (defn ~bangsym
         ~(str "Builds an exception with " sym " and throws it.")
         ([~msg]
          (throw (~sym ~msg nil nil)))
         ([~msg ~data]
          (throw (~sym ~msg ~data nil)))
         ([~msg ~data ~cause]
          (throw (~sym ~msg ~data ~cause)))))))

(defmacro gen-all-ex-fns!
  []
  `(do ~@(map (fn [type] `(gen-ex-fn-for-type ~type)) types)))

(gen-all-ex-fns!)

(s/fdef ex-invalid-spec
  :args (s/cat :spec qualified-keyword?
               :x any?
               :data (s/? (s/nilable ::ex-data))))
(defn ex-invalid-spec
  "Returns an ex-info when value `x` does not conform to spec `spex`"
  ([spec x]
   (ex-invalid-spec spec x nil))
  ([spec x data]
   (exoscale.ex/ex-info (str "Invalid spec: " (s/explain-str spec x))
                        [::invalid-spec [::incorrect]]
                        (assoc data :explain-data (s/explain-data spec x)))))

(s/fdef assert-spec-valid
  :args (s/cat :spec qualified-keyword?
               :x any?
               :data (s/? (s/nilable ::ex-data))))
(defn assert-spec-valid
  "Asserts that `x` conforms to `spec`, otherwise throws with
   `ex-invalid-spec`"
  ([spex x]
   (assert-spec-valid spex x nil))
  ([spec x data]
   (when-not (s/valid? spec x)
     (throw (ex-invalid-spec spec x data)))
   x))

(s/def ::ex-map
  (s/keys :req [::message
                ::type
                ::data]
          :opt [::deriving]))

(s/def ::data ::ex-data)

(extend-protocol p/Datafiable
  #?(:clj clojure.lang.ExceptionInfo :cljs ExceptionInfo)
  (datafy [x]
    (let [data (ex-data x)]
      (if-let [t (type data)]
        (let [deriving (parents t)]
          (cond-> {::type t
                   ::message (ex-message x)
                   ::data (dissoc data :type ::type)}
            (seq deriving)
            (assoc ::deriving deriving)))
        (#?(:clj Throwable->map
            :cljs r/Error->map) x)))))

(defn datafy
  "Convenience function to call datafy on a potential exception"
  [ex]
  (p/datafy ex))

(s/fdef map->ex-info
  :args (s/cat :ex-map ::ex-map
               :options (s/? (s/keys :opt [::derive?]))))
(s/def ::derive? boolean?)
(defn map->ex-info
  "Turns a datafy'ied ex/ex-info into an ex-info"
  ([m] (map->ex-info m {}))
  ([{::keys [message type deriving data cause]}
    {::keys [derive?] :or {derive? false}}]
   (ex-info message
            (cond-> [type]
              (and derive? (some? deriving))
              (conj deriving))
            data
            (when cause (map->ex-info cause)))))
