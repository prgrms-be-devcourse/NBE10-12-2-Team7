package com.dongnemarket.mobile.domain.model

/**
 * 동네(지역) 하나. 동네 선택 화면의 목록 항목에 대응한다.
 *
 * ### ⚠ 통신에 쓰는 값은 [regionId] 가 아니라 [name] 이다
 * 백엔드는 지역을 **이름 문자열로 식별**한다(계약 §7-18).
 *  - 상품 목록/검색 필터: `?regions=서울 강남구&regions=서울 마포구` ← [name] 원문, **최대 2개**
 *  - 내 동네 설정(`PUT /api/members/me/locations`): `{"regions":["서울 강남구"]}` ← [name] 원문
 *
 * 서버는 `existsByName` 으로 **문자열 완전 비교**를 하므로 [regionId] 를 보내면 400 이고,
 * 공백을 다듬거나 사용자 자유입력을 그대로 보내면 원인 불명 400 이 난다.
 * → [name] 은 **서버가 준 원문 그대로** 보관·전송한다.
 * [regionId] 는 목록의 안정적인 key(LazyColumn `key = { it.regionId }`) 용도로만 쓴다.
 *
 * @param regionId 지역 PK. 화면 표시·통신에는 쓰지 않고 리스트 key 로만 쓴다.
 * @param name `"서울 강남구"` 처럼 "시도 시군구"가 공백 하나로 붙은 한 문자열.
 */
data class Region(
    val regionId: Long,
    val name: String,
) {

    /**
     * 시/도 이름. 동네 선택 화면에서 섹션 헤더로 묶을 때(`groupBy { it.sido }`) 쓴다.
     *
     * 첫 공백 앞을 자른다. 229건 중 **`"세종"` 만 공백이 없어서**(계약 §2-6)
     * `split(" ")[1]` 류로 접근하면 그 한 건에서 터진다.
     * `substringBefore` 는 구분자가 없으면 원본을 그대로 돌려주므로 `"세종"` → `"세종"` 이 되어
     * 자기 자신이 그룹명이 된다. 예외 처리를 따로 두지 않아도 되는 이유가 이것이다.
     */
    val sido: String
        get() = name.substringBefore(' ')
}
