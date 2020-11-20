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
  #exoscale.ex.instrumentation
  {:args-invalid!
   (fn [fn-name spec args]
     (throw (ex/ex-info "Function arguments invalid"
                        [::invalid-fn-args [::incorrect]]
                        {::fn-name fn-name
                         ::spec spec
                         ::args args})))
   :ret-invalid! (fn [fn-name spec args ret]
                   (throw (ex/ex-info "Function return value invalid"
                                      [::invalid-fn-ret [::incorrect]]
                                      {::fn-name fn-name
                                       ::spec spec
                                       ::args args
                                       ::ret ret})))})

(defonce ^{:dynamic true}
  *enabled*
  (not= "false" (System/getProperty "exoscale.ex.instrumentation.enabled")))

(defn- assert-args-valid
  [fn-name spec args effect!]
  (when-not (s/valid? spec args)
    (throw (ex/ex-info "Function arguments invalid"
                       [::invalid-fn-args [::incorrect]]
                       {::fn-name fn-name
                        ::spec spec
                        ::args args}))))

(defn- assert-ret-valid
  [fn-name spec args ret]
  (when-not (s/valid? spec args)
    (throw (ex/ex-info "Function return value invalid"
                       [::invalid-fn-ret [::incorrect]]
                       {::fn-name fn-name
                        ::spec spec
                        ::args args
                        ::ret ret}))))

(defmacro defn
  [& defn-args]
  (let [{:keys [fn-name docstring meta]}
        (s/conform :clojure.core.specs.alpha/defn-args
                   defn-args)
        {:keys [args ret]} meta
        instrumented-fn-sym (symbol (format "-instrumented-%s" (str fn-name)))
        fn-sym (symbol (str *ns*) (str fn-name))]
    (if (and *enabled* (or args ret))
      `(do
         (defn- ~instrumented-fn-sym
           ~@(rest defn-args))
         ~(let [fn-args (gensym "fn-args")
                fn-ret (gensym "fn-ret")
                args-spec-sym (gensym "args-spec")
                ret-spec-sym (gensym "ret-spec")]
            `(let [~args-spec-sym ~args
                   ~ret-spec-sym ~ret]
               (clojure.core/defn ~@(remove nil? [fn-name docstring meta])
                 [& ~fn-args]
                 ~(when args
                    `(assert-args-valid '~fn-sym
                                        ~args-spec-sym
                                        ~fn-args))
                 ;; could use .applyTo with clj, or even dispatch on invoke with right # of args
                 (let [~fn-ret (apply ~instrumented-fn-sym ~fn-args)]
                   ~(if ret
                      `(assert-ret-valid '~fn-sym
                                         ~ret-spec-sym
                                         ~fn-args
                                         ~fn-ret)
                      fn-ret))))))
      ;; emit regular fn if not ret/args spec is found
      `(clojure.core/defn ~@defn-args))))

(defn foo
  "asdf"
  ;; {;; :args (s/cat :a string?)
  ;;  ;; :ret number?
  ;;  }
  [^String a] a)

(foo "")
;; (clojure.pprint/pprint (macroexpand '(defn+ foo
;;                                        "asdf"
;;                                        {:args (s/cat :str string?)
;;                                         :ret any?}
;;                                        [^String a] a)))

;; (foo "a")
;; ^:args (s/cat)
;; ^:ret (s/cat)
#_(macroexpand '(defn+
                  foo
                  "docstring"
                  ([^String a] a)
                  ([a b] (+ a b))))

(foo "a")
