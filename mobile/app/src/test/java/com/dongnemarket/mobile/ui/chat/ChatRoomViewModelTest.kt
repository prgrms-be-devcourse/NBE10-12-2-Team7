package com.dongnemarket.mobile.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatMessagePage
import com.dongnemarket.mobile.domain.model.ChatRoomHeader
import com.dongnemarket.mobile.domain.model.Member
import com.dongnemarket.mobile.domain.model.MemberRole
import com.dongnemarket.mobile.domain.model.MemberStatus
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.domain.repository.ChatRepository
import com.dongnemarket.mobile.domain.repository.MemberRepository
import com.dongnemarket.mobile.ui.navigation.MarketOnRoutes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.math.BigDecimal

/**
 * [ChatRoomViewModel] 명세.
 *
 * 이 화면이 감당하는 서버 제약이 로직의 이유 전부다.
 *  - 실시간 수신 경로가 없다 → **3초 폴링**
 *  - 증분 조회 파라미터가 없다 → 폴링은 매번 **첫 페이지 전체**를 다시 받는다
 *    → `messageId` 로 걸러 **새 것만 append** 하지 않으면 같은 말풍선이 계속 쌓인다
 *  - 로그인 응답에 memberId 가 없다 → `getMyProfile()` 이 없으면 말풍선 좌/우를 못 정한다
 *
 * ## 왜 StandardTestDispatcher 인가
 * 폴링 2회차 동작(중복 제거·실패 방어)이 이 파일의 핵심이라
 * "3초 뒤"를 손으로 밀어야 한다(`runCurrent()`, `advanceTimeBy(3_001)`).
 * `UnconfinedTestDispatcher` 는 `launch` 를 즉시 끝까지 달리게 해 회차를 구분할 수 없다.
 * `runTest` 는 `Dispatchers.Main` 이 TestDispatcher 면 그 스케줄러를 공유하므로,
 * 룰이 갈아 끼운 디스패처와 테스트 본문의 가상 시계는 하나다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestRule = MainDispatcherRule()

    private val chatRepository = mockk<ChatRepository>()
    private val memberRepository = mockk<MemberRepository>()

    // ------------------------------------------------------------------
    // 더미 데이터 — 실제 응답 모양 그대로
    // ------------------------------------------------------------------

    /** 로그인한 나. memberId 7 이 말풍선 좌/우 판정의 유일한 기준이다. */
    private val 나 = Member(
        memberId = 7L,
        email = "me@dongne.market",
        nickname = "나",
        role = MemberRole.USER,
        status = MemberStatus.ACTIVE,
        createdAt = "2026-07-01T10:00:00",
    )

    private val 헤더 = ChatRoomHeader(
        roomId = ROOM_ID,
        opponentId = 99L,
        opponentNickname = "동네주민",
        productId = 10L,
        productTitle = "삼천리 하이브리드 자전거",
        productPrice = BigDecimal("120000.00"),
        productTradeStatus = TradeStatus.ON_SALE,
        productThumbnailUrl = null,
    )

    /** 상대(99)가 09시에 보낸 첫 메시지 */
    private val 메시지10 = ChatMessage(10L, 99L, "안녕하세요, 자전거 아직 있나요?", "2026-07-26T09:00:00")

    /** 내가(7) 09시 1분에 보낸 답 */
    private val 메시지11 = ChatMessage(11L, 7L, "네 아직 있습니다", "2026-07-26T09:01:00")

    /** 상대(99)가 폴링 2회차에 새로 보낸 메시지 */
    private val 메시지12 = ChatMessage(12L, 99L, "오늘 저녁에 볼 수 있을까요?", "2026-07-26T09:02:00")

    /**
     * 부수 호출의 기본 응답.
     *
     * ⚠ MockK 는 stub 하지 않은 호출에 예외를 던지므로, ViewModel 이 `init` 에서 부르는
     * 세 가지(내 정보·헤더·읽음처리)는 늘 답이 있어야 한다.
     * **각 시나리오의 주인공이 되는 호출은 테스트 본문에서 다시 stub 해 눈에 보이게 한다.**
     */
    @Before
    fun 기본_응답을_깔아둔다() {
        coEvery { memberRepository.getMyProfile() } returns Result.success(나)
        coEvery { chatRepository.getRoomHeader(ROOM_ID) } returns Result.success(헤더)
        coEvery { chatRepository.markAsRead(ROOM_ID) } returns Result.success(Unit)
    }

    /** 라우트 인자(`chatRoom/{roomId}`)를 통해 방 번호가 들어오는 실제 배선을 그대로 재현한다. */
    private fun 채팅방을_연다() = ChatRoomViewModel(
        chatRepository = chatRepository,
        memberRepository = memberRepository,
        savedStateHandle = SavedStateHandle(mapOf(MarketOnRoutes.ARG_ROOM_ID to ROOM_ID)),
    )

    /** 폴링이 매번 다시 받아 오는 "최신 첫 페이지" 응답. 오래된 것 → 최신 순으로 온다. */
    private fun 첫페이지(vararg messages: ChatMessage) = Result.success(
        ChatMessagePage(
            messages = messages.toList(),
            nextCursor = messages.firstOrNull()?.messageId,
            hasNext = false,
        ),
    )

    // ------------------------------------------------------------------
    // 1. 방 진입
    // ------------------------------------------------------------------

    @Test
    fun `방에 들어가면 상품과 상대 닉네임을 담은 헤더가 상태에 들어온다`() = runTest {
        // Given: 헤더 우회 조회가 성공한다
        coEvery { chatRepository.getRoomHeader(ROOM_ID) } returns Result.success(헤더)

        // When: 방에 들어간다
        val viewModel = 채팅방을_연다()
        runCurrent()

        // Then
        assertEquals(헤더, viewModel.uiState.value.header)
    }

    @Test
    fun `방에 들어가면 내 memberId 를 받아 말풍선 좌우 판정 기준을 확보한다`() = runTest {
        // Given: 로그인 응답에는 memberId 가 없어 이 호출이 유일한 출처다
        coEvery { memberRepository.getMyProfile() } returns Result.success(나)

        // When
        val viewModel = 채팅방을_연다()
        runCurrent()

        // Then
        assertEquals(7L, viewModel.uiState.value.myMemberId)
    }

    @Test
    fun `방에 들어가면 안읽음 배지를 지우려고 읽음 처리를 호출한다`() = runTest {
        // Given / When: 아직 폴링은 시작하지 않은 '진입 직후' 시점
        채팅방을_연다()
        runCurrent()

        // Then: 진입 1회
        coVerify(exactly = 1) { chatRepository.markAsRead(ROOM_ID) }
    }

    @Test
    fun `첫 페이지 메시지는 오래된 것에서 최신 순으로 상태에 담긴다`() = runTest {
        // Given: 서버는 id DESC 로 주지만 Data 계층이 뒤집어 오름차순으로 넘겨준다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns
            첫페이지(메시지10, 메시지11, 메시지12)
        val viewModel = 채팅방을_연다()

        // When: 폴링 첫 바퀴가 진입 로드를 겸한다
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: ViewModel 이 다시 뒤집지 않는다 — 마지막 원소가 가장 최근 메시지여야 한다
        assertEquals(
            listOf(10L, 11L, 12L),
            viewModel.uiState.value.messages.map { it.messageId },
        )
    }

    @Test
    fun `내 memberId 와 첫 페이지가 모두 도착해야 첫 진입 로딩이 끝난다`() = runTest {
        // Given
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When: 내 정보만 도착하고 메시지는 아직 (폴링 시작 전)
        runCurrent()

        // Then: 좌/우가 정해지지 않은 말풍선을 한 프레임도 그리지 않도록 계속 로딩이다
        assertTrue(viewModel.uiState.value.isLoading)

        // When: 첫 페이지까지 도착하면
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 로딩이 끝난다
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ------------------------------------------------------------------
    // 2. 폴링 — 증분 API 가 없어서 매번 첫 페이지를 통째로 다시 받는다
    // ------------------------------------------------------------------

    @Test
    fun `폴링 2회차에 이미 가진 메시지는 중복으로 쌓이지 않고 새 메시지만 뒤에 붙는다`() = runTest {
        // Given: 2회차 응답에도 1회차와 같은 메시지 10·11 이 그대로 다시 들어 있다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returnsMany listOf(
            첫페이지(메시지10, 메시지11),
            첫페이지(메시지10, 메시지11, 메시지12),
        )
        val viewModel = 채팅방을_연다()

        // When: 0초(1회차) → 3초(2회차)
        viewModel.startPolling()
        runCurrent()
        advanceTimeBy(3_001)
        viewModel.stopPolling()

        // Then: 10·11 이 두 번 쌓이지 않고 12 만 늘어난다
        assertEquals(
            listOf(10L, 11L, 12L),
            viewModel.uiState.value.messages.map { it.messageId },
        )
    }

    @Test
    fun `폴링이 실패해도 이미 받아 둔 메시지는 화면에서 사라지지 않는다`() = runTest {
        // Given: 1회차는 성공, 2회차에 엘리베이터에 들어가 네트워크가 끊긴다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returnsMany listOf(
            첫페이지(메시지10, 메시지11),
            Result.failure(AppError.Network()),
        )
        val viewModel = 채팅방을_연다()

        // When: 0초(성공) → 3초(실패)
        viewModel.startPolling()
        runCurrent()
        advanceTimeBy(3_001)
        viewModel.stopPolling()

        // Then: 보고 있던 대화가 그대로 남아 있다(이 화면의 핵심 방어)
        assertEquals(
            listOf(메시지10, 메시지11),
            viewModel.uiState.value.messages,
        )
    }

    @Test
    fun `메시지를 아직 못 받은 상태에서 조회에 실패하면 사용자에게 원인을 알려 준다`() = runTest {
        // Given: 첫 조회부터 실패한다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns
            Result.failure(AppError.Network())
        val viewModel = 채팅방을_연다()

        // When
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 화면을 에러로 덮는 대신 스낵바 문구만 담는다
        assertEquals("네트워크 연결을 확인해 주세요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `상대가 보낸 새 메시지를 화면에 올리면 읽음 지점을 한 번 더 전진시킨다`() = runTest {
        // Given: 첫 페이지에 상대(99)가 보낸 메시지가 있다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When: 진입 + 첫 페이지 수신
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 진입 시 1회 + 상대 메시지를 그린 뒤 1회 = 2회
        coVerify(exactly = 2) { chatRepository.markAsRead(ROOM_ID) }
    }

    // ------------------------------------------------------------------
    // 3. 전송 — 쓴 문장을 잃는 것이 가장 나쁘다
    // ------------------------------------------------------------------

    @Test
    fun `전송에 성공하면 입력창이 비워진다`() = runTest {
        // Given: 대화가 열려 있고 사용자가 문장을 입력했다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        coEvery { chatRepository.sendMessage(ROOM_ID, "네 아직 있습니다") } returns
            Result.success(메시지11)
        val viewModel = 채팅방을_연다()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()
        viewModel.onInputChange("네 아직 있습니다")

        // When
        viewModel.onSendClick()
        runCurrent()

        // Then
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `전송에 성공하면 서버가 돌려준 메시지가 대화 끝에 붙는다`() = runTest {
        // Given
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        coEvery { chatRepository.sendMessage(ROOM_ID, "네 아직 있습니다") } returns
            Result.success(메시지11)
        val viewModel = 채팅방을_연다()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()
        viewModel.onInputChange("네 아직 있습니다")

        // When
        viewModel.onSendClick()
        runCurrent()

        // Then: 재조회 없이 응답 메시지를 그대로 이어 붙인다
        assertEquals(listOf(메시지10, 메시지11), viewModel.uiState.value.messages)
    }

    @Test
    fun `전송에 실패해도 입력창의 문장은 그대로 남는다`() = runTest {
        // Given: 사용자가 긴 문장을 써 두었는데 전송이 실패한다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        coEvery { chatRepository.sendMessage(ROOM_ID, any()) } returns
            Result.failure(AppError.Network())
        val viewModel = 채팅방을_연다()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()
        viewModel.onInputChange("혹시 3만원에 안 될까요? 오늘 저녁에 바로 갈 수 있어요")

        // When
        viewModel.onSendClick()
        runCurrent()

        // Then: 다시 타이핑하게 만들지 않는다
        assertEquals(
            "혹시 3만원에 안 될까요? 오늘 저녁에 바로 갈 수 있어요",
            viewModel.uiState.value.input,
        )
    }

    @Test
    fun `전송에 실패하면 전송 중 표시가 풀려 다시 시도할 수 있다`() = runTest {
        // Given
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        coEvery { chatRepository.sendMessage(ROOM_ID, any()) } returns
            Result.failure(AppError.Network())
        val viewModel = 채팅방을_연다()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()
        viewModel.onInputChange("다시 보낼게요")

        // When
        viewModel.onSendClick()
        runCurrent()

        // Then: 입력창이 잠긴 채 굳지 않는다
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun `상대가 탈퇴해 전송이 거절되면 이후 전송 버튼이 잠긴다`() = runTest {
        // Given: 읽기는 200 이지만 전송만 400 CHAT_PARTNER_WITHDRAWN 으로 막힌다
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        coEvery { chatRepository.sendMessage(ROOM_ID, any()) } returns Result.failure(
            AppError.Api(
                status = 400,
                code = "CHAT_PARTNER_WITHDRAWN",
                message = "상대방이 탈퇴하여 메시지를 보낼 수 없습니다.",
            ),
        )
        val viewModel = 채팅방을_연다()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()
        viewModel.onInputChange("안녕하세요")

        // When
        viewModel.onSendClick()
        runCurrent()

        // Then: 닉네임 문자열이 아니라 에러 코드로 판정해 입력을 잠근다
        assertFalse(viewModel.uiState.value.canSend)
    }

    // ------------------------------------------------------------------
    // 4. 내 정보 조회 실패 — 좌/우 판정 기준이 없는 상태
    // ------------------------------------------------------------------

    @Test
    fun `내 정보 조회에 실패하면 myMemberId 가 null 로 남는다`() = runTest {
        // Given: 세션이 끊겨 GET api members me 가 401 이다
        coEvery { memberRepository.getMyProfile() } returns Result.failure(AppError.Unauthorized())
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 좌/우를 확정할 수 없으므로 기준값을 비워 둔다(임의로 0 을 채우지 않는다)
        assertNull(viewModel.uiState.value.myMemberId)
    }

    @Test
    fun `내 정보 조회에 실패하면 그 사유가 스낵바 문구로 올라온다`() = runTest {
        // Given
        coEvery { memberRepository.getMyProfile() } returns Result.failure(AppError.Unauthorized())
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then
        assertEquals("다시 로그인해 주세요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `내 정보 조회에 실패해도 첫 진입 로딩은 끝난다`() = runTest {
        // Given
        coEvery { memberRepository.getMyProfile() } returns Result.failure(AppError.Unauthorized())
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 로딩 스피너가 영원히 도는 화면에 갇히지 않는다
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ------------------------------------------------------------------
    // 5. 폴링 수명
    // ------------------------------------------------------------------

    @Test
    fun `stopPolling 이후에는 시간이 더 흘러도 메시지를 다시 조회하지 않는다`() = runTest {
        // Given
        coEvery { chatRepository.getMessages(ROOM_ID, null, 30) } returns 첫페이지(메시지10)
        val viewModel = 채팅방을_연다()

        // When: 0초·3초 두 바퀴를 돈 뒤 화면이 가려져 폴링을 멈춘다
        viewModel.startPolling()
        runCurrent()
        advanceTimeBy(3_001)
        viewModel.stopPolling()

        // 화면이 가려진 채 30초가 더 흐른다(살아 있었다면 10번은 더 돌 시간이다)
        advanceTimeBy(30_000)

        // Then: 조회는 멈추기 전의 2번뿐이다
        coVerify(exactly = 2) { chatRepository.getMessages(ROOM_ID, null, 30) }
    }

    private companion object {
        const val ROOM_ID = 1L
    }

    /**
     * `viewModelScope` 가 쓰는 [Dispatchers.Main] 을 테스트 디스패처로 갈아 끼운다.
     * 안 하면 안드로이드 메인 루퍼가 없는 JVM 테스트에서 즉시 예외가 난다.
     *
     * 이 테스트 클래스 **안에** private 중첩 클래스로 둔 이유: 같은 패키지의 다른 테스트 파일도
     * 같은 이름의 룰을 각자 갖는데, 최상위(top-level)로 두면 JVM 클래스 이름이 겹친다.
     */
    private class MainDispatcherRule(
        private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {

        override fun starting(description: Description) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
