(ns cause.base-test
  (:require [cause.core :as c]
            [cause.base :as b]
            [cause.shared :as s]
            [clojure.spec.alpha :as spec]
            [clojure.test :refer [deftest is testing]]))

(deftest test-cb->edn
  (is (= '(:div {:foo "bar"} \w \a \t (:p \b \a \z))
         (b/cb->edn
          (b/transact-
           (b/new-cb)
           [[nil nil [:div {:foo "bar"} "wat"
                      [:p "baz"]]]])))))

(deftest test-transact
  (let [cb (b/new-cb)]
    (testing "new causal base")
    (is (= nil (b/cb->edn cb)))
    (let [cb (b/transact- cb [[nil nil {:a 1}]])]
      (testing "map transactions")
      (is (= {:a 1} (b/cb->edn cb)))
      (is (= {:a "hi"} (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) :a "hi"]]))))
      (is (= {:a 2 :b 3} (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) nil {:a 2 :b 3}]]))))
      (is (= {:a 1 :b {:c 2}} (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) :b {:c 2}]]))))
      (is (= {:c "hi"} (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) :a c/hide]
                                                   [(::b/root-uuid cb) nil {:b 2 :c "hi"}]
                                                   [(::b/root-uuid cb) nil {:b c/hide}]])))))
    (let [cb (b/transact- cb [[nil nil [1 2]]])]
      (testing "list transactions")
      (is (= [1 2] (b/cb->edn cb)))
      (is (= [0 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id 0]]))))
      (is (= [0 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id [0]]]))))
      (is (= [-2 -1 0 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id [-2 -1 0]]]))))
      (is (= [\h \i 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id "hi"]]))))
      (is (= [\h \i 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id ["hi"]]]))))
      (is (= [[\h \i] 1 2] (b/cb->edn (b/transact- cb [[(::b/root-uuid cb) c/root-id [["hi"]]]])))))))

(deftest test-CausalBase
  (is (= 0 (count (c/get-collection (c/new-causal-base)))))
  (is (= nil (seq (c/get-collection (c/new-causal-base)))))
  (let [cb (c/transact (c/new-causal-base) [[nil nil [1 2 3]]])]
    (is (= 3 (count (c/get-collection cb))))
    (is (= [1 2 3] (mapv peek (seq (c/get-collection cb)))))))

(deftest test-expand-reverse-path
  (let [erp (as-> (b/new-cb) cb
                  (b/transact- cb [[nil nil [1 2 3]]])
                  (b/expand-reverse-path cb (first (::b/history cb))))]
    (is (spec/valid? ::s/node (first erp)))
    (is (= 1 (peek (first erp))))
    (is (some? (::s/uuid (.-ct (second erp)))))))

(deftest test-reverse-path->path
  (let [path (as-> (b/new-cb) cb
                   (b/transact- cb [[nil nil [1 2 3]]])
                   (b/reverse-path->path cb (first (::b/history cb))))]
    (is (spec/valid? ::b/path path))))

(deftest test-tx-id-indexes
  (def cb (atom (b/new-cb)))
  (swap! cb b/transact- [[nil nil {:a 1 :b 2}]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :a 3]
                         [(::b/root-uuid @cb) :c 4]
                         [(::b/root-uuid @cb) :e 5]])
  (def last-tx-id (pop (first (peek (::b/history @cb)))))
  (is (= [2 4] (b/tx-id-indexes @cb last-tx-id)))
  (map #(is (= 2 (ffirst %))) ; these 3 reverse paths should all have a lamport-ts of 2
       (subvec (::b/history @cb) 2 (inc 4)))
  (is (= [nil nil] (b/tx-id-indexes @cb [1 "bad site-id"])))
  (is (= [nil nil] (b/tx-id-indexes @cb [1 "bad site-id"]))))

(deftest test-subhis
  (def cb (atom (b/new-cb)))
  (swap! cb b/transact- [[nil nil {:a 1 :b 2}]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :a 3]
                         [(::b/root-uuid @cb) :c 4]
                         [(::b/root-uuid @cb) :e 5]
                         [(::b/root-uuid @cb) :f 6]])
  (def last-tx-id (pop (first (peek (::b/history @cb)))))
  (is (= 4 (count (b/subhis @cb last-tx-id))))
  (is (= 4 (count (b/subhis @cb last-tx-id nil))))
  (def first-tx-id (pop (first (first (::b/history @cb)))))
  (is (= 2 (count (b/subhis @cb nil first-tx-id))))
  (is (= 6 (count (b/subhis @cb first-tx-id last-tx-id))))
  (is (= 6 (count (b/subhis @cb nil nil))))
  (is (= 0 (count (b/subhis @cb nil [0 (::s/site-id @cb)]))))
  (is (= 0 (count (b/subhis @cb [5 (::s/site-id @cb)] nil)))))

(deftest test-invert-path
  (is (= ["yVqwAa8ypPGRC_p3wdKhS" [1 "QeVBlHoQFZSx0" 0] :cause.shared/hide]
         (b/invert-path
          {::s/uuid "yVqwAa8ypPGRC_p3wdKhS"
           ::s/type ::s/map
           ::s/node [[1 "QeVBlHoQFZSx0" 0] :a 1]}))))

(deftest test-invert-
  (def cb (atom (b/new-cb)))
  (swap! cb b/transact- [[nil nil {:a 1 :b 2}]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :a 3]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :c [1 2 3]]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :c ::s/hide]])
  (is (= 3 (:a (b/get-collection- @cb))))
  (is (= 8 (count (::b/history @cb))))
  (swap! cb b/invert- (::b/history @cb))
  (is (nil? (:a (b/get-collection- @cb))))
  (is (= 13 (count (::b/history @cb)))))

(deftest test-get-next-undo-tx-id
  (def cb (atom (b/new-cb)))
  (swap! cb b/transact- [[nil nil {:a 1 :b 2}]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :a 3]])
  (is (= 2 (first (b/get-next-undo-tx-id @cb))))
  (swap! cb assoc ::b/last-undo-lamport-ts 2)
  (is (= 1 (first (b/get-next-undo-tx-id @cb))))
  (swap! cb assoc ::b/last-undo-lamport-ts 1)
  (is (nil? (b/get-next-undo-tx-id @cb)))
  (swap! cb assoc ::b/last-undo-lamport-ts nil)
  (is (= 2 (first (b/get-next-undo-tx-id @cb)))))

(deftest test-undo-
  (def cb (atom (b/new-cb)))
  (swap! cb b/transact- [[nil nil {:a 1 :b 2}]])
  (swap! cb b/transact- [[(::b/root-uuid @cb) :a 3]])
  ; (seq (b/get-collection- (b/undo- @cb)))
  (is (= 3 (:a (b/get-collection- @cb))))
  (is (= 2 (:b (b/get-collection- @cb))))
  (swap! cb b/undo-)
  (is (= 1 (:a (b/get-collection- @cb))))
  (is (= 2 (:b (b/get-collection- @cb))))
  (swap! cb b/undo-)
  (is (nil? (:a (b/get-collection- @cb))))
  (is (nil? (:b (b/get-collection- @cb)))))

(comment
  (do
    (test-cb->edn)
    (test-transact)
    (test-CausalBase)
    (test-expand-reverse-path)
    (test-reverse-path->path)
    (test-tx-id-indexes)
    (test-subhis)
    (test-invert-path)
    (test-invert-)
    (test-get-next-undo-tx-id)
    (test-undo-)))
