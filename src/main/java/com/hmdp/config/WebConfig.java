package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
@Slf4j
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
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {// 扩展消息转换器
        log.info("扩展消息转换器");
        // 创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 为消息转换器设置一个对象转换器, 对象转换器可以将Java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        // 将自己的消息转换器加到容器的最前面
        converters.add(0, converter);
    }
}


