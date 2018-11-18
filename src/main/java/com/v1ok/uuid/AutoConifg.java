package com.v1ok.uuid;

import com.v1ok.uuid.property.SnowflakeProperties;
import com.v1ok.uuid.property.TypeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan()
@EnableConfigurationProperties({SnowflakeProperties.class, TypeProperties.class})
public class AutoConifg {

  @Autowired
  SnowflakeProperties snowflakeProperties;

  @Autowired
  TypeProperties typeProperties;


  @Bean
  public IDGenerate generate() {
    IDGenerate generate = null;
    switch (typeProperties.getType()) {
      case UUID:
        generate = new com.v1ok.uuid.java.GenerateImpl();
        break;
      case SNOWFLAKE:
        long workerId = snowflakeProperties.getWorkerId();
        long dataCenterId = snowflakeProperties.getDataCenterId();
        long epoch = snowflakeProperties.getEpoch();
        generate = new com.v1ok.uuid.snowflake.GenerateImpl(workerId, dataCenterId, epoch);
        break;
    }

    return generate;
  }
}
