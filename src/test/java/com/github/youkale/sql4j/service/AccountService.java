package com.github.youkale.sql4j.service;

import com.github.youkale.sql4j.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class AccountService {

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
        return new Sql4J(dataSource, Dialect.mysql, true);
    }

    @Test
    void testCreateTable() {
        Sql4J sql4J = getSql4J();
        AccountMapper mapper = sql4J.lookup(AccountMapper.class);
        mapper.createTable();
    }

    @Test
    void testAccountWriteRead() {
        Sql4J sql4J = getSql4J();
        AccountMapper mapper = sql4J.lookup(AccountMapper.class);
        mapper.createTable();

        String salt = "test-salt";
        String user = "admin";
        String password = "nimda";

        Date created = new Date();
        Date updated = new Date();
        int account = mapper.createAccount(new Account() {{
            setSalt(salt);
            setUserName(user);
            setUserPassword(password);
            setCreatedAt(created);
            setUpdatedAt(updated);
        }});
        Assertions.assertEquals(1,account);
        Account admin = mapper.get("admin");
        Assertions.assertEquals(salt,admin.getSalt());
        Assertions.assertEquals(user,admin.getUserName());
        Assertions.assertEquals(password,admin.getUserPassword());
        Assertions.assertEquals(created,admin.getCreatedAt());
        Assertions.assertEquals(updated,admin.getUpdatedAt());

    }


}
