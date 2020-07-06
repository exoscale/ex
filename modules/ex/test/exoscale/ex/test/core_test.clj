(ns exoscale.ex.test.core-test
  (:use clojure.test)
  (:require
   [exoscale.ex :as ex]
   [exoscale.ex.test :as t]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha]
   [clojure.core.protocols :as p]))

(clojure.spec.test.alpha/instrument)

(defmacro try-val
  [& body]
  `(try
     ~@body
     (catch Exception e#
       e#)))

(deftest test-catch
  (let [d {:type ::foo}]
    (is (= d
           (ex/try+
            (throw (ex-info "asdf" d))
            (catch ::foo x
              x))))

    (is (true?
         (ex/try+
          true
          (catch ::foo x
            x))))

    ;; no match but still ex-info
    (is (= {:type ::asdf}
           (ex-data (try-val
                     (ex/try+
                      (throw (ex-info "asdf" {:type ::asdf}))
                      (catch ::foo x
                        x))))))

    (is (instance? Exception
                   (try-val
                    (ex/try+
                     (throw (Exception. "boom"))
                     (catch ::foo x
                       x)))))))

(deftest test-catch
  (let [d {:type ::foo}]
    (is (= d
           (ex/catch (ex-info "asdf" d) ::foo
             identity
             (fn [] ::err))))

    ;; no match
    (is (= {:type ::asdf}
           (ex-data (ex/catch (ex-info "asdf" {:type ::asdf}) ::foo
                      (constantly false)
                      identity))))
    (is (instance? Exception
                   (ex/catch (Exception. "boom") ::foo #(throw %)
                             identity)))))

(deftest test-inheritance
  (ex/derive ::bar ::foo)
  (let [d {:type ::bar}]
    (is (ex/try+
         (throw (ex-info "" d))
         (catch ::foo ex
           (= ex d)))))

  (ex/derive ::baz ::bar)
  (let [e {:type ::baz}]
    (is (ex/try+
         (throw (ex-info "" e))
         (catch ::foo ex
           (= ex e)))))

  (let [e {:type ::bak}]
    (is (try-val (ex/try+
                  (throw (ex-info "" e))
                  (catch ::foo ex
                    (= e ex)))))))

(deftest test-bindings
  (is (ex/try+
       (throw (ex-info "" {:type ::foo
                           :bar 1}))
       (catch ::foo {:keys [bar]}
         (= bar 1)))))

(deftest special-binding
  (ex/try+
   (throw (ex-info "" {:type ::foo
                       :bar 1}))
   (catch ::foo {:as e}
     (let [e &ex]
       (ex/try+
        (throw (ex-info "" {:type ::bar}))
        (catch ::bar b
          (is (not= &ex e))
          (is (instance? Exception &ex))
          (is (instance? Exception e))))))))

(deftest test-complex-meta
  (let [x (ex-info "" {:type ::ex-with-meta})]
    (is (ex/try+
         (throw x)
         (catch ::ex-with-meta
                x'
           (-> x' meta ::ex/exception (= x)))))))

(deftest test-spec
  (s/def ::foo string?)
  (ex/set-ex-data-spec! ::a1 (s/keys :req [::foo]))
  (is (false? (s/valid? ::ex/ex-data {:type ::a1})))
  (is (true? (s/valid? ::ex/ex-data {:type ::a1 ::foo "bar"}))))

(deftest test-within-eval
  (is (= 1 (eval `(do (ex/try+
                       (throw (ex-info "boom" {:type ::bar}))
                       (catch ::bar e# 1)))))))

(deftest test-thrown-ex-data
  (is (thrown-ex-info-type? ::foo (throw (ex/ex-info "bar" ::foo)))))

(deftest test-type
  (is (ex/type? (ex/ex-info "bar" ::foo)
                ::foo))

  (is (not (ex/type? (ex/ex-info "bar" ::foo22)
                     ::bar)))

  (is (ex/type? (ex/ex-info "bar" [::fx [::xx]])
                ::xx))

  (is (not (ex/type? (ex/ex-info "bar" [::fx [::xx]])
                     ::xxy))))

(deftest datafy
  (let  [x (ex/ex-info "boom"
                       [::datafy [::ex/foo ::ex/bar]]
                       {:a 1}
                       (ex/ex-incorrect "the-cause"))]
    (is (= (p/datafy x)
           #:exoscale.ex{:type ::datafy
                         :message "boom"
                         :data {:a 1}
                         :deriving #{:exoscale.ex/foo :exoscale.ex/bar}
                         :cause #:exoscale.ex{:type ::ex/incorrect
                                              :message "the-cause"
                                              :data {}}})
        "test datafy in")
    (is (= (p/datafy x) (p/datafy (ex/map->ex-info (p/datafy x) {::ex/derive? true})))
        "test roundtrip")
    (is (= (p/datafy x) (p/datafy (ex/map->ex-info (dissoc (p/datafy x) ::ex/deriving))))
        "test roundtrip without derivation"))

  (let  [x (ex/ex-incorrect "boom")]
    (is (= (p/datafy x)
           #:exoscale.ex{:type :exoscale.ex/incorrect
                         :message "boom"
                         :data {}})
        "test datafy in")
    (is (= (p/datafy x) (p/datafy (ex/map->ex-info (dissoc (p/datafy x) ::ex/deriving))))
        "test roundtrip without derivation"))

  (let  [x (clojure.core/ex-info "boom" {})]
    (is (false? (s/valid? (p/datafy x) ::ex/ex-map))
        "regular ex info is passthrough")))
