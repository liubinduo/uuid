package com.v1ok.uuid.java;

import com.v1ok.uuid.IDGenerate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateImpl implements IDGenerate {

  @Override
  public String nextIdToString() {
    return UUID.randomUUID().toString();
  }

  @Override
  public Long nextIdToLong() {
    throw new UnsupportedOperationException("This method is unsupported");
  }

  @Override
  public Object nextId() {
    return this.nextIdToString();
  }
}
