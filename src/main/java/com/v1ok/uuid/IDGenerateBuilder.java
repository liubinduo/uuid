package com.v1ok.uuid;

import static com.v1ok.uuid.snowflake.GenerateImpl.TIME_WHEN_EPOCH;
import static com.v1ok.uuid.snowflake.GenerateImpl.TO_STRING_BASE;

import com.v1ok.uuid.property.GenerateType;
import com.v1ok.uuid.property.UUIDType;
import com.v1ok.uuid.snowflake.GenerateImpl;

public final class IDGenerateBuilder {

  private GenerateType type;

  private long workerId = 1;
  private long dataCenterId = 1;
  private long epoch = TIME_WHEN_EPOCH;
  private int base = TO_STRING_BASE;
  private UUIDType uuidType;

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
        GenerateImpl generate1 = new GenerateImpl(workerId, dataCenterId, epoch, base);
        generate1.setUuidType(this.uuidType);
        generate = generate1;
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

  public IDGenerateBuilder setUUIDType(UUIDType uuidType) {
    this.uuidType = uuidType;
    return this;
  }
}
