package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.User;
import com.hmdp.exception.PhoneNumberException;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

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

    @Override
    public void sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            throw new PhoneNumberException("电话号码格式错误");//验证手机格式
        }
        String code= RandomUtil.randomNumbers(6);
        session.setAttribute("code",code);
        log.info("验证码为{}",code);
    }
}
