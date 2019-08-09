package com.v1ok.uuid.snowflake;

import static java.lang.Thread.sleep;

import com.v1ok.uuid.IDGenerate;
import com.v1ok.uuid.property.UUIDType;
import com.v1ok.uuid.snowflake.support.IDParseDate;
import com.v1ok.uuid.util.NumericConvertUtil;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Twitter_Snowflake SnowFlake的结构如下(每部分用-分开): 高位 |  41位时间截(毫秒级) |5位数据中心ID |  5位机器ID  | 12位序列，毫秒内的计数|
 * 0 - 0000000000 0000000000 0000000000 0000000000 0  - 00000 - 00000   -  000000000000
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 *
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值
 * （当前时间截 - 开始时间截) 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，
 * {@code 年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365)}  69 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号
 * </p>
 */
@Slf4j
public class GenerateImpl implements IDGenerate {

  private final static Lock writeLock = new ReentrantLock();

  public static final long TIME_WHEN_EPOCH = 1555664758384L;// 2019.4.19

  public static final int TO_STRING_BASE = 16;

  /**
   * 序列在id中占的位数
   */
  private final static long SEQUENCE_BITS = 12L;
  /**
   * 机器id所占的位数
   */
  private final static long WORKER_ID_BITS = 5L;

  /**
   * 数据中心标识id所占的位数
   */
  private final static long DATA_CENTER_ID_BITS = 5L;

  /**
   * 每一部分的最大值
   */
  private final static long MAX_DATA_CENTER_ID_NUM = ~(-1L << DATA_CENTER_ID_BITS);
  private final static long MAX_WORKER_ID_NUM = ~(-1L << WORKER_ID_BITS);
  private final static long MAX_SEQUENCE_NUM = ~(-1L << SEQUENCE_BITS);


  /**
   * 机器ID向左移12位
   */
  public final static long WORKER_ID_LEFT_SHIFT = SEQUENCE_BITS;

  /**
   * 数据标识id向左移17位(12+5)
   */
  public final static long DATA_CENTER_ID_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  ;

  /**
   * 时间截向左移22位(5+5+12)
   */
  public final static long TIMESTAMP_LEFT_SHIFT =
      SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
  ;

  /**
   * 工作机器ID(0~31)
   */
  private long workerId;

  /**
   * 数据中心ID(0~31)
   */
  private long dataCenterId;

  /**
   * 开始时间截 (2018-11-17)
   */
  private long epoch;


  /**
   * 上次生成ID的时间截
   */
  private static AtomicLong lastTimestamp = new AtomicLong(-1L);

  /**
   * 毫秒内序列(0~4095)
   */
  private static AtomicLong sequence = new AtomicLong(0L);
  /**
   * 用多少进制表示
   */
  private int base;

  private UUIDType uuidType;


  public GenerateImpl(long workerId, long dataCenterId) {
    this(workerId, dataCenterId, TIME_WHEN_EPOCH, TO_STRING_BASE);
  }

  public GenerateImpl(long workerId, long dataCenterId, long epoch, int base) {

    this.epoch = epoch;
    this.workerId = workerId;
    this.dataCenterId = dataCenterId;
    this.base = base;

    if (workerId > MAX_WORKER_ID_NUM || workerId < 0) {

      throw new IllegalArgumentException(String
          .format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID_NUM));

    }

    if (dataCenterId > MAX_DATA_CENTER_ID_NUM || dataCenterId < 0) {
      throw new IllegalArgumentException(
          String.format("data center Id can't be greater than %d or less than 0",
              MAX_DATA_CENTER_ID_NUM));
    }

  }

  @Override
  public Object nextId() {

    if (uuidType == UUIDType.TO_STRING) {
      return nextIdToString();
    }

    return nextIdToLong();
  }

  public String nextIdToString() {
    return NumericConvertUtil.toOtherBaseString(this.nextIdToLong(), this.base);
  }

  public Long nextIdToLong() {
    writeLock.lock();
    try {
      long timestamp = timeGen();

      long lastTimestampValue = lastTimestamp.get();
      //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过
      if (timestamp < lastTimestampValue) {
        log.warn(
            String
                .format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    (lastTimestampValue - timestamp)));
        timestamp = tilNextMillis(lastTimestampValue);
      }

      //如果是同一时间生成的，则进行毫秒内序列
      if (lastTimestampValue == timestamp) {

        long sequenceValue = sequence.incrementAndGet() & MAX_SEQUENCE_NUM;
        //毫秒内序列溢出
        if (sequenceValue == 0) {
          //阻塞到下一个毫秒,获得新的时间戳
          timestamp = tilNextMillis(lastTimestampValue);
        }
        lastTimestamp.set(timestamp);

        return ((timestamp - epoch) << TIMESTAMP_LEFT_SHIFT)
            | (dataCenterId << DATA_CENTER_ID_LEFT_SHIFT)
            | (workerId << WORKER_ID_LEFT_SHIFT)
            | sequenceValue;
      }
      //时间戳改变，毫秒内序列重置
      sequence.set(0L);
      lastTimestamp.set(timestamp);

      //移位并通过或运算拼到一起组成64位的ID
      return ((timestamp - epoch) << TIMESTAMP_LEFT_SHIFT)
          | (dataCenterId << DATA_CENTER_ID_LEFT_SHIFT)
          | (workerId << WORKER_ID_LEFT_SHIFT)
          | sequence.get();
    } finally {
      writeLock.unlock();
    }

  }

  public long getEpoch() {
    return epoch;
  }

  public void setEpoch(long epoch) {
    this.epoch = epoch;
  }

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

  //  private long getSequenceValue(long lastTimestamp) {
//    Long sequenceValue = SEQUENCE_CACHE.put(lastTimestamp, 0L);
//    if (sequenceValue == null) {
//      return 0L;
//    }
//    sequenceValue = (sequenceValue + 1L) & MAX_SEQUENCE_NUM;
//    SEQUENCE_CACHE.put(lastTimestamp, sequenceValue);
//    return sequenceValue;
//  }

  /**
   * 阻塞到下一个毫秒，直到获得新的时间戳
   *
   * @return 当前时间戳
   */
  private long tilNextMillis(long lastTimestamp) {
    long timestamp = timeGen();

    while (timestamp <= lastTimestamp) {
      long sleepTime = lastTimestamp - timestamp;
      try {
        sleep(sleepTime);
      } catch (InterruptedException e) {
        log.error("Thread sleep is error", e);
      }
      timestamp = timeGen();
    }
    return timestamp;
  }

  /**
   * 返回以毫秒为单位的当前时间
   *
   * @return 当前时间(毫秒)
   */
  private long timeGen() {
    return System.currentTimeMillis();
  }

  /*===========================
          TEST Method
   ============================*/

  static final int THREAD_COUNT = 6;
  static GenerateImpl idGenerate = new GenerateImpl(1, 1);
  static CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);
  static Set<Long> TEXT_IDS = Collections.synchronizedSet(new HashSet<>());

  public static void main(String[] args) throws InterruptedException {

    System.out.println(System.currentTimeMillis());
    Long x = idGenerate.nextIdToLong();

    Date parse = new IDParseDate(idGenerate).parse(x);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String startTime = sdf.format(parse);
    System.out.println(startTime);

    System.out.println(x);
    System.out.println(idGenerate.nextIdToString());
    System.out.println(x.toString().length());

    for (int i = 0; i < THREAD_COUNT; i++) {

      Thread thread = new Thread(() -> {
        for (int j = 0; j < 1000; j++) {

          Long id = idGenerate.nextIdToLong();
          if (TEXT_IDS.contains(id)) {
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("has same id %d [%s]", id, threadName));
          }
          TEXT_IDS.add(id);

        }
        countDownLatch.countDown();
      });
      thread.setName("TEST[" + i + "]");
      thread.start();
    }
    countDownLatch.await();
    System.out.println(TEXT_IDS.size());
  }

}
