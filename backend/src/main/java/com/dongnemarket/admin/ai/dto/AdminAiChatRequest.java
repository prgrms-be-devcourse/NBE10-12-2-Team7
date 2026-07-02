package com.dongnemarket.admin.ai.dto;

/**
 * AI 어시스턴트 질문 요청.
 * <pre>{ "message": "대시보드 현황 알려줘" }</pre>
 */
public class AdminAiChatRequest {

    private String message;

    public AdminAiChatRequest() {
    }

    public AdminAiChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
