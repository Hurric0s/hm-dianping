package com.hmdp.lock;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock implements ILock {
    private String key;
    private StringRedisTemplate stringRedisTemplate;
    private static final String uuid = UUID.randomUUID().toString();//UUID用来标识不同的JVM进程
    public RedisLock(String key, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
    }

    @Override
    /**
     * @param timeoutSec 锁的过期时间，防止死锁
     */
    public boolean tryLock(long timeoutSec) {
        long threadId = Thread.currentThread().getId();//获取线程id，用来标识同一JVM进程中的不同线程
        String value = uuid+"-"+ threadId;//value由JVM进程标识和线程id组成,用来标识锁的拥有者
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value , timeoutSec, TimeUnit.SECONDS));//原子操作,设置锁

    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        String value = stringRedisTemplate.opsForValue().get(key);
        if(value!=null&&value.equals(uuid+"-"+ Thread.currentThread().getId())){//判断锁的拥有者是否是当前线程
            stringRedisTemplate.delete(key);//释放锁
        }
    }
}
