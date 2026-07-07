package com.dongnemarket.chat.dto;

import com.dongnemarket.member.entity.Member;

/** 채팅 상대·판매자 표시용 회원 요약. 이메일 등 민감정보 없이 닉네임만 노출한다. */
public class ChatMemberSummary {

    private final Long memberId;
    private final String nickname;

    private ChatMemberSummary(Long memberId, String nickname) {
        this.memberId = memberId;
        this.nickname = nickname;
    }

    public static ChatMemberSummary of(Member member) {
        return new ChatMemberSummary(member.getId(), member.getDisplayNickname());
    }

    public Long getMemberId() { return memberId; }
    public String getNickname() { return nickname; }
}
