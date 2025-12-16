package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.lock.RedisLock;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    ISeckillVoucherService seckillVoucherService;
    @Autowired
    RedisIdWorker redisIdWorker;
    @Autowired
    SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Result buySeckillVoucher(Long voucherId) {
        Long userid = UserHolder.getUser().getId();
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠卷不存在！");
        }
        LocalDateTime beginTime = voucher.getBeginTime();
        LocalDateTime endTime = voucher.getEndTime();
        LocalDateTime nowTime = LocalDateTime.now();
        if (!(beginTime.isBefore(nowTime) && endTime.isAfter(nowTime))) {
            return Result.fail("优惠卷已经过期！");
        }
        if (voucher.getStock() <= 0) {
            return Result.fail("优惠卷已经无库存！");
        }
        RedisLock redisLock = new RedisLock(RedisConstants.LOCK_ORDER_KEY + userid, stringRedisTemplate);
        Boolean islock = redisLock.tryLock(RedisConstants.LOCK_ORDER_TTL);
        if (!islock) {
            return Result.fail("不允许重复下单！");

        }
        try {// 创建代理对象，使用代理对象调用第三方事务方法， 防止事务失效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(userid, voucherId);
        } finally {
            redisLock.unlock();
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createVoucherOrder(Long userid, Long voucherId) {
        int count = query().eq("voucher_id", voucherId).eq("user_id", userid).count();
        if (count > 0) {
            throw new RuntimeException("不能重复购买！");
        }
        int updated = seckillVoucherMapper.deductStock(voucherId);
        if (updated == 0) {
            throw new RuntimeException("库存不足！");
        }
        long id = redisIdWorker.getNextId(RedisConstants.CACHE_VOUCHER_KEY);//订单id
        VoucherOrder voucherOrder = VoucherOrder.builder().id(id).userId(userid).payTime(LocalDateTime.now()).voucherId(voucherId).build();
        if (!save(voucherOrder)) {
            throw new RuntimeException("订单创建失败！");
        }
        return Result.ok(id);
    }
}
