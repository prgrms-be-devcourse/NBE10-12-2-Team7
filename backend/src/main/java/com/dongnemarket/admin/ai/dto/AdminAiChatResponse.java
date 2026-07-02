package com.dongnemarket.admin.ai.dto;

/**
 * AI 어시스턴트 답변 응답.
 * <pre>{ "answer": "현재 전체 회원은 12명이며..." }</pre>
 */
public class AdminAiChatResponse {

    private final String answer;

    private AdminAiChatResponse(String answer) {
        this.answer = answer;
    }

    public static AdminAiChatResponse of(String answer) {
        return new AdminAiChatResponse(answer);
    }

    public String getAnswer() {
        return answer;
    }
}
