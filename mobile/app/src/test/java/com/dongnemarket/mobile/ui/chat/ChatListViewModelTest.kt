package com.dongnemarket.mobile.ui.chat

import app.cash.turbine.test
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoom
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.domain.repository.ChatRepository
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.math.BigDecimal

/**
 * [ChatListViewModel] 명세.
 *
 * 이 화면의 핵심은 두 가지다.
 *  1. 방 목록 조회 결과를 [ChatListUiState] 세 상태 중 하나로 정확히 옮기는 것
 *     (특히 **방 0개는 Error 가 아니라 빈 Success**).
 *  2. 실시간 경로가 없어서 도는 **7초 폴링을 화면이 가려지면 확실히 멈추는 것**.
 *     폴링이 새면 배터리·데이터·서버 요청이 아무 이득 없이 계속 탄다.
 *
 * ## 왜 StandardTestDispatcher 인가
 * 이 파일의 주인공이 폴링이라 **가상 시간을 손으로 밀어야** 한다
 * (`runCurrent()` = 지금 이 순간의 작업만, `advanceTimeBy(n)` = n밀리초 경과).
 * `UnconfinedTestDispatcher` 를 쓰면 `launch` 가 즉시 끝까지 달려 버려
 * "몇 초 시점에 몇 번 호출됐는가"를 관측할 수 없다.
 *
 * `runTest` 는 `Dispatchers.Main` 이 TestDispatcher 면 그 스케줄러를 그대로 쓴다.
 * 그래서 아래 룰이 갈아 끼운 디스패처와 테스트 본문의 가상 시계가 하나로 묶인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestRule = MainDispatcherRule()

    private val chatRepository = mockk<ChatRepository>()

    // ------------------------------------------------------------------
    // 더미 데이터 — 실제 서버 응답 모양 그대로
    // (가격은 GET 응답 스케일 "120000.00", 시각은 오프셋 없는 원문 문자열)
    // ------------------------------------------------------------------

    private val 자전거방 = ChatRoom(
        roomId = 1L,
        opponentId = 99L,
        opponentNickname = "동네주민",
        productId = 10L,
        productTitle = "삼천리 하이브리드 자전거",
        productPrice = BigDecimal("120000.00"),
        productTradeStatus = TradeStatus.ON_SALE,
        productThumbnailUrl = null,
        createdAt = "2026-07-26T13:45:30",
        lastMessage = ChatMessage(
            messageId = 501L,
            senderId = 99L,
            content = "혹시 오늘 저녁에 볼 수 있을까요?",
            createdAt = "2026-07-26T18:02:11",
        ),
        unreadCount = 2L,
    )

    private val 책상방 = ChatRoom(
        roomId = 2L,
        opponentId = 77L,
        opponentNickname = "이사가는사람",
        productId = 20L,
        productTitle = "이케아 책상",
        productPrice = BigDecimal("35000.00"),
        productTradeStatus = TradeStatus.RESERVED,
        productThumbnailUrl = "http://10.0.2.2:8080/files/desk.jpg",
        createdAt = "2026-07-25T09:10:00",
        lastMessage = null,
        unreadCount = 0L,
    )

    // ------------------------------------------------------------------
    // 1. 조회 결과 → 화면 상태 매핑
    // ------------------------------------------------------------------

    @Test
    fun `폴링을 시작하면 목록이 Loading 에서 방 2개를 담은 Success 로 바뀐다`() = runTest {
        // Given: 서버에 내가 참여 중인 방이 2개 있다
        coEvery { chatRepository.getRooms() } returns Result.success(listOf(자전거방, 책상방))
        val viewModel = ChatListViewModel(chatRepository)

        // When / Then: 진입 직후엔 Loading, 폴링 첫 바퀴가 돌면 Success 로 전이한다
        viewModel.uiState.test {
            assertEquals(ChatListUiState.Loading, awaitItem())

            viewModel.startPolling()

            assertEquals(ChatListUiState.Success(listOf(자전거방, 책상방)), awaitItem())
        }

        // 폴링 루프를 남긴 채 테스트를 끝내지 않는다(화면이 사라지면 화면이 끄는 것과 같은 정리).
        viewModel.stopPolling()
    }

    @Test
    fun `첫 조회에 실패하면 AppError 의 사용자 문구를 담은 Error 가 된다`() = runTest {
        // Given: 토큰이 만료돼 401 이 온다
        coEvery { chatRepository.getRooms() } returns Result.failure(AppError.Unauthorized())
        val viewModel = ChatListViewModel(chatRepository)

        // When: 폴링 첫 바퀴가 돈다
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 백엔드 ErrorCode 가 아니라 사람이 읽는 문장이 화면에 올라간다
        assertEquals(
            ChatListUiState.Error("다시 로그인해 주세요."),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `참여 중인 방이 하나도 없으면 Error 가 아니라 빈 목록 Success 다`() = runTest {
        // Given: 서버가 빈 배열을 200 으로 준다(방 0개는 실패가 아니다)
        coEvery { chatRepository.getRooms() } returns Result.success(emptyList())
        val viewModel = ChatListViewModel(chatRepository)

        // When
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 화면이 ErrorView 가 아니라 EmptyView 를 그릴 수 있어야 한다
        assertEquals(ChatListUiState.Success(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `이미 목록을 받은 뒤 폴링이 실패하면 보고 있던 목록이 그대로 남는다`() = runTest {
        // Given: 첫 바퀴는 성공하고, 두 바퀴째에 지하철에서 네트워크가 끊긴다
        coEvery { chatRepository.getRooms() } returnsMany listOf(
            Result.success(listOf(자전거방)),
            Result.failure(AppError.Network()),
        )
        val viewModel = ChatListViewModel(chatRepository)

        // When: 0초(성공) → 7초(실패)
        viewModel.startPolling()
        runCurrent()
        advanceTimeBy(7_001)
        viewModel.stopPolling()

        // Then: 목록이 Error 로 덮여 깜빡이지 않는다
        assertEquals(ChatListUiState.Success(listOf(자전거방)), viewModel.uiState.value)
    }

    // ------------------------------------------------------------------
    // 2. 폴링 수명 — 새면 배터리·데이터가 탄다
    // ------------------------------------------------------------------

    @Test
    fun `stopPolling 이후에는 시간이 더 흘러도 목록을 다시 조회하지 않는다`() = runTest {
        // Given
        coEvery { chatRepository.getRooms() } returns Result.success(listOf(자전거방))
        val viewModel = ChatListViewModel(chatRepository)

        // When: 0초·7초 두 바퀴를 돌고 화면이 가려져 폴링을 멈춘다
        viewModel.startPolling()
        runCurrent()
        advanceTimeBy(7_001)
        viewModel.stopPolling()

        // 화면이 가려진 채 30초가 더 흐른다(폴링이 살아 있었다면 4번은 더 돌 시간이다)
        advanceTimeBy(30_000)

        // Then: 조회는 멈추기 전의 2번뿐이다
        coVerify(exactly = 2) { chatRepository.getRooms() }
    }

    @Test
    fun `startPolling 을 두 번 불러도 폴링 루프는 하나만 돈다`() = runTest {
        // Given: 화면 회전 등으로 LaunchedEffect 가 두 번 실행된 상황
        coEvery { chatRepository.getRooms() } returns Result.success(listOf(자전거방))
        val viewModel = ChatListViewModel(chatRepository)

        // When
        viewModel.startPolling()
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        // Then: 루프가 2개로 늘어나 매 주기 요청이 2배가 되지 않는다
        coVerify(exactly = 1) { chatRepository.getRooms() }
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
