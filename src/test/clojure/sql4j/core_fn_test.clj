(ns sql4j.core-fn-test
  (:require [clojure.test :refer :all]
            [sql4j.core :refer :all])
  (:import (java.time LocalDate LocalDateTime)
           (java.util Date)))

(defn- diff-obj [a b]
  (loop [ai a
         bi b
         res true]
    (if (and ai bi (true? res))
      (let [aii (first ai)
            bii (first bi)]
        (recur (next ai) (next bi) (= aii bii)))
      res)))

(defn- local-date-time-diff [^LocalDateTime a ^LocalDateTime b]
  (diff-obj [(.getYear a) (.getMonth a) (.getDayOfMonth a)]
            [(.getYear b) (.getMonth b) (.getDayOfMonth b)]))

(defn- local-date-diff [^LocalDate a ^LocalDate b]
  (diff-obj [(.getYear a) (.getMonth a) (.getDayOfMonth a)]
            [(.getYear b) (.getMonth b) (.getDayOfMonth b)]))

(defn- date-diff [^Date a ^Date b]
  (let [d (Math/abs (- (.getTime a) (.getTime b)))]
    (< d (* 24 3600 1000))))

(def type-cast-cases [
                      [LocalDateTime (Date.) (LocalDateTime/now) local-date-time-diff]
                      [LocalDateTime (LocalDate/now) (LocalDateTime/now) local-date-time-diff]
                      [LocalDateTime (LocalDateTime/now) (LocalDateTime/now) local-date-time-diff]

                      [LocalDate (Date.) (LocalDate/now) local-date-diff]
                      [LocalDate (LocalDate/now) (LocalDate/now) local-date-diff]
                      [LocalDate (LocalDateTime/now) (LocalDate/now) local-date-diff]

                      [Date (LocalDate/now) (Date.) date-diff]
                      [Date (LocalDateTime/now) (Date.) date-diff]

                      [Integer (long 255) 255 true]
                      [Long (int 255) 255 true]
                      [Float (int 22.5) (float 22.0) true]
                      [Double (int 242.5) (double 242.0) true]
                      [Double (int 242.5) (double 242.5) false]
                      [Boolean (int 1) true true]
                      [Boolean 0 false true]
                      ])

(deftest type-cast-test
  (testing "type cast testing ..."
    (loop [cases (seq type-cast-cases)]
      (when cases
        (let [[t act ex res] (first cases)
              f (type-cast-fn t)
              op (if res = not=)
              evr (f act)]
          (is (if (fn? res)
                (res evr ex)
               (op evr ex)))
          (recur (next cases)))))))
