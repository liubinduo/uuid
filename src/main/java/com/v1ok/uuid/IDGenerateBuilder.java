package com.v1ok.uuid;

import static com.v1ok.uuid.snowflake.GenerateImpl.TIME_WHEN_EPOCH;
import static com.v1ok.uuid.snowflake.GenerateImpl.TO_STRING_BASE;

import com.v1ok.uuid.property.GenerateType;

public final class IDGenerateBuilder {

  private GenerateType type;

  private long workerId = 1;
  private long dataCenterId = 1;
  private long epoch = TIME_WHEN_EPOCH;
  private int base = TO_STRING_BASE;

  private IDGenerateBuilder(GenerateType type) {
    this.type = type;
  }

  public static IDGenerateBuilder builder(GenerateType type) {
    return new IDGenerateBuilder(type);
  }


  public IDGenerate build() {
    IDGenerate generate = null;
    switch (type) {
      case UUID:
        generate = new com.v1ok.uuid.java.GenerateImpl();
        break;
      case SNOWFLAKE:
        generate =
            new com.v1ok.uuid.snowflake.GenerateImpl(workerId, dataCenterId, epoch, base);
        break;
    }
    return generate;
  }


  public IDGenerateBuilder setType(GenerateType type) {
    this.type = type;
    return this;
  }


  public IDGenerateBuilder setWorkerId(long workerId) {
    this.workerId = workerId;
    return this;
  }


  public IDGenerateBuilder setDataCenterId(long dataCenterId) {
    this.dataCenterId = dataCenterId;
    return this;
  }


  public IDGenerateBuilder setEpoch(long epoch) {
    this.epoch = epoch;
    return this;
  }

  public IDGenerateBuilder setBase(int base) {
    this.base = base;
    return this;
  }
}
