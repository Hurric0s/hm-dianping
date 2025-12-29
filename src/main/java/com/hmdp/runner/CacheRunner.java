package com.hmdp.runner;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisCacheHelper;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CacheRunner implements CommandLineRunner {
    @Autowired
    ShopMapper shopMapper;
    @Autowired
    RedisCacheHelper redisCacheHelper;
    @Autowired
    IShopService shopService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    void contextLoads() {//这是加载商户的基本信息
        List<Long> ids = shopMapper.getAllIds();
        for (Long id : ids) {
            redisCacheHelper.rebuildCache(id, shopMapper::selectById, RedisConstants.CACHE_SHOP_KEY, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
    }

    void loadShopGeo() {//这是加载商户的地理位置到 Redis GEO 数据结构中
        List<Shop> shops = shopService.list();
        Map<Long, List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> shopMapEntry : map.entrySet()) {
            Long typeId = shopMapEntry.getKey();
            List<Shop> values = shopMapEntry.getValue();
            String key = RedisConstants.SHOP_GEO_KEY + typeId;
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
            for (Shop shop : values) {
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);//将商户的地理位置批量添加到 Redis 的 GEO 数据结构中

        }
    }

    @Override
    public void run(String... args) throws Exception {
        contextLoads();
        loadShopGeo();
    }
}
