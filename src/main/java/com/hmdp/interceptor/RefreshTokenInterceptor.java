package com.hmdp.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1️⃣ 从请求头获取 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true; // 无 token 直接放行
        }

        // 2️⃣ 从 Redis 获取 JSON
        String key = RedisConstants.LOGIN_USER_KEY + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);

        // 3️⃣ 不存在登录信息
        if (StrUtil.isBlank(userJson)) {
            return true;
        }

        // 4️⃣ JSON -> UserDTO
        UserDTO userDTO = JSONUtil.toBean(userJson, UserDTO.class);

        // 5️⃣ 保存到 ThreadLocal
        UserHolder.saveUser(userDTO);

        // 6️⃣ 刷新 token 有效期（滑动过期）
        stringRedisTemplate.expire(
                key,
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.HOURS
        );
        return true;
    }
}
