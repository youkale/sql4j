package com.github.youkale.sql4j;


import com.github.youkale.sql4j.annotation.Alias;
import com.github.youkale.sql4j.annotation.Param;
import com.github.youkale.sql4j.annotation.Result;
import com.github.youkale.sql4j.annotation.Results;

import java.util.List;

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


    @Alias("getOrderList")
    @Results(value = {
            @Result(property = "orderNo", column = "order_code")})
    List<Order> getOrderList(@Param("ids") List<Integer> ids);

    @Alias("queryOrders")
    @Results(value = {
            @Result(property = "id", column = "code"),
            @Result(property = "orderNo", column = "order_code")})
    Order[] getOrderVec(@Param("ids") List<Integer> ids);


    @Alias("queryOrder")
    @Results(value = {
            @Result(property = "id", column = "code"),
            @Result(property = "orderNo", column = "order_code")})
    Order getOrder(@Param("id") Integer id);

    @Alias("queryOrderIds")
    List<Integer> getOrderIds(@Param("ids") List<Integer> ids);


    @Alias("queryOrderIds")
    Integer[] getOrderIdsByVec(@Param("ids") List<Integer> ids);

    @Alias("queryOrderIds")
    int[] getOrderIdsByVecPrim(@Param("ids") List<Integer> ids);
}
