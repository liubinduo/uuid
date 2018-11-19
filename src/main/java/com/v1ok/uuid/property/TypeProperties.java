package com.v1ok.uuid.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uuid")
public class TypeProperties {

  private GenerateType type;

  public GenerateType getType() {
    return type;
  }

  public void setType(GenerateType type) {
    this.type = type;
  }
}
