package com.hmdp.config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redisClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");// 请根据实际情况修改地址和端口
        return Redisson.create(config);
    }
}
