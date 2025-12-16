package com.hmdp.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP=1765803366L;
    public  long getNextId(String prefix) {
        long cur_seconds = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8)); // 东八区
        long timestamp=cur_seconds-BEGIN_TIMESTAMP;
        String timestring =LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count=stringRedisTemplate.opsForValue().increment(prefix+timestring);
        timestamp<<=32;//左移32位,腾出低32位给自增序列
        timestamp|=count;//按位或运算,将自增序列放到低32位
        return timestamp;
    }


}
