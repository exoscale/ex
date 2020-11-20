(ns exoscale.ex.instrumentation
  "Simple instrumentation with low overhead.

  If *enabled* is true (it is by default* it will look in defn
  metadata for :ret and/or :args and emit a function that will do
  args/ret checking.

  Upon failure an effect will be run"
  (:require [clojure.spec.alpha :as s]
            clojure.core.specs.alpha
            [exoscale.ex :as ex])
  (:refer-clojure :exclude [defn]))

(def default-options
  #:exoscale.ex.instrumentation
  {:incorrect-args!
   (fn [fn-name spec args]
     (throw (ex/ex-info "Function arguments are invalid"
                        [::invalid-fn-args [::incorrect]]
                        #:exoscale.ex.instrumentation.failure{:type :args
                                                              :fn-name fn-name
                                                              :spec spec
                                                              :args args})))

   :incorrect-ret!
   (fn [fn-name spec args ret]
     (throw (ex/ex-info "Function return value is invalid"
                        [::invalid-fn-ret [::incorrect]]
                        #:exoscale.ex.instrumentation.failure{:type :ret
                                                              :fn-name fn-name
                                                              :spec spec
                                                              :args args
                                                              :ret ret})))})

(defonce ^{:dynamic true}
  *enabled*
  (not= "false" (System/getProperty "exoscale.ex.instrumentation.enabled")))

(defn- assert-args-valid
  [fn-name spec args opts]
  (when-not (s/valid? spec args)
    ((:exoscale.ex.instrumentation/incorrect-args! opts)
     fn-name spec args)))

(defn- assert-ret-valid
  [fn-name spec args ret opts]
  (when-not (s/valid? spec ret)
    ((:exoscale.ex.instrumentation/incorrect-ret! opts)
     fn-name spec args ret)))

(defmacro defn
  "Same as defn, looks for :ret :args in metadata to trigger arg/ret
  checking"
  [& defn-args]
  (let [{:keys [fn-name docstring meta]}
        (s/conform :clojure.core.specs.alpha/defn-args
                   defn-args)
        {:keys [args ret]} meta
        instrumented-fn-sym (symbol (format "-instrumented-%s" (str fn-name)))
        fn-sym (symbol (str *ns*) (str fn-name))]
    (if (and *enabled* (or args ret))
      `(do
         ;; instead of messing with a registry just emit a private fn
         ;; for the original
         (defn- ~instrumented-fn-sym
           ~@(rest defn-args))
         ~(let [fn-args (gensym "fn-args")
                fn-ret (gensym "fn-ret")
                args-spec-sym (gensym "args-spec")
                ret-spec-sym (gensym "ret-spec")
                assert-opts (merge default-options meta)]
            `(let [~args-spec-sym ~args
                   ~ret-spec-sym ~ret]
               ;; generate the wrapper
               (clojure.core/defn ~@(remove nil? [fn-name docstring meta])
                 [& ~fn-args]
                 ;; only emit args check if meta args is present
                 ~(when args
                    `(assert-args-valid '~fn-sym
                                        ~args-spec-sym
                                        ~fn-args
                                        ~assert-opts))
                 ;; TODO could use .applyTo with clj, or even dispatch
                 ;; on invoke with right # of args
                 (let [~fn-ret (apply ~instrumented-fn-sym ~fn-args)]
                   ;; only emit ret check if meta ret present
                   ~(if ret
                      `(assert-ret-valid '~fn-sym
                                         ~ret-spec-sym
                                         ~fn-args
                                         ~fn-ret
                                         ~assert-opts)
                      fn-ret)))
               ;; set proper arglist for tooling/docs
               #?(:clj (alter-meta! #'~fn-name
                                    assoc :arglists
                                    (-> #'~instrumented-fn-sym meta :arglists)))
               nil)))
      ;; emit regular fn if not ret/args spec is found
      `(clojure.core/defn ~@defn-args))))

(comment (defn foo
           "It does something"
           {:args (s/cat :a string?)
            :ret number?}
           [^String a]
           1))

(foo "as")
