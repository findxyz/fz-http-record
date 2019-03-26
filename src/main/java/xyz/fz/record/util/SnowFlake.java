package xyz.fz.record.util;

/**
 * twitter的snowflake算法 -- java实现
 *
 * author beyond
 * date 2016/11/26
 *
 * update 优化变量及方法名称
 * editor __f1ndwh7
 * date 2018/05/31
 */
public class SnowFlake {

    /*
        snow flake bits meaning
        0 is always the first bit
        41bit timestamp = nowTimestamp - startTimestamp
        5bit dataCenterId (custom decision)
        5bit machineId  (custom decision)
        12bit sequence = 4096 of 1 millisecond
        1 + 41 + 5 + 5 + 12 = 64 = one Long number
     */

    /*
        5bit max number
        1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
        (-1L)
        ^
        1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0
        (-1L << 5)
        =
        0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1
        (31L)
     */

    /**
     * 起始的时间戳
     */
    private final static long START_TIME_STAMP = 1527753682640L;

    /**
     * 每一部分占用的位数
     */
    private final static long DATA_CENTER_BIT = 5;
    private final static long MACHINE_BIT = 5;
    private final static long SEQUENCE_BIT = 12;

    /**
     * 每一部分的最大值
     */
    private final static long MAX_DATA_CENTER_NUM = ~(-1L << DATA_CENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long TIME_STAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;
    private final static long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;
    private final static long MACHINE_LEFT = SEQUENCE_BIT;

    private long lastTimestamp = -1L;
    private long dataCenterId;
    private long machineId;
    private long sequence = 0L;

    public SnowFlake(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId can't be greater than MAX_DATA_CENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     */
    public synchronized long generateNextId() {

        long nowTimestamp = nowTimestamp();
        if (nowTimestamp > lastTimestamp) {
            sequence = 0L;
        } else if (nowTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                nowTimestamp = nextTimestamp();
            }
        } else {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        lastTimestamp = nowTimestamp;

        long timestampTemp = (nowTimestamp - START_TIME_STAMP) << TIME_STAMP_LEFT;
        long dataCenterIdTemp = dataCenterId << DATA_CENTER_LEFT;
        long machineIdTemp = machineId << MACHINE_LEFT;
        long sequenceTemp = sequence;

        return timestampTemp | dataCenterIdTemp | machineIdTemp | sequenceTemp;
    }

    private long nowTimestamp() {
        return System.currentTimeMillis();
    }

    private long nextTimestamp() {
        long nowTimestamp = nowTimestamp();
        while (nowTimestamp <= lastTimestamp) {
            nowTimestamp = nowTimestamp();
        }
        return nowTimestamp;
    }

    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(2, 3);

        long beginTime = System.currentTimeMillis();
        for (long i = 0; i < (1L << 16); i++) {
            System.out.println(snowFlake.generateNextId());
        }
        long endTime = System.currentTimeMillis();

        System.out.println(endTime - beginTime);
    }
}