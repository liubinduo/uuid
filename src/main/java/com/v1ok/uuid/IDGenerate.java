package com.v1ok.uuid;

public interface IDGenerate {

  String nextIdToString() throws InterruptedException;

  Long nextIdToLong() throws InterruptedException;

}
