# uuid
这是一个生成UUID的工具,实现了两种标准的UUID。
uuid and SNOWFLAKE

#jar用法
IDGenerate idg = IDGenerateBuilder.builder(GenerateType.SNOWFLAKE).build();

String id = idg.nextIdToString()  or long id = idg.nextIdtoLong();

#String Boot 用法
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