package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");//从请求头获取token
        Map<Object,Object> userMap= stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);//从redis中获取用户信息
        if(!userMap.isEmpty()){
            stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.HOURS);//刷新token有效期
            UserDTO userDTO= BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);//将map转为UserDTO对象
            UserHolder.saveUser(userDTO);//保存用户信息到ThreadLocal
        }
        return true;//无论是否有用户信息都放行
    }
}
