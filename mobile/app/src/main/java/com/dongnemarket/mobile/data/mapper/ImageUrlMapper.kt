package com.dongnemarket.mobile.data.mapper

import com.dongnemarket.mobile.BuildConfig

/**
 * 서버가 주는 이미지 경로를 Coil 이 바로 로드할 수 있는 **절대 URL** 로 바꾼다.
 *
 * 왜 필요한가: 서버는 `"/api/products/images/{uuid}.jpg"` 처럼 **상대 경로**만 준다.
 * presigned URL·CDN 도메인을 내려주는 코드가 백엔드에 없고, 파일 저장소가 S3 라도 마찬가지다
 * (서버가 바이트를 프록시한다). 이 값을 그대로 Coil 에 넘기면 로드가 실패한다.
 *
 * 그런데 서버는 이미지 URL 문자열을 검증 없이 저장하므로 **절대 URL 이 섞여 있을 수 있다**
 * (테스트 데이터에 `https://example.com/...` 가 존재한다) → `http` 로 시작하면 손대지 않는다.
 */

/**
 * @receiver 서버가 준 원본 값(상대 경로 / 절대 URL / null / 빈 문자열)
 * @param baseUrl 기본값은 빌드 타입별 `BuildConfig.BASE_URL`(끝에 `/` 가 붙어 있다)
 * @return 로드 가능한 절대 URL, 또는 이미지가 없으면 **null**
 *
 * null 을 그대로 돌려주는 것이 중요하다. 여기서 임의의 더미 이미지 URL 을 채우면
 * 화면은 '이미지 있음'으로 착각해 깨진 이미지를 그린다. 플레이스홀더는 UI 의 책임이다.
 * (시드/데모 데이터는 이미지가 아예 없어 대부분 null 이다.)
 */
fun String?.toAbsoluteImageUrl(baseUrl: String = BuildConfig.BASE_URL): String? {
    val raw = this?.trim()
    if (raw.isNullOrEmpty()) return null
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

    val host = baseUrl.trimEnd('/')
    return if (raw.startsWith("/")) "$host$raw" else "$host/$raw"
}

/** 이미지 목록용. 변환 실패(빈 문자열 등) 원소는 화면에 그릴 수 없으니 걸러 낸다. */
fun List<String>.toAbsoluteImageUrls(baseUrl: String = BuildConfig.BASE_URL): List<String> =
    mapNotNull { it.toAbsoluteImageUrl(baseUrl) }
