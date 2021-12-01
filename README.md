# clj-wiremock
[![Build Status](https://travis-ci.org/kelveden/clj-wiremock.svg?branch=master)](https://travis-ci.org/kelveden/clj-wiremock)

Clojure bindings for [WireMock](http://wiremock.org/)

## Usage
![](https://clojars.org/kelveden/clj-wiremock/latest-version.svg)

### As a fixture
Typically you'll want wiremock to fire up at the beginning of your
test namespaces and shutdown at the end. To achieve this, `wiremock-fixture`
is provided as your "once" test fixture.

```clj
(defn around-all
  [f]
  (wmk/wiremock-fixture [{:port wiremock-port}] f))

(use-fixtures :once around-all)

(deftest can-ping
  (wmk/with-stubs
      [{:req [:GET "/ping"] :res [200 {:body "pong"}]}]
  
      (let [{:keys [status]} (http/get (server/url (wmk/only-server) "/ping"))]
        (is (= 200 status)))))
```

Note that there is a subtle variation on this called `wiremock-fixture-fn` which
wraps `wiremock-fixture` in another anonymous, parameterless function. This is useful
if you want to thread through multiple fixtures in your "around all" function; e.g.

```clj
(defn around-all
  [f]
  (-> f
  	  (wmk/wiremock-fixture-fn [{:port wiremock-port}])
  	  (some-other-fixture)))

(use-fixtures :once around-all)
```

See the [full code](test/clj_wiremock/test/examples/as_fixture.clj). 

### As a block
Sometimes you only need wiremock in one or two tests rather than the entire
suite. For cases like this `with-wiremock` is provided to create 
code blocks in which wiremock has been fired up. Wiremock will be stopped
at the end of the block.

```clj
(wmk/with-wiremock [{:port wiremock-port}]
    (wmk/with-stubs
      [{:req [:GET "/ping"] :res [200 {:body "pong"}]}]

      (let [{:keys [status]} (http/get (server/url (wmk/only-server) "/ping"))]
        (is (= 200 status)))))
```

See the [full code](test/clj_wiremock/test/examples/as_block.clj). 


### Manually
If you find yourself wanting to manipulate wiremock from your REPL
it might be best to manipulate the wiremock server directly. Below is a
rather contrived test to illustrate this.

Note that because we're calling `server/stub!` directly the stubs will not get
cleared down automatically - i.e. if you're not
using `with-stubs` then you must handle clearing down stubs manually. (Of course,
in the example below all this is a moot point as the server is being shut down in the `finally`
block anyway.)

```clj
(deftest can-ping
  (let [wiremock (server/init-wiremock {:port wiremock-port})]
    (try
      (server/start! wiremock)
      (server/register-stub! wiremock {:req [:GET "/ping"] :res [200 {:body "pong"}]})

      (let [{:keys [status]} (http/get (server/url wiremock "/ping"))]
        (is (= 200 status)))

      (finally (server/stop! wiremock)))))
```

See the [full code](test/clj_wiremock/test/examples/manually.clj).

### Via Java interop
Sometimes the helper functions provided aren't enough and you need to get your hands
dirty with the underlying Java WireMockServer.

```clj
(deftest can-ping
  (let [wiremock (server/init-wiremock {:port wiremock-port})
        wmk-java (.wmk-java wiremock)]
    (try
      (.start wmk-java)

      (http/post (str "http://localhost:" (.port wmk-java) "/__admin/mappings/new")
                 {:body (json/generate-string {:request  {:method :GET :url "/ping"}
                                               :response {:status 200 :body "pong"}})})

      (let [{:keys [status]} (http/get (str "http://localhost:" (.port wmk-java) "/ping"))]
        (is (= 200 status)))

      (finally (.stop wmk-java)))))
```

See the [full code](test/clj_wiremock/test/examples/with_java_interop.clj).

## Syntactic sugar

### Scenarios
As usual, you can just add the usual scenario/state fields from the Wiremock JSON API to
your stub. Alternatively, you can use the syntactic sugar provided by two fields in your stub:

* `scenario`: The name of the scenario (i.e. simply a synonym for the `scenarioName` field in
the JSON API).
* `state`: A map that has two optional subfields `required` and `new` corresponding to the
`requiredScenarioState` and `newScenarioState` fields on the JSON API respectively. If no
`required` field is specified, "Started" is implied.

Note that if a `state` field is included in a stub but no `scenario`, then a default scenario
name (`__default__`) will be used instead. This is to allow creation of stubs in simpler situations
where only one scenario is required. 

e.g.

```clj
; Specify a required state (in the default scenario) and new state for when the stub is matched
(wmk/with-stubs [{:req [:GET "/ping"] :res [200] :state {:required "Started" :new "pinged"}}] ...)

; Specify a required state but no state change when the stub is matched
(wmk/with-stubs [{:req [:GET "/ping"] :res [200] :state {:required "Started"}}] ...)

; Specify a new state for when the stub is matched and imply "Started" as the required state 
(wmk/with-stubs [{:req [:GET "/ping"] :res [200] :state {:new "pinged"}}] ...)

; Specify a scenario name, required state and new state
(wmk/with-stubs [{:req [:GET "/ping"] :res [200] :scenario "myscenario" :state {:required "Started" :new "pinged"}}] ...)
```

## Configuration

You can configure WireMock with the same options available to the Java library (as of version 2.27.2).

### Network ports and binding

```clj
:port 8000
:https-port 8001
:dynamic-port? true
:dynamic-https-port? true
:bind-address "192.168.1.111"
```

### Jetty Configuration

```clj
:container-threads 5
:jetty-acceptors 4
:jetty-accept-queue-size 100
:jetty-header-buffer-size 16834
:asynchronous-response-enabled? true
:asynchronous-response-threads 10
```

### HTTPS Configuration

```clj
:keystore-path "/path/to/https-certs-keystore.jks"
:keystore-password "verysecret!"
:keystore-type "BKS"
:key-manager-password "donttell"
:need-client-auth? true
:trust-store-path "/path/to/trust-store.jks"
:trust-store-password "trustme"
```

### Proxy Settings

```clj
:enable-browser-proxying? true
:preserve-host-header? false
:proxy-host-header "my.otherdomain.com"
:proxy-via ["my.corporate.proxy", 8080]
:ca-keystore-path "/path/to/ca-key-store.jks"
:ca-keystore-password "trustme"
:ca-keystore-type "JKS"
```

### File Locations

```clj
:using-files-under-directory "/path/to/files-and-mappings-root"
:using-files-under-classpath "root/path/under/classpath"
```

### Request Journal

```clj
:disable-request-journal? true
:max-requests-journal-entries 100
```

### Notification

```clj
:log-to-console? true ;syntactic sugar for .notifier(new ConsoleNotifier(true))
:notifier (reify Notifier
                   (info [_ _])
                   (error [_ _])
                   (error [_ _ _])
```

### Gzip

```clj
:gzip-disabled? true
```

### Extensions
Supports passing instances of Extension only at this point.

```clj
:extensions [(ResponseTemplateTransformer. true)]
```

### Transfer Encoding

Accepts `:always`, `:body-file`, or `:never`.

```clj
:use-chunked-transfer-encoding :body-file
```

### Cross-origin response headers (CORS)

```clj
:stub-cors-enabled? true
```

You can find more information about each configuration on the [WireMock Configuration](http://wiremock.org/docs/configuration) page.

## License
[Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.