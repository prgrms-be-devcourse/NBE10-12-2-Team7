package com.dongnemarket.mobile.domain.model

/**
 * 커서 페이징 한 묶음. (백엔드 `ProductPageResponse` 대응)
 *
 * Spring 의 `Page<T>` 와 달리 **총 건수·전체 페이지 수가 없다.** 서버가 커서 방식만
 * 지원하기 때문이다. 즉 "3/10 페이지", "총 128개" 같은 UI 는 만들 수 없다.
 *
 * 무한스크롤 사용법:
 * 1. 첫 로드는 `cursor = null`
 * 2. 받은 [items] 를 화면 리스트 뒤에 붙인다
 * 3. [hasNext] 가 true 면 다음 로드에 [nextCursor] 를 그대로 넘긴다
 *
 * @property nextCursor 다음 요청에 넘길 커서(= 이 페이지 마지막 상품의 productId). 마지막 페이지면 null.
 * @property hasNext 다음 페이지 존재 여부. **[nextCursor] 가 null 인지로 판단하지 말고 이 값을 봐라.**
 */
data class ProductPage(
    val items: List<Product>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)
