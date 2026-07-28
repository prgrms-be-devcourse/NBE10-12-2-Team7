package com.dongnemarket.chat.dto;

import com.dongnemarket.member.entity.Member;

/** 채팅 상대·판매자 표시용 회원 요약. 이메일 등 민감정보 없이 닉네임만 노출한다. */
public class ChatMemberSummary {

    private final Long memberId;
    private final String nickname;
    /** 상대가 탈퇴(DELETED)했는지 여부. 프론트가 입력창 비활성화·안내 배너를 프로액티브하게 띄우는 신뢰 신호. */
    private final boolean withdrawn;

    private ChatMemberSummary(Long memberId, String nickname, boolean withdrawn) {
        this.memberId = memberId;
        this.nickname = nickname;
        this.withdrawn = withdrawn;
    }

    public static ChatMemberSummary of(Member member) {
        return new ChatMemberSummary(member.getId(), member.getDisplayNickname(), member.isWithdrawn());
    }

    public Long getMemberId() { return memberId; }
    public String getNickname() { return nickname; }
    public boolean isWithdrawn() { return withdrawn; }
}
