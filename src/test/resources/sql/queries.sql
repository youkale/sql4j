-- :name sql4j.core_test.OrderMapper#createTable :!
-- :command :execute
-- :result :affected
-- :doc Create order table
CREATE TABLE `order`
(
    `id`       INT         NOT NULL,
    `order_no` VARCHAR(50) NOT NULL
);


-- :name accountTable :!
-- :command :execute
-- :result :affected
-- :doc Create System account
CREATE TABLE `t_s_account`
(
    `id`            int                           NOT NULL AUTO_INCREMENT,
    `user_name`     varchar(100)  NOT NULL,
    `user_password` varchar(32)            DEFAULT NULL,
    `salt`          varchar(32)      NOT NULL,
    `created_at`    datetime                         ,
    `updated_at`    datetime                                  ,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB  COMMENT='管理后台账号表'


-- :name createAccount :! :n
insert into `t_s_account`(`user_name`,`user_password`,`salt`, `created_at`, `updated_at`)
values (:userName, :userPassword, :salt,:createdAt, :updatedAt);

-- :name get :? :1
select *
from `t_s_account`
where `user_name` = :userName;


-- :name sql4j.core_test.OrderMapper#dropTable :! :n
drop table `order`

-- :name sql4j.core_test.OrderMapper#batchInsert :! :n
    insert into `order` (`id`,`order_no`)
    values :t*: order

-- :name sql4j.core_test.OrderMapper#insertWithMap :! :n
    insert into `order` (`id`,`order_no`)
    values (:id, :orderNo)

-- :name sql4j.core_test.OrderMapper#updateById :! :n
select *
from `order`
where id = :v * :ids


-- :name sql4j.core_test.OrderMapper#updateById :! :n
update `order`
set `order_no` = :orderNo
where id = :id

-- :name sql4j.core_test.OrderMapper#deleteById :! :n
delete
from `order`
where id = :id


-- java unit test

-- :name com.github.youkale.sql4j.OrderMapper#createTable :!
-- :command :execute
-- :result :affected
-- :doc Create order table
CREATE TABLE `order`
(
    `id`       INT         NOT NULL,
    `order_no` VARCHAR(50) NOT NULL
);

-- :name drop :! :n
drop table `order`

-- :name com.github.youkale.sql4j.OrderMapper#batchInsert :! :n
    insert into `order` (`id`,`order_no`)
    values :t*:orders

-- :name saveOrder :! :n
    insert into `order` (`id`,`order_no`)
    values (:id, :orderNo)

-- :name queryOrders :? :*
select id as code, order_no as order_code
from `order`
where id in (:v * :ids)


-- :name getOrderList :? :*
select id, order_no as order_code
from `order`
where id in (:v * :ids)

-- :name queryOrder :? :1
select id as code, order_no as order_code
from `order`
where id = :id

-- :name queryOrderIds :? :*
select id
from `order`
where id in (:v * :ids)


-- :name com.github.youkale.sql4j.OrderMapper#updateById :! :n
update `order`
set `order_no` = :orderNo
where id = :id

-- :name com.github.youkale.sql4j.OrderMapper#deleteById :! :n
delete
from `order`
where id = :id