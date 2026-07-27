package com.dongnemarket.escrow.entity;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EscrowTest {

    private Escrow inEscrow() {
        // 상태 전이 로직은 product/buyer/seller를 쓰지 않으므로 연관은 null로 격리한다.
        return Escrow.create(null, null, null, BigDecimal.valueOf(15_000));
    }

    // ===== confirm =====

    @Test
    @DisplayName("confirm(): IN_ESCROW 거래를 확정하면 DONE으로 전이되고 closedAt이 기록된다")
    void confirm_fromInEscrow_success() {
        Escrow escrow = inEscrow();

        escrow.confirm();

        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.DONE);
        assertThat(escrow.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("confirm(): 이미 DONE인 거래를 재확정하면 ESCROW_NOT_IN_ESCROW 예외")
    void confirm_whenAlreadyDone_throws() {
        Escrow escrow = inEscrow();
        escrow.confirm();

        assertThatThrownBy(escrow::confirm)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ESCROW_NOT_IN_ESCROW);
    }

    @Test
    @DisplayName("confirm(): CANCELED 거래를 확정하면 ESCROW_NOT_IN_ESCROW 예외")
    void confirm_whenCanceled_throws() {
        Escrow escrow = inEscrow();
        escrow.cancel();

        assertThatThrownBy(escrow::confirm)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ESCROW_NOT_IN_ESCROW);
    }

    // ===== cancel =====

    @Test
    @DisplayName("cancel(): IN_ESCROW 거래를 취소하면 CANCELED로 전이되고 closedAt이 기록된다")
    void cancel_fromInEscrow_success() {
        Escrow escrow = inEscrow();

        escrow.cancel();

        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.CANCELED);
        assertThat(escrow.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel(): 이미 DONE인 거래를 취소하면 ESCROW_NOT_CANCELABLE 예외")
    void cancel_whenAlreadyDone_throws() {
        Escrow escrow = inEscrow();
        escrow.confirm();

        assertThatThrownBy(escrow::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ESCROW_NOT_CANCELABLE);
    }

    @Test
    @DisplayName("cancel(): 이미 CANCELED인 거래를 재취소하면 ESCROW_NOT_CANCELABLE 예외")
    void cancel_whenCanceled_throws() {
        Escrow escrow = inEscrow();
        escrow.cancel();

        assertThatThrownBy(escrow::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ESCROW_NOT_CANCELABLE);
    }
}
