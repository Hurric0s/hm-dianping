package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

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
    @Autowired
    RedissonClient redissonClient;
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = newSingleThreadExecutor();
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    //这个脚本实现了秒杀系统中的库存检查和订单创建功能
    //它首先检查指定优惠券的库存是否充足，如果库存不足则返回1；如果用户已经下过单则返回2；如果库存充足且用户未下单，则扣减库存并记录用户订单，最后返回0表示下单成功。

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<Long>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("script/optimize.lua"));
        SECKILL_SCRIPT.setResultType(Long.class); //设置返回值类型
    }

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(() -> {
            while (true) {
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 索取锁失败，重试或者直接抛异常（这个业务是一人一单，所以直接返回失败信息）
            log.error("一人只能下一单");
            return;
        }
        try {
            // 创建订单（使用代理对象调用，是为了确保事务生效）
            boolean flag = this.save(voucherOrder);
            if (!flag) {
                throw new RuntimeException("创建秒杀券订单失败");
            }
        } finally {
            lock.unlock();
        }
    }


//    @Override
//    public Result buySeckillVoucher(Long voucherId) throws InterruptedException {
//        Long userid = UserHolder.getUser().getId();
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        if (voucher == null) {
//            return Result.fail("优惠卷不存在！");
//        }
//        LocalDateTime beginTime = voucher.getBeginTime();
//        LocalDateTime endTime = voucher.getEndTime();
//        LocalDateTime nowTime = LocalDateTime.now();
//        if (!(beginTime.isBefore(nowTime) && endTime.isAfter(nowTime))) {
//            return Result.fail("优惠卷已经过期！");
//        }
//        if (voucher.getStock() <= 0) {
//            return Result.fail("优惠卷已经无库存！");
//        }
//        //RedisLock redisLock = new RedisLock(RedisConstants.LOCK_ORDER_KEY + userid, stringRedisTemplate);
//        RLock lock=   redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userid);
//        boolean islock = lock.tryLock();
//        if (!islock) {
//            return Result.fail("不允许重复下单！");
//        }
//        try {// 创建代理对象，使用代理对象调用第三方事务方法， 防止事务失效
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(userid, voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }


    @Override
    public Result buySeckillVoucher(Long voucherId) {//这是一个测试版本，不使用消息队列，直接下单
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足！" : "不能重复购买");//1代表库存不足，2代表不能重复购买
        }
        long id = redisIdWorker.getNextId(RedisConstants.CACHE_VOUCHER_KEY);//订单id
        VoucherOrder voucherOrder = VoucherOrder.builder().id(id).userId(UserHolder.getUser().getId()).payTime(LocalDateTime.now()).voucherId(voucherId).build();
        // 将订单保存到阻塞队列中
        orderTasks.add(voucherOrder);
        return Result.ok();
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
