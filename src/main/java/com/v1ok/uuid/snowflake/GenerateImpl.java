package com.v1ok.uuid.snowflake;

import static java.lang.Thread.sleep;

import com.v1ok.uuid.IDGenerate;

/**
 * Twitter_Snowflake<br> SnowFlake的结构如下(每部分用-分开):<br> 高位 |  41位时间截(毫秒级)                            |
 * 5位数据中心ID |  5位机器ID  | 12位序列，毫秒内的计数| <br>0 - 0000000000 0000000000 0000000000 0000000000 0  -
 * 00000 - 00000   -  000000000000 <br>
 *
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 * <ul>
 * <li>1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0</li>
 * <li>41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截) 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T
 * = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69</li>
 * <li>10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId</li>
 * <li>12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号</li>
 * </ul>
 */
public class GenerateImpl implements IDGenerate {

  public static final long TIME_WHEN_EPOCH = 1542444628916L;

  /**
   * 序列在id中占的位数
   */
  private final long sequenceBits = 12L;

  /**
   * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
   */
  private final long sequenceMask = -1L ^ (-1L << sequenceBits);


  /**
   * 开始时间截 (2018-11-17)
   */
  private long twepoch;

  /**
   * 机器id所占的位数
   */
  private long workerIdBits;

  /**
   * 数据中心标识id所占的位数
   */
  private long dataCenterIdBits;

  /**
   * 机器ID向左移12位
   */
  private long workerIdShift;

  /**
   * 数据标识id向左移17位(12+5)
   */
  private long dataCenterIdShift;

  /**
   * 时间截向左移22位(5+5+12)
   */
  private long timestampLeftShift;

  /**
   * 工作机器ID(0~31)
   */
  private long workerId;

  /**
   * 数据中心ID(0~31)
   */
  private long dataCenterId;

  /**
   * 毫秒内序列(0~4095)
   */
  private long sequence = 0L;

  /**
   * 上次生成ID的时间截
   */
  private long lastTimestamp = -1L;

  public GenerateImpl(long workerId, long dataCenterId) {
    this(workerId, dataCenterId, TIME_WHEN_EPOCH);
  }

  public GenerateImpl(long workerId, long dataCenterId, long twepoch) {
    this.workerIdBits = 5L;
    this.dataCenterIdBits = 5L;
    this.twepoch = twepoch;
    this.workerId = workerId;
    this.dataCenterId = dataCenterId;
    this.workerIdShift = this.sequenceBits;
    this.dataCenterIdShift = this.sequenceBits + this.workerIdBits;
    this.timestampLeftShift = this.sequenceBits + this.dataCenterIdBits;

    if (workerId > this.getMaxWorkerId() || workerId < 0) {

      throw new IllegalArgumentException(String
          .format("worker Id can't be greater than %d or less than 0", this.getMaxWorkerId()));

    }

    if (dataCenterId > this.getMaxDateCenterId() || dataCenterId < 0) {
      throw new IllegalArgumentException(
          String.format("data center Id can't be greater than %d or less than 0",
              this.getMaxDateCenterId()));
    }

  }

  /**
   * 获取最大机器id (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
   *
   * @return 最大机器ID
   */
  protected long getMaxWorkerId() {
    return -1L ^ (-1L << workerIdBits);
  }

  /**
   * 获取最大数据中心id
   *
   * @return 最大数据中心Id
   */
  protected long getMaxDateCenterId() {
    return -1L ^ (-1L << dataCenterIdBits);
  }

  public String nextIdToString() throws InterruptedException {
    return Long.toHexString(this.nextIdToLong());
  }

  public synchronized Long nextIdToLong() throws InterruptedException {
    long timestamp = timeGen();

    //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
    if (timestamp < lastTimestamp) {
      throw new RuntimeException(
          String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
              lastTimestamp - timestamp));
    }

    //如果是同一时间生成的，则进行毫秒内序列
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
      //毫秒内序列溢出
      if (sequence == 0) {
        //阻塞到下一个毫秒,获得新的时间戳
        timestamp = tilNextMillis(lastTimestamp);
      }
    }
    //时间戳改变，毫秒内序列重置
    else {
      sequence = 0L;
    }

    //上次生成ID的时间截
    lastTimestamp = timestamp;

    //移位并通过或运算拼到一起组成64位的ID
    return ((timestamp - twepoch) << timestampLeftShift) //
        | (dataCenterId << dataCenterIdShift) //
        | (workerId << workerIdShift) //
        | sequence;
  }

  /**
   * 阻塞到下一个毫秒，直到获得新的时间戳
   *
   * @param lastTimestamp 上次生成ID的时间截
   * @return 当前时间戳
   */
  protected long tilNextMillis(long lastTimestamp) throws InterruptedException {
    long timestamp = timeGen();

    while (timestamp <= lastTimestamp) {
      long sleepTime = lastTimestamp - timestamp;
      sleep(sleepTime);
      timestamp = timeGen();
    }
    return timestamp;
  }

  /**
   * 返回以毫秒为单位的当前时间
   *
   * @return 当前时间(毫秒)
   */
  protected long timeGen() {
    return System.currentTimeMillis();
  }


  public static void main(String[] args) throws InterruptedException {
    IDGenerate idGenerate = new GenerateImpl(1, 0);
    for (int i = 0; i < 1000; i++) {
      Long id = idGenerate.nextIdToLong();
//      System.out.println(Long.toBinaryString(id));
      System.out.println(id + "  " + Long.toHexString(id));
      System.out.println(id.toString().length() + "  " + Long.toHexString(id).length());
    }

    String s = String.valueOf(Long.MAX_VALUE);
    System.out.println(s + "   " + s.length()  +" "+ Long.toHexString(Long.MAX_VALUE).length());
  }

}
