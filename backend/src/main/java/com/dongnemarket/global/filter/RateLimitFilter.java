package com.dongnemarket.global.filter;

import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 클라이언트(IP)별 API 요청 속도를 슬라이딩 윈도우 방식으로 제한한다.
 * <p>로그인 시도 제한({@code auth.LoginAttemptService}, 고정 윈도우)과는 별개의, 모든 {@code /api/**}
 * 요청에 적용되는 범용 남용/과도한 트래픽 방지 장치다.
 * <p>Redis ZSET에 "요청시각"을 원소로 쌓아두고, 매 요청마다 윈도우 밖(오래된) 원소를 제거한 뒤
 * 남은 개수로 한도를 판단한다 — 고정 윈도우와 달리 윈도우 경계에서 순간적으로 두 배 허용되는 문제가 없다.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long capacity;
    private final long windowMillis;

    public RateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                            long capacity, long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.windowMillis = windowSeconds * 1000;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String key = KEY_PREFIX + clientId(request);
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, windowStart);
        Long currentCount = redisTemplate.opsForZSet().zCard(key);

        if (currentCount != null && currentCount >= capacity) {
            writeTooManyRequests(response);
            return;
        }

        redisTemplate.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        redisTemplate.expire(key, Duration.ofMillis(windowMillis + 1000));

        filterChain.doFilter(request, response);
    }

    /** 프록시/로드밸런서를 거치는 경우를 대비해 X-Forwarded-For를 우선 확인하고, 없으면 원격 주소를 쓴다. */
    private String clientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS));
    }
}
