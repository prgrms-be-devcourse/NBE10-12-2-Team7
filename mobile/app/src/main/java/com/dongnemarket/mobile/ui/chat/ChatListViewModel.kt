package com.dongnemarket.mobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 채팅 목록 폴링 주기. 계약 §4-6 권장 구간(5~10초)의 중간값.
 * 짧게 하면 서버 부담·배터리, 길게 하면 "새 메시지가 안 뜬다"는 체감이 생긴다.
 */
private const val CHAT_LIST_POLL_INTERVAL_MS = 7_000L

/**
 * 채팅 목록 화면의 상태 보관소.
 *
 * Spring 에 빗대면 화면 전용 서비스 계층이다 — Repository(= 서버 호출)를 조합해
 * 화면이 그리기만 하면 되는 [ChatListUiState] 로 가공한다.
 * `android.*` UI 타입을 참조하지 않기 때문에 화면 없이도 단위 테스트가 된다.
 *
 * ## 실시간이 없어서 폴링을 한다
 * 백엔드에 WebSocket/STOMP/SSE 가 **0건**(계약 §7-22)이라 새 메시지를 밀어 주는 경로가 없다.
 * 그래서 화면이 보이는 동안 [getRooms][ChatRepository.getRooms] 를
 * [CHAT_LIST_POLL_INTERVAL_MS] 마다 다시 호출한다.
 *
 * ## 폴링 루프를 왜 `init` 이 아니라 [startPolling] 으로 뺐는가
 * `init` 에서 `viewModelScope.launch { while(true) ... }` 를 띄우면 사용자가 홈 버튼을 눌러
 * 앱이 백그라운드로 가도 루프가 계속 돈다 — ViewModel 은 화면이 보이는지 모르기 때문이다.
 * 그러면 배터리·데이터·서버 요청을 아무 이득 없이 계속 태운다.
 * 그래서 **시작/중지 스위치만 공개**하고, 실제 켜고 끄기는 화면이
 * `repeatOnLifecycle(STARTED)` 로 판단한다(`ChatListScreen` 의 `PollingEffect`).
 * 최후 방어선으로 `viewModelScope` 는 `onCleared()` 에서 자동 취소되므로
 * 화면이 완전히 사라지면 [pollingJob] 도 함께 죽는다.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    /**
     * 당겨서 새로고침 중인지. [uiState] 와 분리한 이유는 [ChatListUiState] 주석 참고
     * (새로고침은 목록을 대체하지 않고 그 위에 겹치는 표시다).
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** 돌고 있는 폴링 루프. 중복 실행을 막고 [stopPolling] 에서 취소하기 위해 들고 있다. */
    private var pollingJob: Job? = null

    /**
     * 폴링 시작. **첫 조회도 이 루프의 첫 바퀴가 담당한다**(그래서 `init` 에 별도 로드가 없다) —
     * 진입 즉시 한 번 부르고 이후 주기적으로 반복하는 것이 같은 코드로 표현된다.
     *
     * 이미 돌고 있으면 아무것도 하지 않는다. 화면 회전처럼 `LaunchedEffect` 가
     * 다시 실행되는 상황에서 루프가 2개, 3개로 늘어나는 것을 막는 가드다.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchRooms()
                delay(CHAT_LIST_POLL_INTERVAL_MS)
            }
        }
    }

    /** 폴링 중지(화면이 가려질 때). `delay` 중이면 즉시 취소된다. */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /** 당겨서 새로고침. 폴링 주기를 기다리지 않고 지금 한 번 더 받아 온다. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchRooms()
            } finally {
                // 실패해도(또는 코루틴이 취소돼도) 인디케이터가 영원히 돌지 않게 한다.
                _isRefreshing.value = false
            }
        }
    }

    /** 에러 화면의 '다시 시도' 버튼. 로딩으로 되돌리고 한 번 더 조회한다. */
    fun retry() {
        viewModelScope.launch {
            _uiState.value = ChatListUiState.Loading
            fetchRooms()
        }
    }

    /**
     * 목록 1회 조회.
     *
     * ## 실패를 다루는 규칙 — 화면을 에러로 덮지 않는다
     * 폴링은 30초에 4~5번 실패할 수 있다(지하철·엘리베이터). 그때마다 목록을 지우고
     * 에러 화면으로 바꾸면 **보고 있던 채팅 목록이 사라졌다 나타났다** 하며 깜빡인다.
     * 그래서 **이미 [ChatListUiState.Success] 인 경우에는 실패를 조용히 삼키고** 다음 주기에 맡긴다.
     * 아직 아무것도 못 받은 상태(Loading)에서의 실패만 에러 화면으로 승격한다.
     */
    private suspend fun fetchRooms() {
        chatRepository.getRooms()
            .onSuccess { rooms -> _uiState.value = ChatListUiState.Success(rooms) }
            .onFailure { error ->
                if (_uiState.value !is ChatListUiState.Success) {
                    _uiState.value = ChatListUiState.Error(error.toUserMessage())
                }
            }
    }

    /**
     * 실패 원인 → 화면에 그대로 띄울 한국어 문장.
     *
     * `AppError.userMessage` 만 쓰고 백엔드 `error` 코드(`INVALID_TOKEN` 등)는 노출하지 않는다.
     * `AppError` 가 아닌 예외(계약 위반·버그)까지 흡수해 화면이 빈 문자열을 그리지 않게 한다.
     */
    private fun Throwable.toUserMessage(): String =
        (this as? AppError)?.userMessage ?: "요청을 처리할 수 없습니다."
}
