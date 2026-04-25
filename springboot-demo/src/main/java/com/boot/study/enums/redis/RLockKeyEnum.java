package com.boot.study.enums.redis;

import lombok.Getter;


@Getter
public enum RLockKeyEnum {

    USER_ID("USER_ID:%s", 3L, 5L, "用户登录锁"),
    USER_NAME_REGISTER("USER_NAME_REGISTER", 3L, 5L, "用户名注册锁"),

    ;

    RLockKeyEnum(String keyPrefix, Long waitTime, Long leaseTime, String desc) {
        this.keyPrefix = keyPrefix;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
        this.desc = desc;
    }

    /**
     * 分布式锁的业务前缀
     */
    public static final String BUSINESS_PREFIX = "lock";

    /**
     * RLock锁的前缀，不确定的部分通过占位符标识
     */
    private final String keyPrefix;

    /**
     * 等待时间，在等待时间期间内会持续尝试获取锁，在这个时间内没有获取到锁就会返回false，加锁失败
     * RLock对象的lock()/tryLock()方法不需要这个时间，但有些方法需要等待时间，这个参数会在实际加锁调用时校验，校验不通过直接抛异常
     * 在RLock还有个时间概念是锁的过期时间，这个不建议手动设置，因为RLock有自动续期机制，锁默认30s过期，锁过期前业务未执行完，会自动续期
     */
    private final Long waitTime;

    /**
     * 上锁后自动释放时间
     */
    private final Long leaseTime;

    /**
     * 使用场景描述
     */
    private final String desc;

    private String param;

    public RLockKeyEnum param(Object values) {
        if (values instanceof String) {
            this.param = (String) values;
        } else {
            this.param = String.valueOf(values);
        }
        return this;
    }

    /**
     * 格式化key，拿到替换了占位符后的实际key
     */
    public static String formatKey(RLockKeyEnum rLockKeyEnum) {
        return String.join(BUSINESS_PREFIX, String.format(rLockKeyEnum.keyPrefix, rLockKeyEnum.param));
    }
}
