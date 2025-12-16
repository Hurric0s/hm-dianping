package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    RedisIdWorker redisIdWorker;
    @Test
    void testtimestamp() {
        System.out.println(redisIdWorker.getNextId("test"));
    }
//    @Autowired
//    ShopMapper shopMapper;
//    @Autowired
//    RedisCacheHelper redisCacheHelper;
//    @Test
//    void contextLoads() {
//        List<Long> ids= shopMapper.getAllIds();
//        for(Long id:ids){
//             redisCacheHelper.rebuildCache(id, shopMapper::selectById, RedisConstants.CACHE_SHOP_KEY,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        }
//    }

}
