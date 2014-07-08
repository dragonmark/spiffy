(ns spiffy.core-test
  (:require 
   [spiffy.core :as sc]
   #+cljs [cemerick.cljs.test :as t]
   #+clj [clojure.test :as t
          :refer (is deftest with-test run-tests testing)]
   )
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)])
  )

(deftest a-test
  (testing "FIXME, I fail."
    #+cljs (is (= 2 1))
    (is (= 2 1))))
