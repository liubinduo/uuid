package com.v1ok.uuid.snowflake.support;

import com.v1ok.uuid.snowflake.GenerateImpl;
import java.util.Date;

public class IDParseDate {

  public Date parse(long id) {
    long timestamp = id >> GenerateImpl.TIMESTAMP_LEFT_SHIFT;
    return new Date(timestamp + GenerateImpl.TIME_WHEN_EPOCH);
  }

}
