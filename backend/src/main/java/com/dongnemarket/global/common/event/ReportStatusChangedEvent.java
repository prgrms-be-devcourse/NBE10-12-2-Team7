package com.dongnemarket.global.common.event;

import com.dongnemarket.report.entity.ReportStatus;

/**
 * 관리자가 신고 상태를 변경할 때 발행되는 도메인 이벤트.
 * <p>manner 도메인이 구독하여, 상태가 {@code COMPLETED}(정당한 신고 확정)로 바뀌면 피신고자의 매너온도를,
 * {@code REJECTED}(무고성 판정)로 바뀌면 신고자의 매너온도를 낮춘다. COMPLETED 확정이 누적되면
 * 계정 자동 정지 판단에도 쓰인다.
 * <p>Report 도메인은 이 이벤트를 발행만 할 뿐 누가 구독하는지 알지 못한다(단방향 의존, 결합도 최소화).
 *
 * @param reportId   상태가 바뀐 신고 id
 * @param newStatus  변경된 이후 상태
 */
public record ReportStatusChangedEvent(Long reportId, ReportStatus newStatus) {
}
