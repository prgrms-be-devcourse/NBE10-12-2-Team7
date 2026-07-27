package com.dongnemarket.mobile.domain.repository

import com.dongnemarket.mobile.domain.model.Category

/**
 * 카테고리 조회 창구. **인터페이스를 Domain 이 소유한다**(의존성 역전).
 *
 * ViewModel 은 이 인터페이스만 알고, 구현체(`data.repository.CategoryRepositoryImpl`)는 Hilt 가 끼워 준다.
 * 덕분에 UI 는 Retrofit·DTO·JSON 을 전혀 모른 채 컴파일된다.
 *
 * 반환이 `Result<T>` 인 이유: **이 계층은 예외를 던지지 않는다.**
 * 실패는 `Result.failure(AppError)` 로 오고, 화면은 `AppError.userMessage` 만 읽어 노출한다.
 */
interface CategoryRepository {

    /**
     * 카테고리 전량을 조회한다. 홈 화면의 카테고리 칩 목록에 쓴다.
     *
     * - 순서는 **서버 순서(시드 등록순) 그대로**다. 정렬하지 말고 그대로 그려라.
     * - 실패 시 `Result.failure(AppError)`. 칩은 화면의 부가 요소이므로
     *   실패해도 화면 전체를 에러로 덮지 말고 칩 영역만 비우는 편이 자연스럽다.
     */
    suspend fun getCategories(): Result<List<Category>>
}
