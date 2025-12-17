package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    RedisIdWorker redisIdWorker;
    @Autowired
    RedissonClient redissonClient;
//    @Test
//    void testtimestamp() {
//        System.out.println(redisIdWorker.getNextId("test"));
//    }
//
//
//    @Test
//    void testredisson() throws InterruptedException {
//        RLock lock = redissonClient.getLock("test");
//        boolean islock = lock.tryLock(1,10, TimeUnit.MINUTES );//参数分别为等待时间和自动解锁时间
//        if(islock){
//            try {
//                System.out.println("正在执行逻辑");
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            finally {
//                lock.unlock();
//            }
//        }
//    }
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
