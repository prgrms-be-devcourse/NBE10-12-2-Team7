package com.dongnemarket.mobile.ui.productdetail

import com.dongnemarket.mobile.domain.model.ProductDetail

/**
 * 상품 상세 화면이 가질 수 있는 상태 전부.
 *
 * `sealed interface` 로 두는 이유: 화면이 `when (state)` 를 쓸 때 **컴파일러가 빠진 분기를 잡아 준다**.
 * "로딩 플래그 + 데이터 + 에러 문자열" 을 각각 nullable 로 들고 다니면
 * "로딩인데 데이터도 있고 에러도 있는" 있을 수 없는 조합이 만들어진다.
 *
 * 화면은 이 세 가지 중 하나만 그리면 되고, ViewModel 이 상태 전이를 책임진다.
 */
sealed interface ProductDetailUiState {

    /** 첫 진입(또는 재시도) 로딩 중. */
    data object Loading : ProductDetailUiState

    /**
     * 상세 조회 성공. 화면이 그릴 값이 전부 여기에 있다.
     *
     * @param product 서버 상세 응답. **작성 시각 필드가 없다**(계약 §7-10) → 화면에도 시간을 찍지 않는다.
     * @param isFavorite 내가 이 상품을 찜했는지. ⚠ 상세 응답에 이 정보가 **없어서**
     *   `FavoriteRepository.favoriteProductIds`(앱 전역 캐시)에서 조합한 값이다(계약 §7-2).
     * @param favoriteCount 찜 개수. `product.favoriteCount` 로 시작해 토글마다 **로컬로 ±1** 한다 —
     *   찜 등록/취소 응답에 갱신된 개수가 오지 않고, 다시 맞추려고 상세를 재조회하면
     *   그 GET 이 조회수를 +1 해 버린다(계약 §7-11).
     * @param isChatCreating '채팅하기' 를 눌러 방 생성 요청이 진행 중인지(버튼 중복 탭 방지 + 스피너).
     * @param message 스낵바에 한 번 띄우고 버리는 일회성 메시지. 표시 후 [ProductDetailViewModel.consumeMessage] 로 지운다.
     * @param categoryName 카테고리 이름. 상세 응답은 `categoryId` 숫자만 주므로 카테고리 목록에서 찾아 채운다.
     *   못 찾으면 `null` → 화면에서 그 칸을 아예 그리지 않는다(서버에 카테고리가 추가돼도 앱은 그냥 지나간다).
     * @param isMyProduct 내가 판매자인지(내 `memberId` == `product.sellerId`).
     *   판매자는 자기 상품에 채팅방을 만들 수 없으므로(400 `CANNOT_CHAT_WITH_SELF`)
     *   **실패를 유발하지 않고 버튼을 미리 비활성**으로 두는 데 쓴다.
     *   비로그인·토큰만료로 내 정보를 못 받았으면 `false` 다(판정 불가 → 버튼은 살려 두고 서버 응답에 맡긴다).
     */
    data class Success(
        val product: ProductDetail,
        val isFavorite: Boolean,
        val favoriteCount: Int,
        val isChatCreating: Boolean = false,
        val message: String? = null,
        val categoryName: String? = null,
        val isMyProduct: Boolean = false,
    ) : ProductDetailUiState

    /**
     * 상세 조회 실패. [message] 는 항상 `AppError.userMessage`(사용자에게 보여도 되는 한국어)이고,
     * 백엔드 `error` 코드 문자열은 절대 들어오지 않는다.
     *
     * 여기 오는 것이 '장애' 만은 아니다 — 삭제·거래완료·판매자 탈퇴 상품은 **정상적으로** 404 다(계약 §7-20).
     */
    data class Error(val message: String) : ProductDetailUiState
}
