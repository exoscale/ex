# ex

Exception handling library for clojure(+manifold)

## Rationale

* We want to be able to express/handle exceptions via `ex-info` in a
more formalised way.

* We want to be able to support java like exceptions hierarchies,
without having to define/extend classes.

* We want the shape of our custom `ex-info`s to be consistent.

* We want to have a default/generic categorization of `ex-info`s

* We don't want to differ from the original `try`/`catch`/`finally` semantics.

* We want to have the same mechanism for `manifold.deferred/catch`.

* We don't want to minimize any performance penalty it might have.

* We don't want to emit `catch Throwable` (it's bad), stay as close as
  possible to what the user would write by hand.

## How

This is an exception library, drop in replacement for
`try`/`catch`/`finally`, that adds support for `ex-info`/`ex-data`
with a custom (clojure) hierarchy that allows to express exceptions
relations. It also comes with manifold support.

So we have `exoscale.ex/try+`, which supports vanilla `catch`/`finally`
clauses.

If you specify a `catch-data` clause with a keyword as first argument
things get interesting. **We assume you always put a `:type` key in
the ex-infos you generate**, and will match its value to the value of
the key in the `catch-data` clause.

### The basics

Essentially `catch-data` takes this form:

```clj
(catch-data :something m
   ;; where m is a binding to the ex-data (you can destructure at that level as well)
   )
```

So you can do things like that.

```clj

(require '[exoscale.ex :as ex])

(ex/try+

  (throw (ex-info "Argh" {:type ::bar :foo "a foo"}))

  (catch-data ::foo data
    (prn :got-ex-data data))

  (catch-data ::bar {:as data :keys [foo]}
    ;; in that case it would hit this one
    (prn :got-ex-data-again foo))

  (catch ExceptionInfo e
   ;; this would match an ex-info that didn't get a hit with catch-ex-info)

  (catch Exception e (prn :boring))

  (finally (prn :boring-too)))

```

### Exception hierarchies

We leverage a clojure hierarchy so you can essentially create
exceptions relations/extending without having to mess with Java
classes directly and in a clojuresque" way.

```clj
;; so bar is a foo

(ex/derive ::bar ::foo)

(ex/try+
  (throw (ex-info "I am a bar" {:type ::bar})
  (catch-data ::foo d
    (prn "got a foo with data" d)
    (prn "Original exception instance is " (-> d meta ::ex/exception))))

```

### Manifold support

We have `exoscale.ex.manifold/catch-data` that matches the semantics
of a `catch-data` block in `try+` but with a more manifold like feel.

```clj
(require '[exoscale.ex.manifold :as mx])
(require '[manifold.deferred :as d])

(-> (d/error-deferred (ex-info "boom" {:type :bar}))
    (mx/catch-data :bar (fn [data] (prn "ex-data is: " data)))
    (d/catch (fn [ex] "... regular manifold handling here")))
```

### Specing your ex-infos data

You can specify a clojure.spec for the ex-data via the multimethod at
`:exoscale.ex/ex-data-spec` or via the sugar fn provided
`exoscale.ex/set-ex-data-spec!`:

```clj
(ex/set-ex-data-spec! ::foo (s/keys :req [...] :opt [...]}))]}))`
```

By default this is enforced via clojure.spec/assert, meaning unless
you toggled it "on" explicitely, it will be off.

You can change this behavior and set the validator to something else
like a log statement:

```clj
(ex/set-validator!
  (fn [data]
    (when-not (s/valid? ::ex/ex-data)
      (log/warnf "ex-data caught doesn't match the spec for it's :type : %s" data))))
```

It is strongly discouraged to make this validation breaking the flow
of execution in production as this would only show up at "catch time".

### How to get to the original exception

You can also get the full exception instance via the metadata on the
ex-data we extract, it's under the `:exoscale.ex/exception` key.

###  Our default internal `:type`s table

We suggest you also either use one of these as `:type` or derive your
own with these.

Within the namespace `:exoscale.ex`:

| category | retry | fix
| ---- | ---- | --- |
| :unavailable | yes | make sure callee healthy |
| :interrupted | yes | stop interrupting |
| :incorrect | no | fix caller bug |
| :forbidden | no | fix caller creds |
| :unsupported | no | fix caller verb |
| :not-found | no | fix caller noun |
| :conflict | no | coordinate with callee |
| :fault | no | fix callee bug |
| :busy | yes | backoff and retry |

This is very much inspired by
[cognitect-labs/anomalies](https://github.com/cognitect-labs/anomalies).

TODO: add more, refine

## How to generate/use good ex-infos

* Specify a `:type` key **always**

* The type key should either be one of our base type or a descendant

* If it's a rethrow or comes from another exception pass the original
  exception as `cause` (3rd arg of `ex-info`)

* `ex-info`s should contain enough info but not too much (don't dump a
  system/env map on its data)

* If you use more than once the same `:type` you might want to spec it

* Avoid returning raw values in error-deferreds, return properly
  formated ex-infos
  `(d/error-deferred ::foo) vs (d/error-deferred (ex-info ".." {...}))`

* Have logging in mind when you create them. It's easier to pull
  predefined set of values from ELK or aggregate than searching on a
  string message.

* Do not leak data that is meant to emutate a usage context
  (cloudstack error codes, http status codes). That should be handled
  by a middleware at the edges.

* TODO

## Usages examples

Some real life examples of usage for this:

* Deny all display of user exceptions to the end-user by default via
  top level middleware and only mark the ones safe to show as
  ::user-exposable in a declarative way.

* skip sentry logging for some kind of exceptions (or the inverse)
