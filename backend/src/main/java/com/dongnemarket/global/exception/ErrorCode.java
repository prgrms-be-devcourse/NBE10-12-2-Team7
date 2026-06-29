package com.dongnemarket.global.exception;

/**
 * 전역 ErrorCode.
 * <p>각 팀원은 <b>자기 담당 도메인 주석 영역에만</b> ErrorCode 를 추가한다.
 * COMMON 영역은 팀장만 수정한다. (00-ai-common-rules.md §4)
 */
public enum ErrorCode {

	// ===== COMMON ERROR (팀장만 수정) =====
	INTERNAL_SERVER_ERROR(500, "COMMON_001", "서버 내부 오류가 발생했습니다."),
	INVALID_INPUT_VALUE(400, "COMMON_002", "잘못된 입력값입니다."),
	UNAUTHORIZED(401, "COMMON_003", "인증이 필요합니다."),
	FORBIDDEN(403, "COMMON_004", "접근 권한이 없습니다."),

	// ===== AUTH ERROR (김대연) =====
	DUPLICATE_EMAIL(409, "AUTH_001", "이미 사용 중인 이메일입니다."),
	DUPLICATE_NICKNAME(409, "AUTH_002", "이미 사용 중인 닉네임입니다."),
	INVALID_PASSWORD(401, "AUTH_003", "비밀번호가 일치하지 않습니다."),
	INVALID_TOKEN(401, "AUTH_004", "유효하지 않은 토큰입니다."),

	// ===== MEMBER ERROR (김대연) =====
	MEMBER_NOT_FOUND(404, "MEMBER_001", "회원을 찾을 수 없습니다."),
	DELETED_MEMBER(400, "MEMBER_002", "탈퇴한 회원입니다."),
	SUSPENDED_MEMBER(403, "MEMBER_003", "정지된 회원입니다."),

	// ===== PRODUCT ERROR (한상민) =====
	PRODUCT_NOT_FOUND(404, "PRODUCT_001", "상품을 찾을 수 없습니다."),
	PRODUCT_OWNER_ONLY(403, "PRODUCT_002", "상품 작성자만 처리할 수 있습니다."),
	HIDDEN_PRODUCT(403, "PRODUCT_003", "숨김 처리된 상품입니다."),
	DELETED_PRODUCT(404, "PRODUCT_004", "삭제된 상품입니다."),
	INVALID_PRODUCT_TITLE(400, "PRODUCT_005", "상품 제목은 필수입니다."),
	INVALID_PRODUCT_PRICE(400, "PRODUCT_006", "상품 가격은 0원 이상이어야 합니다."),
	CANNOT_UPDATE_COMPLETED_PRODUCT(400, "PRODUCT_007", "거래완료된 상품은 수정할 수 없습니다."),

	// ===== CATEGORY ERROR (한상민) =====
	CATEGORY_NOT_FOUND(404, "CATEGORY_001", "카테고리를 찾을 수 없습니다."),

	// ===== TRADE ERROR (한상민) =====
	INVALID_TRADE_STATUS(400, "TRADE_001", "잘못된 거래 상태입니다."),
	CANNOT_CHANGE_COMPLETED_PRODUCT(400, "TRADE_002", "거래완료된 상품은 상태를 변경할 수 없습니다."),

	// ===== SEARCH ERROR (한상민) =====
	INVALID_SEARCH_CONDITION(400, "SEARCH_001", "잘못된 검색 조건입니다."),

	// ===== FAVORITE ERROR (권건우) =====
	FAVORITE_ALREADY_EXISTS(409, "FAVORITE_001", "이미 관심 등록한 상품입니다."),
	FAVORITE_NOT_FOUND(404, "FAVORITE_002", "관심 상품을 찾을 수 없습니다."),

	// ===== COMMENT ERROR (권건우) =====
	COMMENT_NOT_FOUND(404, "COMMENT_001", "댓글을 찾을 수 없습니다."),
	COMMENT_OWNER_ONLY(403, "COMMENT_002", "댓글 작성자만 처리할 수 있습니다."),

	// ===== REPORT ERROR (서유진) =====
	REPORT_NOT_FOUND(404, "REPORT_001", "신고 내역을 찾을 수 없습니다."),
	CANNOT_REPORT_OWN_PRODUCT(400, "REPORT_002", "본인이 등록한 상품은 신고할 수 없습니다."),
	CANNOT_REPORT_SELF(400, "REPORT_003", "본인 계정은 신고할 수 없습니다."),
	INVALID_REPORT_TARGET(400, "REPORT_004", "잘못된 신고 대상입니다."),
	DUPLICATE_REPORT(409, "REPORT_005", "이미 신고한 대상입니다."),

	// ===== ADMIN ERROR (팀장) =====
	ADMIN_ONLY(403, "ADMIN_001", "관리자만 접근할 수 있습니다."),
	INVALID_MEMBER_STATUS(400, "ADMIN_002", "잘못된 회원 상태 값입니다."),
	INVALID_REPORT_STATUS(400, "ADMIN_003", "잘못된 신고 상태 값입니다.");

	private final int status;
	private final String code;
	private final String message;

	ErrorCode(int status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
