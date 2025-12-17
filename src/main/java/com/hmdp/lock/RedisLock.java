package com.hmdp.lock;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock implements ILock {
    private String key;
    private StringRedisTemplate stringRedisTemplate;
    private static final String uuid = UUID.randomUUID().toString();//UUID用来标识不同的JVM进程
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;//定义lua脚本用于释放锁,返回值为Long类型，表示删除的key的数量

    static {
        UNLOCK_SCRIPT= new DefaultRedisScript<Long>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("script/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class); //设置返回值类型

    }
    public RedisLock(String key, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
    }

    @Override
    /**
     * @param timeoutSec 锁的过期时间，防止死锁
     */
    public boolean tryLock(long timeoutSec) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, getValue() , timeoutSec, TimeUnit.SECONDS));//原子操作,设置锁
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
       List<String> keys = java.util.Collections.singletonList(key);
        stringRedisTemplate.execute(UNLOCK_SCRIPT,keys,getValue());//使用lua脚本释放锁，保证原子性,要注意value是动态参数，传递的不是数组
        //这是不使用lua脚本的写法，存在并发问题
//        String value = stringRedisTemplate.opsForValue().get(key);
//        if(value!=null&&value.equals(uuid+"-"+ Thread.currentThread().getId())){//判断锁的拥有者是否是当前线程
//            stringRedisTemplate.delete(key);//释放锁
//        }
    }

    private String getValue(){
        return uuid+"-"+ Thread.currentThread().getId();// UUID用来标识不同的JVM进程,线程id用来标识同一JVM进程中的不同线程
    }
}
