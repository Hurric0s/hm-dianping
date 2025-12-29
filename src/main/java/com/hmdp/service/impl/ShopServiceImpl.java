package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.exception.ShopNotFoundException;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisCacheHelper;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private RedisCacheHelper redisCacheHelper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Shop queryShopById(Long id) {//先从redis中查询店铺信息
        return redisCacheHelper.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES, RedisConstants.LOCK_SHOP_KEY);
    }

    //    private boolean setlock(String key) {//设置分布式锁
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
//        return Boolean.TRUE.equals(flag);
//
//    }
//
//    private boolean unlock(String key) {//释放分布式锁
//        return stringRedisTemplate.delete(key);
//    }
//
//    private Shop queryWithMutex(Long id) throws InterruptedException {
//        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id.toString());
//        if (shopJson == null) {
//            boolean islock = setlock(RedisConstants.LOCK_SHOP_KEY + id.toString());//获取分布式锁
//            if (!islock) {//获取锁失败，休眠重试
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                return queryShopById(id);//递归重试
//            }
//            //获取锁成功，查询数据库
//            //double check
//            if (stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id.toString()) != null) {
//                unlock(RedisConstants.LOCK_SHOP_KEY + id.toString());
//                return JSONUtil.toBean(stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id.toString()), Shop.class);
//            }
//            Shop shop = getById(id);
//            Thread.sleep(200);//模拟处理业务逻辑的延时
//            if (shop == null) {
//                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id.toString(), "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);//数据库中不存在该店铺，缓存空值，防止缓存穿透
//                return null;
//            }
//            //数据库中存在该店铺，写入redis
//            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id.toString(), JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//            //释放锁
//            unlock(RedisConstants.LOCK_SHOP_KEY + id.toString());
//            return shop;
//        }
//        return JSONUtil.toBean(shopJson, Shop.class);//如果缓存中存在，直接返回
//
//    }
//
//    public Shop queryWithLogicExpire(Long id) {
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        // 1. 查询缓存
//        String json = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(json)) {
//            // 逻辑过期方案：缓存不存在直接返回
//            return null;
//        }
//        // 2. 反序列化 RedisData
//        RedisData<Shop> redisData = JSONObject.parseObject(json, RedisData.class);
//        Shop shop = redisData.getData();
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        // 3. 未过期，直接返回
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;
//        }
//        // 4. 已过期，尝试获取互斥锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean isLock = setlock(lockKey);
//        if (isLock) {
//            // 5. 获取锁成功，异步重建缓存
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    rebuildShopCache(id);
//                } finally {
//                    unlock(lockKey);
//                }
//            });
//        }
//        //接下来是进行double check ：如果没有搞到锁，但是别的进程已经进行了redis更新，那么就可以拿来用
//        String json = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(json)) {
//            // 逻辑过期方案：缓存不存在直接返回
//            return null;
//        }
//        // 2. 反序列化 RedisData
//        RedisData<Shop> redisData = JSONObject.parseObject(json, RedisData.class);
//        Shop shop = redisData.getData();
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        // 3. 未过期，直接返回
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;
//        }
//
//        return shop;
//    }
    @Override
    public void updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            throw new ShopNotFoundException("店铺id不能为空");
        }
        updateById(shop);//先更新数据库，然后删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x == null || y == null) {
            Page<Shop> page = query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        int start = (current - 1) * DEFAULT_BATCH_SIZE;
        int end = current * DEFAULT_BATCH_SIZE;
        GeoResults<RedisGeoCommands.GeoLocation<String>> search = stringRedisTemplate.opsForGeo().search(
                RedisConstants.SHOP_GEO_KEY + typeId,
                GeoReference.fromCoordinate(new Point(x, y)),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));//分页查询，返回指定范围内的店铺
        if (search == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = search.getContent();
        if (list.size() <= start) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(start).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);

    }


}
