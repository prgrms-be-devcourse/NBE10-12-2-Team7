package com.dongnemarket.auth.client;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 제공자 호출 예외를 공통 규칙으로 매핑한다: 4xx는 인가 코드 관련 인증 실패, 그 외(5xx/네트워크/timeout/
 * 비정상 JSON/응답 크기 초과)는 모두 제공자 장애로 취급한다. 어느 경우든 제공자의 원문 응답 본문/에러
 * 메시지는 그대로 노출하지 않는다.
 */
final class OAuthClientErrorMapper {

	private OAuthClientErrorMapper() {
	}

	static <T> T call(Supplier<T> action) {
		try {
			return action.get();
		} catch (WebClientResponseException e) {
			if (e.getStatusCode().is4xxClientError()) {
				throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
			}
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		} catch (WebClientRequestException e) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		} catch (CodecException | DataBufferLimitException e) {
			// 200 응답인데 JSON이 깨졌거나(CodecException), 응답이 설정한 최대 크기를 넘은 경우
			// (DataBufferLimitException) — 둘 다 제공자가 정상적으로 응답하지 못한 것으로 취급한다.
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		} catch (RuntimeException e) {
			if (e.getCause() instanceof TimeoutException) {
				throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
			}
			throw e;
		}
	}
}
