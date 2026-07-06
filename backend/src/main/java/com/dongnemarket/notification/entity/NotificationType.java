package com.dongnemarket.notification.entity;

/**
 * 알림 종류.
 * <p>{@code COMMENT}만 notifications 테이블에 저장한다. 채팅 알림은 저장하지 않고
 * 조회 시점에 안읽음 방에서 파생하므로(PR-N2) 저장 대상 enum에는 아직 포함하지 않는다.
 * 가격 변경 알림({@code PRICE_CHANGE})은 후속 협업 단위(PR-N3)에서 추가한다.
 */
public enum NotificationType {
    COMMENT
}
