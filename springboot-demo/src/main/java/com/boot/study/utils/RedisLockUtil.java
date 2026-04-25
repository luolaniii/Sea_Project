package com.boot.study.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.boot.study.consts.fun.Callable;
import com.boot.study.enums.redis.RLockKeyEnum;
import com.boot.study.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author: study
 **/
@Slf4j
public class RedisLockUtil {

    /**
     * 分布式锁名前缀
     */
    private static final String DISTRIBUTED_SYNCHRONIZATION_LOCK = "LOCK:SYN_LOCK_";

    private static RedissonClient redissonClient;

    /**
     * 初始化
     */
    public static void init(RedissonClient redissonClient) {
        RedisLockUtil.redissonClient = redissonClient;
    }


    /**
     * lock加锁的方法，如果尝试加锁成功，会直接返回
     * 加锁失败。会开启异步任务订阅锁，在循环中一直尝试加锁，直到加锁成功，取消订阅返回
     * 任务执行时间超长，运行期间会自动给锁续期，不用担心锁自动过期，业务执行完，就不会给锁续期，手段不释放锁，锁默认在30s后自动删除
     */
    public static <T> T tryLock(RLockKeyEnum keyEnum, Callable<T> callable) {
        //获取锁
        RLock lock = redissonClient.getLock(RLockKeyEnum.formatKey(keyEnum));
        try {
            if (lock.tryLock()) {
                //执行业务流程
                return callable.execute();
            } else {
                log.error("加锁失败，请稍后重试,请求锁为:{}", RLockKeyEnum.formatKey(keyEnum));
                throw new ServiceException("处理失败，请稍后重试！");
            }
        } finally {
            //释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 在指定时间内尝试获取锁，获取失败，抛出异常
     * 同样在获取锁以后，redisson有锁续期的机制，不需要设置锁的过期时间
     */
    public static <T> T tryLockInTime(RLockKeyEnum keyEnum, Callable<T> callable) {
        RLock lock = redissonClient.getLock(RLockKeyEnum.formatKey(keyEnum));
        checkWaitTime(keyEnum);
        try {
            if (lock.tryLock(keyEnum.getWaitTime(), keyEnum.getLeaseTime(), TimeUnit.SECONDS)) {
                //执行业务逻辑
                return callable.execute();
            } else {
                log.error("加锁失败，请稍后重试,请求锁为:{}", RLockKeyEnum.formatKey(keyEnum));
                throw new ServiceException("系统繁忙，请稍后重试！");
            }
        } catch (InterruptedException e) {
            //锁被中断，保留中断发生的证据，以便调用栈中更高层的代码能知道中断，并对中断作出响应
            Thread.currentThread().interrupt();
        } finally {
            //释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        throw new ServiceException("处理失败，请稍后重试！");
    }

    public static void tryLockInTime(RLockKeyEnum keyEnum, Runnable runnable) {
        RLock lock = redissonClient.getLock(RLockKeyEnum.formatKey(keyEnum));
        checkWaitTime(keyEnum);
        try {
            if (lock.tryLock(keyEnum.getWaitTime(), keyEnum.getLeaseTime(), TimeUnit.SECONDS)) {
                //执行业务逻辑
                runnable.run();
            } else {
                log.error("加锁失败，请稍后重试,请求锁为:{}", RLockKeyEnum.formatKey(keyEnum));
                throw new ServiceException("系统繁忙，请稍后重试！");
            }
        } catch (InterruptedException e) {
            //锁被中断，保留中断发生的证据，以便调用栈中更高层的代码能知道中断，并对中断作出响应
            Thread.currentThread().interrupt();
        } finally {
            //释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    private static void checkWaitTime(RLockKeyEnum keyEnum) {
        if (ObjectUtil.isNull(keyEnum.getWaitTime()) || keyEnum.getWaitTime() < 0L) {
            log.error("加锁参数有误导致加锁失败，RLockKeyEnum：{}", JSONUtil.toJsonStr(keyEnum));
            throw new ServiceException("处理失败，请稍后重试！");
        }
    }
}
