package com.dongnemarket.mobile.ui.chat

import com.dongnemarket.mobile.domain.model.ChatRoom

/**
 * 채팅 목록 화면이 가질 수 있는 상태. 세 가지 중 **정확히 하나**다.
 *
 * `sealed interface` 로 묶는 이유: 화면에서 `when` 을 쓰면 컴파일러가 분기 누락을 잡아 주고,
 * "로딩 중인데 목록도 있고 에러 메시지도 있는" 모순된 조합이 **애초에 표현되지 않는다.**
 * (필드를 다 가진 한 덩어리 클래스로 두면 화면마다 `if (!loading && error == null)` 같은
 * 방어 코드가 늘어나고, 조합이 어긋난 화면이 하나씩 생긴다.)
 *
 * ## '새로고침 중'이 여기 없는 이유
 * 당겨서 새로고침은 [Success] 를 **대체하는 상태가 아니라 그 위에 겹치는 표시**다.
 * 여기에 넣으면 새로고침하는 동안 목록이 사라져야 해서, `ChatListViewModel` 이
 * `isRefreshing` 을 별도 `StateFlow` 로 들고 있다.
 */
sealed interface ChatListUiState {

    /** 첫 조회 중. 화면에 아직 보여 줄 것이 없다. */
    data object Loading : ChatListUiState

    /**
     * 조회 성공.
     *
     * ⚠ **빈 리스트도 성공이다.** 채팅방이 하나도 없는 것은 서버 에러가 아니므로
     * 화면은 `rooms.isEmpty()` 를 `ErrorView` 가 아니라 `EmptyView` 로 그려야 한다.
     *
     * 순서는 서버가 정한 것(마지막 메시지 시각 DESC → roomId DESC)을 그대로 담는다.
     * 서버가 이미 정렬을 끝냈으니 화면에서 재정렬하지 마라.
     */
    data class Success(val rooms: List<ChatRoom>) : ChatListUiState

    /**
     * 첫 조회 실패. [message] 는 반드시 `AppError.userMessage` 다
     * (백엔드 ErrorCode 문자열 `INVALID_TOKEN` 같은 것을 사용자에게 노출하지 않는다).
     *
     * ⚠ **폴링 실패는 이 상태로 오지 않는다.** 이미 보고 있는 목록을 지우면 안 되기 때문에
     * ViewModel 이 조용히 삼키고 다음 주기를 기다린다(자세한 이유는 ViewModel 주석).
     */
    data class Error(val message: String) : ChatListUiState
}
