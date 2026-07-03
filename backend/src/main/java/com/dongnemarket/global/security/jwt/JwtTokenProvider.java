package com.dongnemarket.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * JWT 발급/검증/파싱.
 * <p>auth 도메인은 로그인 성공 시 {@link #createAccessToken(Long, String)} 으로 토큰을 발급한다.
 * role 값은 {@code "ROLE_USER"} / {@code "ROLE_ADMIN"} 처럼 권한 prefix 를 포함해 전달한다
 * (SecurityConfig 의 {@code hasRole("ADMIN")} 매칭).
 */
@Component
public class JwtTokenProvider {

	private static final String CLAIM_TYPE = "type";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessTokenValidityMillis;
	private final long refreshTokenValidityMillis;

	public JwtTokenProvider(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
			@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityMillis = accessTokenValiditySeconds * 1000L;
		this.refreshTokenValidityMillis = refreshTokenValiditySeconds * 1000L;
	}

	/** 로그인 성공 시 호출: memberId(subject) + role 클레임으로 액세스 토큰 발급 */
	public String createAccessToken(Long memberId, String role) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + accessTokenValidityMillis);
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim("role", role)
				.claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	/** 로그인 성공 시 호출: memberId(subject)만 담아 리프레시 토큰 발급 */
	public String createRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + refreshTokenValidityMillis);
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	/** 토큰의 만료 시각(java.time). RefreshToken 저장 시 expiresAt 계산에 사용 */
	public LocalDateTime getExpiration(String token) {
		Date expiration = parse(token).getExpiration();
		return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
	}

	/** 토큰 유효성 검증 (서명/만료/형식) */
	public boolean validateToken(String token) {
		try {
			parse(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 토큰 → 인증 객체 (principal = memberId, authorities = [role]).
	 * Access Token({@code type=access})이 아니면 인증에 사용할 수 없다(Refresh Token 오용 방지).
	 */
	public Authentication getAuthentication(String token) {
		Claims claims = parse(token);
		if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new JwtException("Access Token이 아닙니다.");
		}
		Long memberId = Long.valueOf(claims.getSubject());
		String role = claims.get("role", String.class);
		Collection<GrantedAuthority> authorities = (role == null)
				? List.of()
				: List.of(new SimpleGrantedAuthority(role));
		return new UsernamePasswordAuthenticationToken(memberId, token, authorities);
	}

	/** 토큰에서 memberId 추출 */
	public Long getMemberId(String token) {
		return Long.valueOf(parse(token).getSubject());
	}

	/** Refresh Token({@code type=refresh})인지 여부 */
	public boolean isRefreshToken(String token) {
		return TOKEN_TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
	}

	private Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
