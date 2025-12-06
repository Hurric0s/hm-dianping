package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Autowired
    LoginInterceptor loginInterceptor;
    @Autowired
    RefreshTokenInterceptor refreshTokenInterceptor;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor).addPathPatterns("/**");
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns("/user/logout", "/user/login", "/shop/**", "/voucher/**", "/upload/**", "/user/code", "/blog/hot", "/shop-type/**");
    }
}


