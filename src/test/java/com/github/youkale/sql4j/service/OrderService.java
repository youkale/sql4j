package com.github.youkale.sql4j.service;

import com.github.youkale.sql4j.Dialect;
import com.github.youkale.sql4j.Order;
import com.github.youkale.sql4j.OrderMapper;
import com.github.youkale.sql4j.Sql4J;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

public class OrderService {


    public DataSource initDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl( "jdbc:h2:mem:testdb;IGNORECASE=TRUE;SCHEMA=PUBLIC;MODE=mysql" );
        config.setDriverClassName("org.h2.Driver");
        config.setUsername( "sa" );
        config.setPassword( "sa" );
        return new HikariDataSource(config);
    }

    private Sql4J getSql4J() {
        return new Sql4J(initDataSource(), Dialect.mysql,"sql", true);
    }

    @Test
    void testCreateTable() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        lookup.dropTable();
    }

    @Test
    void testSaveOrder(){
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        Order order = new Order();
        order.setId(1);
        order.setOrderNo("001");
        int i = lookup.save(order);
        Assertions.assertEquals(1,i);
        lookup.dropTable();
    }

    @Test
    void testBatchInsert() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        int i = lookup.batchInsert(List.of(List.of(1, "001"), List.of(2, "002"), List.of(3, "003"), List.of(4, "004")));
        Assertions.assertEquals(4,i);
        lookup.dropTable();
    }

    @Test
    void testQueryOrders() {
        Sql4J sql4J = getSql4J();
        OrderMapper lookup = sql4J.lookup(OrderMapper.class);
        lookup.createTable();
        int i = lookup.batchInsert(List.of(List.of(1, "001"), List.of(2, "002"), List.of(3, "003"), List.of(4, "004")));
        Assertions.assertEquals(4,i);
        List<Order> orders = lookup.getOrders(List.of(1, 2, 3, 4));

        lookup.dropTable();
    }

}
