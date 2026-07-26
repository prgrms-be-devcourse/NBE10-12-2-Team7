package com.dongnemarket.mobile.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatMessagePage
import com.dongnemarket.mobile.domain.repository.ChatRepository
import com.dongnemarket.mobile.domain.repository.MemberRepository
import com.dongnemarket.mobile.ui.navigation.MarketOnRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 채팅방 폴링 주기. 계약 §4-6 권장 구간(3~5초) 중 가장 짧은 값 — 대화 중에는 반응이 빨라야 한다. */
private const val CHAT_ROOM_POLL_INTERVAL_MS = 3_000L

/** 서버 검증과 같은 상한(`@Size(max = 1000)`). 넘겨 보내면 400 이 되므로 입력 단계에서 자른다. */
private const val MAX_CONTENT_LENGTH = 1_000

/** 상대가 탈퇴해 전송이 막혔음을 알려 주는 백엔드 ErrorCode 이름. 이 코드로만 판정한다(닉네임 문자열 금지). */
private const val ERROR_PARTNER_WITHDRAWN = "CHAT_PARTNER_WITHDRAWN"

/**
 * 채팅방 화면의 상태 보관소.
 *
 * ## 이 화면이 다루는 서버 제약 4가지 (여기 로직 대부분의 이유다)
 * 1. **실시간 수신 경로가 없다**(WebSocket/SSE 0건) → 3초 폴링.
 * 2. **증분 조회 파라미터가 없다**(`since`/`after` 없음, 계약 §7-13) → 폴링은 매번 최신 첫 페이지를
 *    통째로 다시 받고, 로컬이 가진 **최대 messageId 보다 큰 것만** 골라 뒤에 붙인다.
 *    전체를 교체하면 리스트가 새로 만들어져 스크롤이 튀고 애니메이션이 깜빡인다.
 * 3. **방 단건 상세 API 가 없다**(계약 §7-16) → 헤더는 `getRoomHeader()` 우회 조회이고,
 *    실패해도 대화는 그대로 보여 준다.
 * 4. **내 memberId 를 로그인 응답이 주지 않는다** → `GET /api/members/me` 로 따로 받아야
 *    말풍선 좌/우를 정할 수 있다.
 *
 * ## roomId 를 화면 파라미터로 받지 않는 이유
 * 경로 인자는 [SavedStateHandle] 로 꺼낸다. Navigation 이 저장해 둔 값이라
 * 프로세스가 죽고 복원돼도 살아 있고, 화면 Composable 은 인자를 몰라도 된다.
 */
@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val memberRepository: MemberRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 라우트 `chatRoom/{roomId}` 의 경로 인자. 없으면 라우팅 배선 버그이므로 즉시 실패시킨다. */
    private val roomId: Long = checkNotNull(savedStateHandle.get<Long>(MarketOnRoutes.ARG_ROOM_ID)) {
        "roomId 경로 인자가 없다 — NavHost 의 argument 이름을 MarketOnRoutes.ARG_ROOM_ID 와 맞춰라."
    }

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    /**
     * '과거 더보기' 커서 상태. UiState 에 넣지 않은 이유는 **화면이 몰라도 되는 값**이기 때문이다
     * (화면은 "위에 닿았다"만 알려 주고, 더 받을 게 있는지·어디서부터 받을지는 여기서 판단한다).
     */
    private var oldestCursor: Long? = null
    private var hasOlderMessages = true
    private var isLoadingOlder = false

    /**
     * 커서를 첫 페이지에서 **한 번만** 초기화하기 위한 플래그.
     *
     * 폴링도 같은 `getMessages(cursor = null)` 을 부르는데, 그 응답의 `nextCursor` 로
     * [oldestCursor] 를 계속 덮어쓰면 사용자가 이미 과거로 30개를 올려 본 뒤에도
     * 커서가 "최신 페이지의 가장 오래된 id" 로 되돌아가 같은 페이지를 다시 받게 된다.
     */
    private var cursorInitialized = false

    // 첫 진입 로딩을 언제 끝낼지 판단하는 두 조건(둘 다 도착해야 화면을 그릴 수 있다).
    private var profileSettled = false
    private var firstPageSettled = false

    init {
        loadMyMemberId()
        loadHeader()
        // 방에 들어온 순간 읽음 처리(안읽음 배지 제거). 실패해도 화면을 막지 않는다 — 부가 기능이다.
        viewModelScope.launch { chatRepository.markAsRead(roomId) }
        // 메시지 첫 로드는 startPolling() 의 첫 바퀴가 담당한다(같은 코드로 진입 로드 + 주기 갱신).
    }

    /**
     * 폴링 시작. 이미 돌고 있으면 무시한다(리컴포지션으로 루프가 여러 개 생기는 것을 막는 가드).
     * 켜고 끄는 판단은 화면이 `repeatOnLifecycle(STARTED)` 로 한다 — 화면이 가려진 동안
     * 3초마다 요청을 보내면 배터리·데이터·서버를 아무 이득 없이 태운다.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchLatestMessages()
                delay(CHAT_ROOM_POLL_INTERVAL_MS)
            }
        }
    }

    /** 폴링 중지(화면이 가려질 때). `delay` 중이면 즉시 취소된다. */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /** 입력창 변경. 서버 상한(1000자)을 넘는 입력은 잘라서 받는다(붙여넣기를 통째로 버리지 않기 위해 take). */
    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value.take(MAX_CONTENT_LENGTH)) }
    }

    /**
     * 전송.
     *
     * **낙관적 UI(먼저 그려 놓기)를 쓰지 않는다.** 서버가 저장된 메시지를 그대로 돌려주므로
     * 응답을 받아 붙이면 되고(재조회 불필요), 가짜 messageId 로 먼저 그리면 폴링이 가져온
     * 진짜 메시지와 중복 제거를 할 수 없다(id 가 다르므로 같은 말풍선이 두 개가 된다).
     * 왕복은 보통 100~300ms 이고 그동안 입력창이 잠기므로 체감 문제도 없다.
     *
     * 실패해도 [ChatRoomUiState.input] 을 지우지 않는다 — 쓴 문장을 잃는 것이 가장 나쁘다.
     */
    fun onSendClick() {
        val state = _uiState.value
        val content = state.input.trim()
        if (content.isEmpty() || state.isSending || state.isPartnerWithdrawn) return

        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            chatRepository.sendMessage(roomId, content)
                .onSuccess { message ->
                    _uiState.update { current ->
                        // 폴링이 한발 먼저 같은 메시지를 가져왔을 수 있으므로 messageId 로 중복 확인.
                        val alreadyThere = current.messages.any { it.messageId == message.messageId }
                        current.copy(
                            input = "",
                            isSending = false,
                            messages = if (alreadyThere) current.messages else current.messages + message,
                        )
                    }
                }
                .onFailure { error ->
                    val withdrawn = (error as? AppError.Api)?.code == ERROR_PARTNER_WITHDRAWN
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.toUserMessage(),
                            // 한 번 탈퇴로 확인되면 되돌리지 않는다(읽기는 계속 되지만 전송은 영구 차단).
                            isPartnerWithdrawn = it.isPartnerWithdrawn || withdrawn,
                        )
                    }
                }
        }
    }

    /**
     * 과거 메시지 더보기(리스트 최상단에 닿았을 때).
     *
     * 커서는 **과거 방향 전용**이다(서버 조건이 `id < cursor`). 받은 페이지는 이미
     * 오래된 것 → 최신 순으로 정렬돼 있으므로 **앞쪽에 붙이면** 시간 순서가 유지된다.
     *
     * 실패는 조용히 무시한다 — 보고 있는 대화를 에러로 덮지 않는다는 원칙이 여기도 적용된다.
     */
    fun loadOlderMessages() {
        if (isLoadingOlder || !hasOlderMessages) return
        val cursor = oldestCursor ?: return

        isLoadingOlder = true
        viewModelScope.launch {
            chatRepository.getMessages(roomId, cursor = cursor)
                .onSuccess { page ->
                    oldestCursor = page.nextCursor
                    hasOlderMessages = page.hasNext
                    prependOlderMessages(page.messages)
                }
            isLoadingOlder = false
        }
    }

    /** 에러 화면·빈 화면의 '다시 시도'. 실패한 조각만 다시 부른다. */
    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        firstPageSettled = false

        if (_uiState.value.myMemberId == null) {
            profileSettled = false
            loadMyMemberId()
        }
        if (_uiState.value.header == null) {
            loadHeader()
        }
        viewModelScope.launch { fetchLatestMessages() }
    }

    /** 스낵바로 한 번 보여 준 에러를 지운다(같은 메시지가 화면 회전마다 다시 뜨지 않게). */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ------------------------------------------------------------------
    // 내부 구현
    // ------------------------------------------------------------------

    /**
     * 내 memberId 확보. 이것이 없으면 말풍선 좌/우를 정할 수 없어 화면을 그리지 못한다.
     * 로그인 응답에 `accessToken` 한 필드밖에 없어서 이 호출이 유일한 출처다(계약 §1-2).
     */
    private fun loadMyMemberId() {
        viewModelScope.launch {
            memberRepository.getMyProfile()
                .onSuccess { me -> _uiState.update { it.copy(myMemberId = me.memberId) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.toUserMessage()) } }
            profileSettled = true
            settleLoadingIfReady()
        }
    }

    /**
     * 헤더(상품 카드 + 상대 닉네임) 조회.
     *
     * ⚠ `getRoomHeader` 는 방 단건 API 가 없어서 **내 방 목록 전체를 받아 찾는 우회**다.
     * 그래서 실패 가능성이 대화 조회보다 높은데, 헤더가 없다고 대화를 못 보면 안 되므로
     * **실패를 errorMessage 로도 올리지 않고 조용히 넘긴다**(헤더 줄만 접힌다).
     */
    private fun loadHeader() {
        viewModelScope.launch {
            chatRepository.getRoomHeader(roomId)
                .onSuccess { header -> _uiState.update { it.copy(header = header) } }
        }
    }

    /**
     * 최신 첫 페이지 조회(폴링 + 진입 로드 겸용).
     *
     * 실패 처리 규칙: **화면을 에러로 덮지 않고** 스낵바용 [ChatRoomUiState.errorMessage] 에만 담는다.
     * 폴링이 한 번 실패했다고 보고 있던 대화가 사라지면 최악이다.
     * 단 메시지가 아직 하나도 없을 때는 사용자가 원인을 알아야 하므로 같은 문장을 스낵바로 띄운다.
     */
    private suspend fun fetchLatestMessages() {
        chatRepository.getMessages(roomId)
            .onSuccess { page ->
                initCursorOnce(page)
                appendNewMessages(page.messages)
            }
            .onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.toUserMessage()) }
            }
        firstPageSettled = true
        settleLoadingIfReady()
    }

    /** 첫 성공 응답에서만 과거 커서를 세운다(이유는 [cursorInitialized] 주석). */
    private fun initCursorOnce(page: ChatMessagePage) {
        if (cursorInitialized) return
        oldestCursor = page.nextCursor
        hasOlderMessages = page.hasNext
        cursorInitialized = true
    }

    /**
     * 새 메시지만 뒤에 붙인다.
     *
     * 증분 파라미터가 없어 폴링 응답에는 이미 가진 메시지 29개 + 새 메시지 1개가 섞여 온다.
     * `messageId` 는 삽입순 단조증가이므로 **로컬 최대 id 보다 큰 것**이 곧 새 메시지다.
     * (내용+시각 비교로 중복을 제거하면 같은 문장을 두 번 보낸 경우에 오작동한다.)
     */
    private fun appendNewMessages(incoming: List<ChatMessage>) {
        if (incoming.isEmpty()) return

        val current = _uiState.value.messages
        val maxKnownId = current.maxOfOrNull { it.messageId } ?: Long.MIN_VALUE
        val fresh = incoming.filter { it.messageId > maxKnownId }
        if (fresh.isEmpty()) return

        _uiState.update { it.copy(messages = it.messages + fresh) }

        // 상대가 보낸 새 메시지를 화면에 올렸으면 읽음 지점을 전진시킨다(내 메시지만 늘었으면 불필요).
        val myId = _uiState.value.myMemberId
        if (fresh.any { it.senderId != myId }) {
            viewModelScope.launch { chatRepository.markAsRead(roomId) }
        }
    }

    /** 과거 페이지를 앞쪽에 붙인다. 이미 가진 id 는 건너뛴다(경계에서 겹칠 수 있다). */
    private fun prependOlderMessages(older: List<ChatMessage>) {
        if (older.isEmpty()) return
        _uiState.update { current ->
            val knownIds = current.messages.mapTo(HashSet()) { it.messageId }
            val toAdd = older.filterNot { it.messageId in knownIds }
            if (toAdd.isEmpty()) current else current.copy(messages = toAdd + current.messages)
        }
    }

    /**
     * 첫 진입 로딩 종료 판정. **내 memberId 와 첫 메시지 페이지가 둘 다 도착해야** 끝난다.
     * 메시지만 먼저 도착해 로딩을 끝내면 좌/우가 정해지지 않은 말풍선이 한 프레임 그려진다.
     */
    private fun settleLoadingIfReady() {
        if (profileSettled && firstPageSettled) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 실패 원인 → 화면에 그대로 띄울 한국어 문장.
     *
     * `AppError.userMessage` 만 쓰고 백엔드 `error` 코드(`CHAT_ACCESS_DENIED` 등)는 노출하지 않는다.
     * 500 이 와도 "서버 장애" 로 단정하지 않는 이유: 커서·size 에 숫자가 아닌 값을 보내는
     * **내 버그도 500 으로 온다**(계약 §7-4). 문구는 AppError 가 중립적으로 갖고 있다.
     */
    private fun Throwable.toUserMessage(): String =
        (this as? AppError)?.userMessage ?: "요청을 처리할 수 없습니다."
}
