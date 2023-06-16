package com.github.youkale.sql4j;


import com.github.youkale.sql4j.annotation.Alias;
import com.github.youkale.sql4j.annotation.Param;
import com.github.youkale.sql4j.annotation.Result;
import com.github.youkale.sql4j.annotation.Results;

import java.util.List;

public interface AccountMapper {

    @Alias("accountTable")
    void createTable();

    /**
     *
     * @param entity
     * @return
     */
    @Alias("createAccount")
    int createAccount(Account entity);


//    @Results(value = {@Result(property = "id", column = "id"),
//            @Result(property = "userName", column = "user_name"),
//            @Result(property = "userPassword", column = "user_password"),
//            @Result(property = "salt", column = "salt"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at")})
    @Alias("get")
    Account get(@Param("userName") String userName);
}
