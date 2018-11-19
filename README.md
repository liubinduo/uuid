# uuid
这是一个生成UUID的工具,实现了两种标准的UUID。
uuid and SNOWFLAKE

# jar用法

```java
IDGenerate idg = IDGenerateBuilder.builder(GenerateType.SNOWFLAKE)
                                  .setWorkerId(workerId)
                                  .setDataCenterId(dataCenterId)
                                  .setEpoch(epoch)
                                  .setBase(base)
                                  .build();

String id = idg.nextIdToString()  or long id = idg.nextIdtoLong();
```

# Spring Boot 用法
在 pom.xml 引用
```xml
<dependency>
   <groupId>com.v1ok</groupId>
   <artifactId>uuid</artifactId>
   <version>${version}</version>
</dependency>
```

在application.properties文件中加入配置
uuid.type=snowflake 或者 uuid.type=uuid

```java
@RestController
public class Demo {

  @Autowired
  IDGenerate generate;

  @GetMapping
  public Object test(){
    try {
      return generate.nextIdToLong();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return null;
  }
}
```
# Config
```properties
uuid.type                   // UUID类型   取值范围：snowflake, uuid
uuid.snowflake.workerId     //机器ID       取值范围：(0~31)
uuid.snowflake.dataCenterId //数据中心ID   取值范围：(0~31)
uuid.snowflake.epoch        //纪元时间戳 
uuid.snowflake.base         //toString 时的进制数，取值范围：2～36
```