package com.dongnemarket.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 메시지 전송 요청. Jackson 역직렬화를 위해 public 생성자를 둔다. */
public class ChatMessageCreateRequest {

    @NotBlank
    @Size(max = 1000)
    private String content;

    public ChatMessageCreateRequest() {}

    public ChatMessageCreateRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
