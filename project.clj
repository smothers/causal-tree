(defproject causal-tree "0.0.1"
            :description "An EDN-like CRDT (Causal Tree) for Clojure(Script) that automatically tracks history and resolves conflicts."
            :url "https://github.com/smothers/causal-tree"

            :license {:name "MIT License"
                      :url "https://opensource.org/licenses/MIT"}

            :dependencies [[org.clojure/clojure       "1.9.0" :scope "provided"]
                           [org.clojure/clojurescript "1.10.238" :scope "provided"]
                           [org.clojure/core.async "0.4.474"]
                           [nano-id                   "0.9.3"]]

            :plugins [[lein-doo       "0.1.10"]
                      [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
                      [nrepl/lein-nrepl "0.2.0"]
                      [lein-figwheel "0.5.17"]]

            :profiles
            {:dev
             {:main user
              :source-paths ["dev"]
              :dependencies [[org.clojure/test.check "0.10.0-alpha3"]
                             [pjstadig/humane-test-output "0.8.3"]
                             [walmartlabs/datascope "0.1.1"]
                             [com.taoensso/tufte "2.0.1"]
                             [criterium "0.4.4"]
                             [com.clojure-goes-fast/clj-memory-meter "0.1.2"]
                             [nrepl/nrepl "0.4.5"]
                             [proto-repl "0.3.1"]
                             [figwheel-sidecar "0.5.16"]
                             [com.bhauman/rebel-readline "0.1.4"]
                             [cider/piggieback "0.3.10"]]
              :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

            :doo {:alias {:browsers [:chrome :firefox]}}

            :aliases {"deploy"    ["do" "clean," "deploy" "clojars"]
                      "test"      ["do" ["clean"] ["test"]]
                      "cljs-test" ["do" ["doo" "browsers" "browser-test" "once"]]
                      "cljs-test-watch" ["do" ["doo" "browsers" "browser-test" "auto"]]}

            :clean-targets ^{:protect false} ["target"]

            :cljsbuild
            {:builds
             [{:id "browser-test"
               :source-paths ["src" "test"]
               :compiler {:main          causal-tree.runner
                          :output-to     "target/browser-tests.js"
                          :output-dir    "target"
                          :optimizations :advanced
                          :parallel-build true}}
              {:id "dev"
               :figwheel {:open-urls ["http://localhost:3449"]}
               :source-paths ["src"]
               :compiler {:main causal-tree.core
                          :asset-path "resources/public"
                          :output-to "target/figwheel/main.js"
                          :output-dir "target/figwheel/out"
                          :verbose false
                          :optimizations :none
                          :cache-analysis true
                          :source-map true}}]})
