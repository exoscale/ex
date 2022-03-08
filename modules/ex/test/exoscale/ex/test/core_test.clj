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
  (let [d {:exoscale.ex/type ::foo}]
    (is (= d
           (ex/try+
            (throw (ex-info "asdf" d))
            (catch ::foo x
              x))))

    (is (= d
           (ex/try+
            (throw (ex-info "asdf" {:type ::foo}))
            (catch ::foo x
              x)))
        "ensure legacy format works")

    (is (true?
         (ex/try+
          true
          (catch ::foo x
            x))))

    ;; no match but still ex-info
    (is (= {:exoscale.ex/type ::asdf}
           (ex-data (try-val
                     (ex/try+
                      (throw (ex-info "asdf" {:exoscale.ex/type ::asdf}))
                      (catch ::foo x
                        x))))))

    (is (instance? Exception
                   (try-val
                    (ex/try+
                     (throw (Exception. "boom"))
                     (catch ::foo x
                       x)))))))

(deftest test-catch
  (let [d {:exoscale.ex/type ::foo}]
    (is (= d
           (ex/catch (ex-info "asdf" d) ::foo
             identity
             (fn [] ::err))))

    ;; no match
    (is (= {:exoscale.ex/type ::asdf}
           (ex-data (ex/catch (ex-info "asdf" {:exoscale.ex/type ::asdf}) ::foo
                      (constantly false)
                      identity))))
    (is (instance? Exception
                   (ex/catch (Exception. "boom") ::foo #(throw %)
                             identity)))))

(deftest test-inheritance
  (ex/derive ::bar ::foo)
  (let [d {:exoscale.ex/type ::bar}]
    (is (ex/try+
         (throw (ex-info "" d))
         (catch ::foo ex
           (= ex d)))))

  (ex/derive ::baz ::bar)
  (let [e {:exoscale.ex/type ::baz}]
    (is (ex/try+
         (throw (ex-info "" e))
         (catch ::foo ex
           (= ex e)))))

  (let [e {:exoscale.ex/type ::bak}]
    (is (try-val (ex/try+
                  (throw (ex-info "" e))
                  (catch ::foo ex
                    (= e ex)))))))

(deftest test-bindings
  (is (ex/try+
       (throw (ex-info "" {:exoscale.ex/type ::foo
                           :bar 1}))
       (catch ::foo {:keys [bar]}
         (= bar 1)))))

(deftest special-binding
  (ex/try+
   (throw (ex-info "" {:exoscale.ex/type ::foo
                       :bar 1}))
   (catch ::foo {:as e}
     (let [e &ex]
       (ex/try+
        (throw (ex-info "" {:exoscale.ex/type ::bar}))
        (catch ::bar b
          (is (not= &ex e))
          (is (instance? Exception &ex))
          (is (instance? Exception e))))))))

(deftest test-complex-meta
  (let [x (ex-info "" {:exoscale.ex/type ::ex-with-meta})]
    (is (ex/try+
         (throw x)
         (catch ::ex-with-meta
                x'
           (-> x' meta ::ex/exception (= x)))))))

(deftest test-within-eval
  (is (= 1 (eval `(do (ex/try+
                       (throw (ex-info "boom" {:exoscale.ex/type ::bar}))
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
                       (ex/ex-incorrect "the-cause" {:key "value"}))]
    (is (= (p/datafy x)
           (ex/datafy x)
           #:exoscale.ex{:exoscale.ex/type ::datafy
                         :message "boom"
                         :data {:a 1}
                         :deriving #{:exoscale.ex/foo :exoscale.ex/bar}})
        "test datafy in")
    (is (true? (s/valid? ::ex/ex-map (p/datafy x)))
        "datafy are ::ex/ex-map")
    (is (true? (s/valid? ::ex/ex-map (p/datafy (ex/map->ex-info (ex/datafy x)))))
        "roundtrip datafy are ::ex/ex-map")
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
    (is (false? (s/valid? ::ex/ex-map (p/datafy x)))
        "regular ex info is passthrough")))
