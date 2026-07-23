package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.OAuthProvider;

import java.time.Duration;
import java.util.Optional;

/**
 * OAuth state(+PKCE code_verifier, 브라우저 귀속값, OIDC nonce) 저장소 추상화.
 * <p>{@code test} → {@link InMemoryOAuthStateRepository}
 * <p>그 외(dev/prod) → {@link RedisOAuthStateRepository}({@code auth:oauth:state:{state}} Hash +
 * {@code auth:oauth:bcid:{browserCorrelationHash}:states} ZSET, Lua로 원자 처리)
 */
public interface OAuthStateRepository {

	/**
	 * state를 발급한다. 같은 브라우저(browserCorrelationHash)에 이미 {@code maxPendingPerBrowser}개
	 * 이상의 만료 전 state가 있으면 발급을 거부한다(false 반환) — "발급 시 만료 항목 제거 → 개수 확인 →
	 * state 저장 → 브라우저별 인덱스 추가"가 하나의 원자 연산으로 처리된다.
	 */
	boolean issue(String state, OAuthAuthorizationState value, Duration ttl, int maxPendingPerBrowser);

	/**
	 * state를 검증하고 즉시 소비(삭제)한다. {@code provider}와 {@code browserCorrelationHash}가
	 * 저장된 값과 일치할 때만 원자적으로 삭제 후 반환한다 — 검증에 실패한 요청이 정상 state를
	 * 소모하는 일이 없어야 한다. 존재하지 않거나 만료됐거나 불일치하면 {@link Optional#empty()}.
	 * 소비 시 브라우저별 인덱스(ZSET)에서도 함께 제거된다.
	 */
	Optional<OAuthAuthorizationState> consume(String state, OAuthProvider provider, String browserCorrelationHash);
}
