package com.dongnemarket.global.security;

import com.dongnemarket.global.security.jwt.JwtAccessDeniedHandler;
import com.dongnemarket.global.security.jwt.JwtAuthenticationEntryPoint;
import com.dongnemarket.global.security.jwt.JwtAuthenticationFilter;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 공통 보안 설정 (JWT, stateless). URL 권한 정책은 00-ai-common-rules.md §7 기준.
 * 팀장만 수정한다.
 */
@Configuration
public class SecurityConfig {

	private static final String[] SWAGGER_WHITELIST = {
			"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**"
	};

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;
	private final JwtAccessDeniedHandler accessDeniedHandler;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider,
						  JwtAuthenticationEntryPoint authenticationEntryPoint,
						  JwtAccessDeniedHandler accessDeniedHandler) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Swagger 문서
						.requestMatchers(SWAGGER_WHITELIST).permitAll()
						// 헬스체크(배포/모니터링용,인증 불필요)
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						// Prometheus 메트릭 스크레이프 (로컬 모니터링용, 인증 불필요 — 운영 반영 시 접근 제한 필요)
						.requestMatchers("/actuator/prometheus").permitAll()
						// 인증 불필요 (회원가입/로그인, 공개 조회)
						.requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{productId}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/regions").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products/{productId}/comments").permitAll()

						// 관리자 전용
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						// 그 외 모든 요청은 인증 필요
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** auth 도메인 로그인에서 사용할 수 있도록 노출 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
