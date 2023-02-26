(ns sql4j.core-test
  (:require [clojure.test :refer :all]
            [hikari-cp.core :refer [make-datasource]])
  (:import (com.github.youkale.sql4j Dialect Sql4J)
           (com.github.youkale.sql4j.annotation Param)
           (java.util List Map)))

(definterface OrderMapper
  (^int createTable [])
  (^void dropTable [])
  (^int batchInsert [^{:tag java.util.List Param "order"} orderList])
  (^int insertWithMap [^java.util.Map m])
  (^int updateById [^java.util.Map m])
  (^int deleteById [^java.util.Map m]))

(def datasource-options {:adapter "h2"
                         :url     "jdbc:h2:file:./target/sql4j;IGNORECASE=TRUE;SCHEMA=PUBLIC;MODE=mysql"
                         :username "sa"})

(def sql4j (Sql4J. (make-datasource datasource-options) Dialect/mysql "sql" true))

(def order-mapper ^OrderMapper (.lookup sql4j OrderMapper))

(deftest sql4j-create-table-test
  (testing "testing sql4j create table ..."
    (is (= (.createTable order-mapper) 0))))

(deftest sql4j-insert-batch-test
  (testing "insert with map testing ..."
    (is (= (.batchInsert order-mapper (List/of (List/of 1 "001") (List/of 2 "002") (List/of 3 "003"))) 3))))

(deftest sql4j-insert-with-map-test
  (testing "insert with map testing ..."
    (is (= (.insertWithMap order-mapper (Map/of "id" 4 "orderNo" "oNo")) 1))))

(deftest sql4j-update-by-id-test
  (testing "update by map testing ..."
    (is (> (.updateById order-mapper (Map/of "id" 4 "orderNo" "11111")) 0))))

(deftest sql4j-delete-by-id-test
  (testing "delete by map testing ..."
    (is (> (.deleteById order-mapper (Map/of "id" 4 "orderNo" "11111")) 1))))

(deftest sql4j-drop-table-test
  (testing "drop table testing ..."
    (is (= (first (.dropTable order-mapper)) 0))))