package com.github.youkale.sql4j;


import com.github.youkale.sql4j.annotation.Alias;
import com.github.youkale.sql4j.annotation.Param;
import com.github.youkale.sql4j.annotation.Result;
import com.github.youkale.sql4j.annotation.Results;

import java.util.List;
import java.util.Optional;

public interface OrderMapper {

    void createTable();

    @Alias("drop")
    void dropTable();

    int batchInsert(@Param("orders") List<List<Object>> orders);

    @Alias("saveOrder")
    int save(Order order);

    @Alias("queryOrders")
    @Results(value = {
            @Result(property = "id", column = "code"),
            @Result(property = "orderNo", column = "order_code")})
    List<Order> getOrders(@Param("ids") List<Integer> ids);
}
