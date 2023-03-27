# sql4j

<p>
    <br> 中文 | <a href="README.md">English</a>
</p>

<p>
  <a href="LICENSE" target="_blank">
    <img alt="MIT License" src="https://img.shields.io/badge/License-MIT-green" />
  </a>

  <img alt="Maven" src="https://img.shields.io/badge/-Maven-red?style=flat-square&logo=apachemaven&logoColor=white" />

  <img alt="Java" src="https://img.shields.io/badge/-Java-blue?style=flat-square&logo=openjdk&logoColor=white" />

  <img alt="Rust" src="https://img.shields.io/badge/-Clojure-green?style=flat-square&logo=clojure&logoColor=white" />
</p>

一个轻量级持久化框架，把代码与sql进行解耦，如果您厌倦了复杂的的DSL，`Sql4j`也许是您另外一个选择.

## 为什么要造这个轮子

目前主流的持久层框架`Mybatis` `Jooq` 等都是结合一些DSL在使用，在不运行程序的情况下，单独运行sql来测试变得很困难，当然也有插件可以输出sql，但如果需要在服务器上运行就比较困难了。

## 安装

```xml
<dependency>
    <groupId>com.github.youkale</groupId>
    <artifactId>sql4j</artifactId>
    <version>${version}</version>
</dependency>
```

## 使用

#### 编写Sql, 比如src/resources/sql/order.sql

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

#### 定义 mapper

```java
public interface OrderMapper {

    List<Integer> getOrderIds(@Param("ids") List<Integer> ids);

    @Alias("queryOrderIds")
    Integer[] getOrderIdsByVec(@Param("ids") List<Integer> ids);

    @Alias("queryOrderIds")
    int[] getOrderIdsByVecPrim(@Param("ids") List<Integer> ids);
}
```

#### 调用

```java
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Sql4J sql4J = new Sql4J(dataSource, Dialect.mysql, "sql", true);
        OrderMapper mapper = sql4J.lookup(OrderMapper.class);
        List<Integer> ids = mapper.getOrderIds(List.of(1, 2, 3));
    }
}

```

## 实现逻辑

- mapper
    - 通过使用`java`的动态代理，将定义的Mapper与[hugsql](https://github.com/layerware/hugsql)的`db-fn*`
      进行绑定，默认使用Mapper的类名+方法的全限定名称，如果有指定`Alias`那么将使用其定义的名称。
    - 返回的范型类型如果不是`ParameterizedType`是不支持返回范型的，比如
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
- 入参处理（以下说明是针对在sql中取值）
    - Map类型可以根据key进行访问,java bean会根据property名称进行访问 `orderNo`
    - 基本类型和List、Set类型，需要配合`@Param`注解进行命名,方可在`sql`文件中访问.
- 返回处理
    - Map直接返回
    - List<Order>、Order[]、Order等类型，会获取对bean的PropertyDescriptor的writeMethod进行写入。如遇到数据库`column`
      与`property`不一致的情况，需要通过`@Results`注解进行配置映射关系。
        - `@Result` 是对应的每一个映射关系. 包含`column`与`property`

## 开发者

- JDK11
- Maven

某些IDE可能会出现`clojure`代码没有编译的情况，建议调试或者运行的时候先执行如下命令

```shell
mvn -P dev compile -DskipTests=true
```

## 依赖

[hugsql](https://www.hugsql.org/)

## License

[MIT](./LICENSE)