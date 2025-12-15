package com.hmdp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        List<String> jsonList= stringRedisTemplate.opsForList().range(RedisConstants.SHOP_TYPE,0,-1);
        if(!jsonList.isEmpty()){//缓存中有数据
            List<ShopType> shopTypeList=new ArrayList<>();
            for(String json:jsonList){
                shopTypeList.add(JSONObject.parseObject(json,ShopType.class));
            }
            return shopTypeList;
        }
        //缓存中没有数据，从数据库中查询
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if(typeList.isEmpty()){
            throw new RuntimeException("店铺类型不存在");
        }
        //将数据存入缓存
        for(ShopType shopType:typeList){
            stringRedisTemplate.opsForList().rightPush(RedisConstants.SHOP_TYPE, JSONObject.toJSONString(shopType));
        }
        return typeList;
    }
}
