package com.v1ok.uuid.java;

import com.v1ok.uuid.IDGenerate;
import java.util.UUID;

public class GenerateImpl implements IDGenerate {

  @Override
  public String nextIdToString() throws InterruptedException {
    return UUID.randomUUID().toString();
  }

  @Override
  public Long nextIdToLong() throws InterruptedException {
    throw new UnsupportedOperationException("This method is unsupported");
  }
}
