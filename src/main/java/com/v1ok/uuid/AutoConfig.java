package com.v1ok.uuid;

import com.v1ok.uuid.property.SnowflakeProperties;
import com.v1ok.uuid.property.TypeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableConfigurationProperties({SnowflakeProperties.class, TypeProperties.class})
public class AutoConfig {

  @Autowired
  SnowflakeProperties snowflakeProperties;

  @Autowired
  TypeProperties typeProperties;


  @Bean
  @ConditionalOnProperty(prefix = "uuid", name = "type")
  public IDGenerate generate() {

    long workerId = snowflakeProperties.getWorkerId();
    long dataCenterId = snowflakeProperties.getDataCenterId();
    long epoch = snowflakeProperties.getEpoch();

    return IDGenerateBuilder.builder(typeProperties.getType())
        .setWorkerId(workerId)
        .setDataCenterId(dataCenterId)
        .setEpoch(epoch)
        .build();
  }
}
