package com.boot.study.keygen;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Snowflake程序id生成器 源于Twitter的Snowflake算法 但由于原版算法对应的分布式层级结构太简单，所以目前的算法实际是Sony对Snowflake算法的改进版本的Sonyflake算法
 * Sonyflake算法原版实现语言为Golang（可参考github中的开源项目），当前类是java翻译版本 Created by Marion on 2019/6/27.
 */
public final class SnowflakeKeygen {
    /* --------------------------------------------------成员和构造函数---------------------------------------------- */
    // 常量
    private static final int[] loopback = new int[]{127, 0, 0, 1};// 默认ip
    private static final long SnowflakeTimeUnit = (long) 1e7; // 时间单位，一纳秒的多少倍，1e6 = 一毫秒，1e7 = 百分之一秒，1e8 = 十分之一秒
    private static final int BitLenSequence = 8; // 序列号的个数最多256个(0-255)，即每单位时间并发数，如时间单位是1e7，则单实例qps = 25600
    private static final int BitLenDataCenterId = 3; // 数据中心个数最多8个(0-7)，即同一个环境（生产、预发布、测试等）的数据中心（假设一个机房相同数据域的应用服务器集群只有一个，则数据中心数等于机房数）最多有8个
    private static final int BitLenMachineId = 16; // 同一个数据中心下最多65536个应用实例（0-65535），默认是根据实例ip后两段算实例id（k8s环境动态创建Pod，也建议用此方式），所以需要预留255
    // * 255这么多
    private static final long BitLenTime = (long) Math.pow(2, 36); // 时间戳之差最大 = 2的36次方 * 时间单位 / 1e9
    // 秒，目前的设计最多可以用21.79年就需要更新开始时间（随之还需要归档旧数据和更新次新数据id）
    // 总共63位，不超过bit64

    // 成员变量
    private static final ReentrantLock lock = new ReentrantLock();
    private long elapsedTime;
    private final long startTime;
    private int sequence;
    private final int dataCenterId;
    private final int machineId;

    // 私有构造
    private SnowflakeKeygen() {
        // 以下的成员变量初始化值可以改为由配置中获取
        this.startTime = toSnowflakeTime(LocalDateTime.of(2020, 10, 1, 0, 0, 0, 0));
        this.sequence = (1 << BitLenSequence) - 1;
        this.dataCenterId = 0;
        this.machineId = getPrivateIPv4Id();
    }

    /**
     * 获得当前单例对象的实例
     *
     * @return 时间戳生成对象
     */
    public static SnowflakeKeygen getInstance() {
        return Nested.instance;
    }

    /* ----------------------------------------------------id生成运算----------------------------------------------- */

    /**
     * 获取程序生成的唯一id
     *
     * @return id
     */
    public static Long getNextId() {
        return SnowflakeKeygen.getInstance().getKey();
    }

    /**
     * 生成程序编号，实现自定义的程序生产id接口IProgramKey的方法 但SnowflakeKeygen算法实际上用不上传入的类型和泛型参数，如果没有实现IProgramKey接口的需求，可以改掉
     *
     * @param <T>   类型泛型
     * @return 程序编号
     */
    public <T> Long getKey() {
        int maskSequence = (1 << BitLenSequence) - 1;

        lock.lock();
        try {
            long current = getCurrentElapsedTime(this.startTime);
            if (this.elapsedTime < current) {
                this.elapsedTime = current;
                this.sequence = 0;
            } else { // this.elapsedTime >= current
                this.sequence = (this.sequence + 1) & maskSequence;
                if (this.sequence == 0) {
                    this.elapsedTime++;
                    long overtime = this.elapsedTime - current;
                    long sleepTime = getSleepTime(overtime);
                    Thread.sleep(sleepTime / 1000000, (int) (sleepTime % 1000000));
                }
            }

            return toId();
        } catch (InterruptedException e) { // 此异常仅Thread.sleep会抛出，正常情况不可能出现
            Thread.currentThread().interrupt();
            // 记录中断异常，但不打印堆栈（性能优化）
            return toId();
        } finally {
            lock.unlock();
        }
    }

    private long toId() {
        if (this.elapsedTime >= BitLenTime) {
            return 0;
        }

        return this.elapsedTime << (BitLenSequence + BitLenDataCenterId + BitLenMachineId)
                | this.sequence << (BitLenDataCenterId + BitLenMachineId) | this.dataCenterId << BitLenMachineId
                | this.machineId;
    }

    /* ------------------------------------------------------时间运算------------------------------------------------ */
    private static long getUnixNano(LocalDateTime t) {
        long milliSecond = t.toInstant(ZoneOffset.UTC).toEpochMilli();
        int nano = t.toInstant(ZoneOffset.UTC).getNano();
        return milliSecond * 1000000 + nano / 1000;
    }

    private static long toSnowflakeTime(LocalDateTime t) {
        return getUnixNano(t) / SnowflakeTimeUnit;
    }

    private static long getCurrentElapsedTime(long startTime) {
        return toSnowflakeTime(LocalDateTime.now()) - startTime;
    }

    private static long getSleepTime(long overtime) {
        return overtime * 10000000 - getUnixNano(LocalDateTime.now()) % SnowflakeTimeUnit;
    }

    /* ------------------------------------------------------本地IP运算---------------------------------------------- */
    public static int[] getPrivateIPv4() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipAddress = addr.getAddress();
            int[] ip4 = new int[]{ipAddress[0] & 0xff, ipAddress[1] & 0xff, ipAddress[2] & 0xff, ipAddress[3] & 0xff};
            if (isPrivateIPv4(ip4)) {
                return ip4;
            } else {
                return loopback;
            }
        } catch (Exception ignored) {
            return loopback;
        }
    }

    private static boolean isPrivateIPv4(int[] ip) {
        return ip != null && ip.length > 3
                && (ip[0] == 10 || ip[0] == 172 && (ip[1] >= 16 && ip[1] < 32) || ip[0] == 192 && ip[1] == 168);
    }

    private static int getPrivateIPv4Id() {
        int[] ip = getPrivateIPv4();
        return (ip[2] << 8) + ip[3];
    }

    /* ------------------------------------------------------单例内部类---------------------------------------------- */
    private static final class Nested {
        private static final SnowflakeKeygen instance = new SnowflakeKeygen();
    }

    /* -------------------------------------------------------测试方法----------------------------------------------- */
/*    public static void main(String[] args) {
        int poolSize = 20;
        int idSize = 10;
        List<Long> list = new CopyOnWriteArrayList<>();
        CountDownLatch counter = new CountDownLatch(idSize);

        long startTime = getUnixNano(LocalDateTime.now());
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            for (int i = 0; i < idSize; i++) {
                pool.execute(() -> {
                    list.add(getNextId());
                    counter.countDown();
                });
            }

            try {
                counter.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        } finally {
            pool.shutdown();
        }

        long endTime = getUnixNano(LocalDateTime.now());
        long usedTime = (endTime - startTime);
        System.out.printf("Id size: %d\nTotal seconds:%d\nAvg nanos per id:%d\n", list.size(), usedTime / 1000000000L,
                usedTime / idSize);

        long badSize = list.stream().collect(Collectors.groupingBy(k -> k)).entrySet().stream()
                .filter(p -> p.getValue().size() > 1).count();

        if (badSize > 0) {
            System.out.println("Not Good");
        } else {
            System.out.println("OK");
        }
    }*/
}
