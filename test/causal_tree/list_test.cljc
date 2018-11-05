(ns causal-tree.list-test
  (:require
   [causal-tree.shared :as s]
   [causal-tree.core :as c]
   [causal-tree.list :as c-list]
   [clojure.string :as string]
   [clojure.test :refer [deftest is]]
   #? (:clj [criterium.core :refer [quick-bench]])))

(def simple-values
  (concat [c/delete c/delete c/delete \ , \ , \ , \ , \newline] (map char (take 26 (iterate inc 97)))))

; (def site-ids [0 1 2])
(def site-ids [(s/site-id) (s/site-id) (s/site-id) (s/site-id) (s/site-id)])

(defn rand-node
  ([causal-list] (rand-node causal-list (rand-nth site-ids)))
  ([causal-list site-id] (rand-node causal-list site-id
                                    (rand-nth simple-values)))
                                    ; (char (+ (rand 52) 65))
                                    ; (gen/generate (spec/gen ::s/value))
  ([causal-list site-id value]
   (let [causal-tree (.-ct causal-list)
         cause (rand-nth (keys (::s/nodes causal-tree)))
         lamport-ts (inc (max
                          (first cause)
                          (or (ffirst (last (get-in causal-tree
                                                    [::s/yarns site-id])))
                              0)))]
     (c/node lamport-ts site-id cause value))))

(defn insert-rand-node
  ([causal-list] (c/insert causal-list (rand-node causal-list))))

(defn idempotent? [causal-list]
  (let [causal-tree (.-ct causal-list)
        refreshed-ct (s/refresh-caches c-list/weave causal-tree)]
    (is (= (::s/site-id causal-tree) (::s/site-id refreshed-ct)))
    (is (= (::s/lamport-ts causal-tree) (::s/lamport-ts refreshed-ct)))
    (is (= (::s/nodes causal-tree) (::s/nodes refreshed-ct)))
    (is (= (::s/yarns causal-tree) (::s/yarns refreshed-ct)))
    (is (= (::s/weave causal-tree) (::s/weave refreshed-ct)))))
    ; (is (= causal-tree refreshed-ct))))

(deftest known-idempotent-insert-edge-cases
  (let [nodes [[[1 "xT_odlTBwTRNU"] [0 "0"] ::s/delete]
               [[2 "9FyYzf9pum6E4"] [1 "xT_odlTBwTRNU"] \d]
               [[3 "9FyYzf9pum6E4"] [0 "0"] \r]
               [[4 "NwudSBdQg3Ru2"] [3 "9FyYzf9pum6E4"] \space]
               [[4 "9FyYzf9pum6E4"] [0 "0"] \d]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "xT_odlTBwTRNU"] [0 "0"] \space]
               [[2 "xT_odlTBwTRNU"] [0 "0"] \b]
               [[2 "NwudSBdQg3Ru2"] [1 "xT_odlTBwTRNU"] \q]
               [[2 "9FyYzf9pum6E4"] [1 "xT_odlTBwTRNU"] \space]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "Pz8iuNCXvVsYN"] [0 "0"] \o]
               [[2 "Pz8iuNCXvVsYN"] [1 "Pz8iuNCXvVsYN"] ::s/delete]
               [[3 "9FyYzf9pum6E4"] [2 "Pz8iuNCXvVsYN"] \u]
               [[2 "NwudSBdQg3Ru2"] [1 "Pz8iuNCXvVsYN"] \space]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "W7XhooU1Hsw7E"] [0 "0"] \j]
               [[1 "VdIJLRISw~zgo"] [0 "0"] \w]
               [[1 "A~iIXinAXkGX7"] [0 "0"] ::s/delete]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "W7XhooU1Hsw7E"] [0 "0"] \u]
               [[2 "W7XhooU1Hsw7E"] [1 "W7XhooU1Hsw7E"] \space]
               [[2 "7hLbMKLvcll_4"] [1 "W7XhooU1Hsw7E"] ::s/delete]
               [[1 "VdIJLRISw~zgo"] [0 "0"] \m]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "Ftbpo0oG7ZnpR"] [0 "0"] ::s/delete]
               [[1 "A~iIXinAXkGX7"] [0 "0"] ::s/delete]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 "VdIJLRISw~zgo"] [0 "0"] ::s/delete]
               [[2 "A~iIXinAXkGX7"] [1 "VdIJLRISw~zgo"] "j"]
               [[3 "A~iIXinAXkGX7"] [0 "0"] "i"]
               [[1 "W7XhooU1Hsw7E"] [0 "0"] "s"]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 " f "] [0 "0"] ::s/delete]
               [[2 " z "] [1 " f "] " "]
               [[2 " f "] [0 "0"] "l"]
               [[2 " a "] [1 " f "] "v"]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl))
  (let [nodes [[[1 " f "] [0 "0"] ::s/delete]
               [[2 " f "] [0 "0"] ::s/delete]
               [[3 " a "] [2 " f "] "c"]
               [[2 " z "] [1 " f "] "r"]]
        cl (reduce c/insert (c/causal-list) nodes)]
    (idempotent? cl)))

(defn find-weave-inconsistencies
  ([] (find-weave-inconsistencies 9))
  ([max-steps]
   (loop [cl (c/causal-list)
          insertions (c/get-weave cl)
          step 0]
     (if (>= step max-steps)
       nil
       (if (is (= (c/get-weave cl) (::s/weave (c-list/weave (.-ct cl)))))
         (let [node (rand-node cl)]
           (recur (c/insert cl node) (conj insertions node) (inc step)))
         {:insertions insertions
          :step step
          :initial (c/causal->edn cl)
          :reweave (c/causal->edn (c-list/weave cl))})))))

(deftest try-to-find-new-idempotent-edge-cases
  (is (empty? (keep (fn [_] (find-weave-inconsistencies 9))
                    (range 99)))))

(def prose (string/split "Hereupon Legrand arose, with a grave and stately air, and brought me the beetle
from a glass case in which it was enclosed. It was a beautiful scarabaeus, and, at
that time, unknown to naturalists—of course a great prize in a scientific point
of view. There were two round black spots near one extremity of the back, and a
long one near the other. The scales were exceedingly hard and glossy, with all the
appearance of burnished gold. The weight of the insect was very remarkable, and,
taking all things into consideration, I could hardly blame Jupiter for his opinion
respecting it." #" "))

(defn rand-phrase []
  (let [t (+ 2 (rand-int 6))
        d (- (rand-int (count prose)) t)]
    (string/join " " (take t (drop d prose)))))

(defn rand-weave-of-phrases
  ([] (rand-weave-of-phrases 3))
  ([n-phrases]
   (let [starting-phrases (map #(str " <" % "> ") (repeatedly n-phrases rand-phrase))]
     (loop [cl (c/causal-list)
            insertions []
            phrase (first starting-phrases)
            phrases (rest starting-phrases)
            site-id (s/site-id)]
       (if (not-empty phrase)
         (let [cause (last (get-in (.-ct cl) [::s/yarns site-id]))
               node  (c/node (inc (or (ffirst cause) 1)) site-id
                             (or (first cause) s/root-id) (first phrase))]
               ; (rand-node cl site-id (first phrase))]
           (recur (c/insert cl node)
                  (conj insertions node)
                  (if (not-empty (rest phrase)) (rest phrase) (first phrases))
                  (if (not-empty (rest phrase)) phrases (rest phrases))
                  (if (not-empty (rest phrase)) site-id (s/site-id))))
         {:cl cl
          :insertions insertions
          :phrases starting-phrases
          :materialized-weave (apply str (c/causal->edn cl))
          :materialized-reweave (apply str (c/causal->edn (c-list/weave (.-ct cl))))})))))

(deftest concurrent-runs-stick-together
  (let [result (rand-weave-of-phrases 5)]
    (doall (map #(is (string/includes? (:materialized-weave result) %))
                (:phrases result)))))

(comment
  (do
    (known-idempotent-insert-edge-cases)
    (try-to-find-new-idempotent-edge-cases)
    (concurrent-runs-stick-together))

  (time
   (keep (fn [_] (find-weave-inconsistencies 9))
         (range 999)))

  (rand-phrase)
  (dissoc (rand-weave-of-phrases 3) :cl :insertions)

  (def cl (atom (c/causal-list)))
  (time (do (doall (repeatedly 50 #(swap! cl insert-rand-node))) nil))
  (quick-bench (do (insert-rand-node @cl) nil))
  (quick-bench (do (c/causal->edn @cl) nil)))
