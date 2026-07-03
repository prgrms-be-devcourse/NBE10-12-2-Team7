package com.dongnemarket.chat.dto;

import java.util.List;

/**
 * 메시지 커서 페이지네이션 응답.
 * <p>messages는 최신순(id DESC). nextCursor는 다음(더 오래된) 페이지 요청에 그대로 넘길 커서(가장 오래된 메시지 id),
 * 더 없으면 null. hasNext=false면 과거 메시지가 더 없다.
 */
public class ChatMessagePageResponse {

    private final List<ChatMessageResponse> messages;
    private final Long nextCursor;
    private final boolean hasNext;

    private ChatMessagePageResponse(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
        this.messages = messages;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static ChatMessagePageResponse of(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
        return new ChatMessagePageResponse(messages, nextCursor, hasNext);
    }

    public List<ChatMessageResponse> getMessages() { return messages; }
    public Long getNextCursor() { return nextCursor; }
    public boolean isHasNext() { return hasNext; }
}
