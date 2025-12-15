package com.hmdp.runner;

import com.hmdp.mapper.ShopMapper;
import com.hmdp.utils.RedisCacheHelper;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
@Component
public class CacheRunner implements CommandLineRunner {
    @Autowired
    ShopMapper shopMapper;
    @Autowired
    RedisCacheHelper redisCacheHelper;
    void contextLoads() {
        List<Long> ids= shopMapper.getAllIds();
        for(Long id:ids){
            redisCacheHelper.rebuildCache(id, shopMapper::selectById, RedisConstants.CACHE_SHOP_KEY,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
    }
    @Override
    public void run(String... args) throws Exception {
        contextLoads();
    }
}
