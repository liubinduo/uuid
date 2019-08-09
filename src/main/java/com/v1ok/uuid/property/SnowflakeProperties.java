package com.v1ok.uuid.property;

import static com.v1ok.uuid.snowflake.GenerateImpl.TIME_WHEN_EPOCH;
import static com.v1ok.uuid.snowflake.GenerateImpl.TO_STRING_BASE;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uuid.snowflake")
public class SnowflakeProperties {

  private long workerId = 1;
  private long dataCenterId = 1;
  private long epoch = TIME_WHEN_EPOCH;
  private int base = TO_STRING_BASE;
  private UUIDType uuidType = UUIDType.TO_STRING;


  public long getWorkerId() {
    return workerId;
  }

  public void setWorkerId(long workerId) {
    this.workerId = workerId;
  }

  public long getDataCenterId() {
    return dataCenterId;
  }

  public void setDataCenterId(long dataCenterId) {
    this.dataCenterId = dataCenterId;
  }

  public long getEpoch() {
    return epoch;
  }

  public void setEpoch(long epoch) {
    this.epoch = epoch;
  }

  public int getBase() {
    return base;
  }

  public void setBase(int base) {
    this.base = base;
  }

  public UUIDType getUuidType() {
    return uuidType;
  }

  public void setUuidType(UUIDType uuidType) {
    this.uuidType = uuidType;
  }
}
