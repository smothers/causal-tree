(ns test.map
  (:require
   [causal-tree.util :as u]
   [causal-tree.shared :as s]
   [causal-tree.core :as c]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string]
   [clojure.test :refer [deftest is]]
   [com.walmartlabs.datascope :as ds]))

(comment
  (pprint
   (c/assoc
    (c/new-causal-tree :map)
    :foo "bar")))

(comment
  (def ct (atom (c/new-causal-tree :map)))
  (pprint (swap! ct c/assoc :foo "bar"))
  (pprint (swap! ct c/assoc :baz "boo"))
  (pprint (swap! ct c/assoc :baz "bing!"))
  (def ct2 (atom (c/new-causal-tree :list)))
  (swap! ct2 c/conj "a")
  (swap! ct2 c/conj "b")
  (swap! ct2 c/conj "c")
  ; (swap! ct2 c/conj @ct)
  (pprint (swap! ct c/assoc :fizz ct2))
  (pprint (swap! ct c/dissoc :foo))
  (pprint (deref ct))
  (c/ct->edn @ct2)
  (swap! ct2 c/conj "d")
  (swap! (c/get @ct :fizz) c/conj "e")
  (apply str (:fizz (c/ct->edn @ct)))
  (ds/view @ct)
  (c/ct->edn @ct2))
