(ns causal.collections.shared-test
  (:require [causal.collections.shared :as s]
            [clojure.test :refer [deftest is]]
            [clojure.spec.test.alpha :as stest]))

(def ret-key
  #? (:clj :clojure.spec.test.check/ret)
  #? (:cljs :clojure.test.check/ret))

(deftest spec-fdefs-pass
  (is (get-in (first (stest/check `s/new-node)) [ret-key :pass?])))
  ; TODO: switch to this once enumerate-namespace is added to cljs.spec.test.alpha
  ;   It's currently in master awaiting the next release: https://github.com/clojure/clojurescript/blame/master/src/main/cljs/cljs/spec/test/alpha.cljc#L25
  ; (->> (stest/enumerate-namespace 'causal.shared)
  ;      (stest/check)
  ;      (map #(is (get-in % [ret-key :pass?])))))