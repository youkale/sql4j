# Sql4j

---
<p>
    <br> English | <a href="README-CN.md">中文</a>
</p>
<p >
  <a href="LICENSE" target="_blank">
    <img alt="Apache License" src="https://img.shields.io/badge/License-Apache 2-green" />
  </a>

  <img alt="Maven" src="https://img.shields.io/badge/-Maven-red?style=flat-square&logo=apachemaven&logoColor=white" />

  <img alt="Java" src="https://img.shields.io/badge/-Java-blue?style=flat-square&logo=openjdk&logoColor=white" />

  <img alt="Rust" src="https://img.shields.io/badge/-Clojure-green?style=flat-square&logo=clojure&logoColor=white" />
</p>

A lightweight persistence framework that decouples code from sql, if you are tired of complex DSL, `Sql4j` may be
another option for you.

## Why build this project

The current mainstream persistence layer framework `Mybatis` `Jooq` and so on are combined with some DSL in use , in the
case of not running the program , running sql alone to test becomes very difficult , of course there are plug-ins can
output sql , but if you need to run on the server is more difficult . So I built this project

## Installation

### Add dependencies in Maven

```xml
<dependency>
    <groupId>io.github.youkale</groupId>
    <artifactId>sql4j</artifactId>
    <version>0.1.5</version>
</dependency>
```

## Usage

#### Write Sql, e.g. src/resources/sql/order.sql

```sql
-- :name com.github.youkale.sql4j.OrderMapper#getOrderIds :? :*
select id
from `order`
where id in (:v * :ids)

-- :name queryOrderIds :? :*
select id
from `order`
where id in (:v * :ids)
```

#### Define mapper

``` java
public interface OrderMapper {
    
    List<Integer> getOrderIds(@Param("ids") List<Integer> ids).
    
    @Alias("queryOrderIds")
    Integer[] getOrderIdsByVec(@Param("ids") List<Integer> ids).

    @Alias("queryOrderIds")
    int[] getOrderIdsByVecPrim(@Param("ids") List<Integer> ids).
}
```

#### Calls

``` java
import java.util.List.

public class Main {
    public static void main(String[] args) {
        Sql4J sql4J = new Sql4J(dataSource, Dialect.mysql, "sql", true).
        OrderMapper mapper = sql4J.lookup(OrderMapper.class).
        List<Integer> ids = mapper.getOrderIds(List.of(1, 2, 3)).
    }
}

```
## [HugSql](https://www.hugsql.org/) Usage

### Declarations
- `:name` indicates the name of the mapped mapper
- `:command` The command to be executed, divided into `:execute` and `:query`, which means execute and query, can be written in one line with `:name`
- `:result` result type, you can write a line with `:name`
    - The corresponding `:command` for `:raw` can be `:execute` and `:query`.
    - `:many`, `:one` The corresponding `:command` is `:query`.
    - `:affected` corresponds to the `:command` as `:execute`.
- `:doc` for document

#### abbreviation
- The `command` is `:execute`.
    - `:execute`->`:!`.
    - `:query`-> `:?` .
- `:result'.
  - `:many` -> `:*`.
  - `:one` -> `:1`.
  - `:affected`-> `:n`.

#### Example
- Multiple rows
  ```sql
  -- :name create-characters-table
  -- :command :execute
  -- :result :raw
  -- :doc create-characters-table
  -- Auto-increment and current timestamp are
  -- for H2 databases (adjust to your database).
    ```
- Single row (query multiple rows)
    ```sql
    -- :name queryOrders :? :*
    ```
- Single row (single row query)
    ```sql
    -- :name queryOrders :? :!
    ```

### Placeholders
- `:value` or `:v`, Value type parameter, here `:id` is `{"id": 123}`. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-value-parameters)
  ```sql
  --:name value-param :? :*
  select * from characters where id = :id

  --:name value-param-with-param-type :? :*
  select * from characters where id = :v:id
    ```
- `:value*` or `:v*` , Value list parameter, where `names` is `List<String >`. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-value-list-parameters)
  ```sql
  --:name-value-list-param :? :*
  select * from characters where name in (:v*:names)
    ```
- `:tuple` or `t`, here `id-name` is `[1 , "youkale"]`. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-tuple-parameters) 
    ```sql
    -- :name tuple-param
    -- :doc Tuple Param
    select * from test
    where (id, name) = :tuple:id-name
    ```
- `:tuple*` or `:t*`, Tuple list parameter, here `people` is `{"person": [[1, "Ed"], [2 "Al"], [3 "Bo"]]}``. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-tuple-parameters)
  ```sql
  -- :name tuple-param-list
  -- :doc Tuple Param List
  insert into test (id, name)
  value :t*:person
  ```
- `:identifier` or `i` , the quoted parameter, is replaced with the quoted field at runtime, for example mysql will be replaced with \`table\`. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-identifier-parameters)
    ```sql
    --:name identifier-param :? :*
    select * from :i:table-name
    ```

- `:identifier*` or `i*`,  The quoted list parameter, which is replaced with the quoted field at runtime. For example, mysql replaces it with \`columns\`. [Doc](https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-identifier-list-parameters)
  ```sql
  --:name identifier-list-param :? :*
  select :i*:column-names, count(*) as population
  From the example
  group by :i*:column-names
  order by :i*:column-names
  ```


## Implement

- mapper
    - Bind the defined mapper to the `db-fn*` of [hugsql](https://github.com/layerware/hugsql) by using the dynamic
      proxy of `java`, by default using the class name of the mapper + the fully qualified name of the method,
      if `@Alias` is specified then it will use its defined if `@Alias` is specified.
    - The returned paradigm type is not supported if it is not a `ParameterizedType`, for example
      ```java
      interface Foo<T> {
          T foo();
      }
      
      class Bar {
        void call(){
          Foo f = sql4j.lookup(Foo.class);
        }  
      }
      ```
- Calls parameter mapping (the following instructions are for taking values in sql)
    - Map type can be accessed according to key, java bean will be accessed according to property name ``orderNo``
    - Basic types and List and Set types need to be named with the `@Param` annotation to be accessed in the `sql` file.
- Return parameter mapping
    - Map returns directly
    - List<Order>, Order[], Order, etc., will get the `writeMethod` of the `PropertyDescriptor` of the bean to write. In
      case of inconsistency between database `column` and `property`, you need to configure the mapping relationship
      through the `@Results` annotation.
        - `@Result` is the corresponding mapping relation for each. Contains `column` and `property`.

## Developer

- JDK11
- Maven

Some IDEs may not compile the `clojure` code, so it is recommended that you run the following command when debugging or
running

```shell
mvn -P dev compile -DskipTests=true
```

## Dependencies

[hugsql](https://www.hugsql.org/)

### Other
Translated with www.DeepL.com/Translator (free version)

## License

[Apache License V2.0](./LICENSE)