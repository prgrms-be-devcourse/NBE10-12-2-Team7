package com.dongnemarket.mobile.ui.home

import com.dongnemarket.mobile.domain.model.Category
import com.dongnemarket.mobile.domain.model.Product

/**
 * 홈(상품목록) 화면이 그릴 수 있는 상태 3가지.
 *
 * `sealed interface` 로 묶는 이유: 화면은 `when (state)` 하나로 분기하고 컴파일러가
 * "빠뜨린 상태" 를 잡아 준다. Spring 의 응답 DTO 처럼 필드를 다 담은 하나의 클래스를 쓰면
 * "로딩 중인데 products 는 뭘 넣지" 같은 무의미한 조합이 생긴다.
 *
 * ⚠️ 이 화면에는 **상품 등록 버튼(FAB)·시간 표기·숫자 통계가 없다.** 각각 등록 화면 미구현(Phase 1),
 * 상품 응답에 시각 필드 없음(계약 §7-10), 통계 API 없음(§8-1) 때문이다 — 없는 걸 그리면 거짓이 된다.
 */
sealed interface HomeUiState {

    /** 첫 진입 로딩(동네·카테고리·상품을 아직 아무것도 모르는 상태). */
    data object Loading : HomeUiState

    /**
     * 상품 목록을 보여 줄 수 있는 상태.
     *
     * **동네·카테고리 조회가 실패해도 이 상태로 온다.** 부가 정보(헤더 동네 이름, 칩)가
     * 없을 뿐이고 목록 본문은 보여 주는 것이 맞다 — 상품 조회 실패만 [Error] 가 된다.
     *
     * @property products 현재 필터로 조회된 상품. 기본 목록은 커서 페이징으로 계속 뒤에 붙고,
     *   검색·카테고리 모드는 서버가 전량을 한 번에 주므로 더 붙지 않는다.
     * @property categories 카테고리 칩 원본. 서버가 주는 `id ASC` 순서를 그대로 그린다(재정렬 금지).
     *   조회 실패 시 `emptyList()` — 칩 영역만 비고 화면은 살아 있다.
     * @property region 헤더에 찍을 **대표 동네 이름**(`"서울 강남구"`). 미설정·조회 실패면 null 이고,
     *   그때는 히어로에서 동네 줄을 아예 생략한다(빈 칸을 남기지 않는다).
     * @property selectedCategoryId 선택된 카테고리 칩. **null = "전체"** 칩.
     * @property keyword **확정된** 검색어(빈 문자열 = 검색 안 함). 입력 중인 글자가 아니다 —
     *   타이핑마다 API 를 때리지 않기 위해 입력 초안은 검색바가 자기 안에 들고 있고,
     *   IME 검색 버튼을 누른 순간에만 이 값으로 승격된다.
     * @property isAppending 목록을 채우는 중(다음 페이지 append 또는 필터 변경 후 재조회).
     *   무한스크롤 중복 요청을 막는 가드이자 하단 스피너 표시 조건이다.
     * @property hasNext 다음 페이지 존재 여부. 검색·카테고리 모드에서는 **항상 false** 다
     *   (그 엔드포인트에 페이징이 없다 — 계약 §7-7).
     */
    data class Success(
        val products: List<Product>,
        val categories: List<Category>,
        val region: String?,
        val selectedCategoryId: Long?,
        val keyword: String,
        val isAppending: Boolean = false,
        val hasNext: Boolean = false,
    ) : HomeUiState {

        /**
         * 검색어나 카테고리 칩이 걸려 있는가.
         *
         * 이 값이 true 면 목록의 출처가 `GET /api/products/search`(전량 반환)로 바뀌므로
         * 무한스크롤이 없고, 결과 0건 문구도 "등록된 상품 없음" 이 아니라 "검색 결과 없음" 이어야 한다.
         */
        val isFiltering: Boolean
            get() = keyword.isNotBlank() || selectedCategoryId != null
    }

    /**
     * **상품 조회 실패**. 동네·카테고리 실패는 여기로 오지 않는다.
     *
     * @property message `AppError.userMessage` 그대로. 백엔드 `error` 코드(ErrorCode 상수명)를 담지 않는다.
     */
    data class Error(val message: String) : HomeUiState
}
