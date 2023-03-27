package com.github.youkale.sql4j.service;

import com.github.youkale.sql4j.Dialect;
import com.github.youkale.sql4j.Order;
import com.github.youkale.sql4j.OrderMapper;
import com.github.youkale.sql4j.Sql4J;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OrderService {

    private HikariDataSource dataSource;

    @BeforeEach
    public void initDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;IGNORECASE=TRUE;SCHEMA=PUBLIC;MODE=mysql");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("sa");
        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    public void dest() {
        if (null != dataSource) {
            dataSource.close();
        }
    }

    private Sql4J getSql4J() {
        return new Sql4J(dataSource, Dialect.mysql, "sql", true);
    }

    @Test
    void testCreateTable() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        lookup.dropTable();
    }

    @Test
    void testSaveOrder() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        Order order = new Order();
        order.setId(1);
        order.setOrderNo("001");
        int i = lookup.save(order);
        Assertions.assertEquals(1, i);
        lookup.dropTable();
    }

    @Test
    void testBatchInsert() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        int i = lookup.batchInsert(List.of(List.of(1, "001"), List.of(2, "002"), List.of(3, "003"), List.of(4, "004")));
        Assertions.assertEquals(4, i);
        lookup.dropTable();
    }

    @Test
    void testQueryOrders() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        int i = lookup.batchInsert(List.of(List.of(1, "001"), List.of(2, "002"), List.of(3, "003"), List.of(4, "004")));
        Assertions.assertEquals(4, i);
        List<Order> orders = lookup.getOrders(List.of(1, 2, 3, 4));

        Assertions.assertEquals(1, orders.get(0).getId());
        Assertions.assertEquals("001", orders.get(0).getOrderNo());

        Assertions.assertEquals(2, orders.get(1).getId());
        Assertions.assertEquals("002", orders.get(1).getOrderNo());

        Assertions.assertEquals(3, orders.get(2).getId());
        Assertions.assertEquals("003", orders.get(2).getOrderNo());

        Order[] orderVec = lookup.getOrderVec(List.of(1, 2));

        Assertions.assertEquals(1, orderVec[0].getId());
        Assertions.assertEquals("001", orderVec[0].getOrderNo());

        Assertions.assertEquals(2, orderVec[1].getId());
        Assertions.assertEquals("002", orderVec[1].getOrderNo());

        Order order = lookup.getOrder(1);
        Assertions.assertEquals(1, order.getId());
        Assertions.assertEquals("001", order.getOrderNo());

        List<Integer> orderIds = lookup.getOrderIds(List.of(1, 2, 3, 4));
        Assertions.assertEquals(1, orderIds.get(0));
        Assertions.assertEquals(2, orderIds.get(1));
        Assertions.assertEquals(3, orderIds.get(2));
        Assertions.assertEquals(4, orderIds.get(3));

        Integer[] orderIdsByVec = lookup.getOrderIdsByVec(List.of(1, 2, 3, 4));

        Assertions.assertEquals(1, orderIdsByVec[0]);
        Assertions.assertEquals(2, orderIdsByVec[1]);
        Assertions.assertEquals(3, orderIdsByVec[2]);
        Assertions.assertEquals(4, orderIdsByVec[3]);

        int[] orderIdsByVecPrim = lookup.getOrderIdsByVecPrim(List.of(1, 2, 3, 4));

        Assertions.assertEquals(1, orderIdsByVecPrim[0]);
        Assertions.assertEquals(2, orderIdsByVecPrim[1]);
        Assertions.assertEquals(3, orderIdsByVecPrim[2]);
        Assertions.assertEquals(4, orderIdsByVecPrim[3]);


        List<Order> orderList = lookup.getOrderList(List.of(1, 2, 3, 4));
        Assertions.assertEquals(1, orderList.get(0).getId());
        Assertions.assertEquals("001", orderList.get(0).getOrderNo());

        Assertions.assertEquals(2, orderList.get(1).getId());
        Assertions.assertEquals("002", orderList.get(1).getOrderNo());

        Assertions.assertEquals(3, orderList.get(2).getId());
        Assertions.assertEquals("003", orderList.get(2).getOrderNo());

        lookup.dropTable();
    }

}
