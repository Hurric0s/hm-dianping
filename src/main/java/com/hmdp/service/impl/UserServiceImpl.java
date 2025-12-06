package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.exception.PhoneNumberException;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Override
    public void sendCode(String phone, HttpSession session) {//session 不是前端传的，而是 Spring 自动根据 Cookie 里的 sessionId 找到并注入的。
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new PhoneNumberException("电话号码格式错误");//验证手机格式
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);//将验证码存入redis，设置过期时间为2分钟
        log.info("验证码为{}", code);
    }

    @Override
    public String login(LoginFormDTO loginForm, HttpSession session) throws FailedLoginException {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            throw new PhoneNumberException("电话号码格式错误");
        }
        String code=  stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());//从redis中获取验证码和前端传过来的验证码进行比对
        if (code == null || !code.equals(loginForm.getCode())) {
            throw new FailedLoginException("验证码错误");
        }
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) { // 新用户，注册并保存
            user = createUserWithPhone(loginForm.getPhone());
            boolean saved = save(user); // ServiceImpl 提供的保存方法
            if (!saved) {
                throw new FailedLoginException("用户注册失败");
            }
        }
        String token= UUID.randomUUID().toString(true);
        UserDTO userDTO= new UserDTO();
        BeanUtil.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) ->
                                fieldValue == null ? null : fieldValue.toString())
        );//将userDTO转换为map
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);//将用户信息存入redis
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);//设置过期时间
        return  token;

    }

    private User createUserWithPhone(String phone) {
        return User.builder().phone(phone).nickName(RandomUtil.randomNumbers(10)).updateTime(LocalDateTime.now()).createTime(LocalDateTime.now()).build();
    }

}
