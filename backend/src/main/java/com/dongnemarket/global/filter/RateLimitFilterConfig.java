package com.dongnemarket.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RateLimitFilter를 서블릿 컨테이너 필터 체인에 등록한다. Spring Security 필터 체인보다 앞단(가장 먼저)에서
 * 동작해, 한도를 초과한 요청은 인증·인가 등 이후 처리를 전혀 거치지 않고 즉시 거부된다.
 * <p>test 프로파일은 RedisAutoConfiguration이 빠져 StringRedisTemplate 빈이 없으므로 이 설정 자체를 제외한다
 * (auth의 RedisLoginAttemptRepository와 동일한 방식).
 */
@Configuration
@Profile("!test")
public class RateLimitFilterConfig {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitFilterConfig(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            @Value("${rate-limit.capacity:60}") long capacity,
            @Value("${rate-limit.window-seconds:10}") long windowSeconds) {
        RateLimitFilter filter = new RateLimitFilter(redisTemplate, objectMapper, capacity, windowSeconds);
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
