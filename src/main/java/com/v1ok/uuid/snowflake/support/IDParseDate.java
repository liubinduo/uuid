package com.v1ok.uuid.snowflake.support;

import com.v1ok.uuid.snowflake.GenerateImpl;
import java.util.Date;

public class IDParseDate {

  private GenerateImpl generate;

  public IDParseDate(GenerateImpl generate) {
    this.generate = generate;
  }

  public Date parse(long id) {
    long timestamp = id >> GenerateImpl.TIMESTAMP_LEFT_SHIFT;
    return new Date(timestamp + generate.getEpoch());
  }

}
