package com.dongnemarket.mobile.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoomHeader
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** 화면이 붙인 testTag 들. 구현(ChatInputBar·ChatRoomScreen)과 같은 문자열이어야 한다. */
private const val TAG_MESSAGE_LIST = "chat_message_list"
private const val TAG_INPUT = "chat_input"
private const val TAG_SEND = "chat_send"

/** 내 회원 PK. 말풍선 좌/우 판정의 유일한 기준이다(`senderId == myMemberId` → 내 메시지). */
private const val MY_MEMBER_ID = 11L

/** 상대 회원 PK. */
private const val OPPONENT_ID = 22L

/**
 * 채팅방 화면(stateless 본체 [ChatRoomScreenContent])의 Compose UI 테스트.
 *
 * **Hilt·ViewModel 을 쓰지 않는다.** 채팅방은 상태 하나(`ChatRoomUiState`)와 람다 6개만 받도록
 * 설계돼 있어서, "이 상태를 주면 화면이 이렇게 보이고 이 조작이 이렇게 위로 전달된다" 만 검증한다.
 * 폴링(`PollingEffect`)은 연결 버전에만 있으므로 여기서는 돌지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class ChatRoomScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------
    // 테스트가 쓰는 대화. 오래된 것 → 최신 순이며, 마지막 원소가 가장 최근 메시지다.
    // ------------------------------------------------------------------
    private val 상대_인사 = ChatMessage(1L, OPPONENT_ID, "안녕하세요", "2026-07-26T15:20:10")
    private val 내_답장 = ChatMessage(2L, MY_MEMBER_ID, "반갑습니다", "2026-07-26T15:24:05.7")
    private val 상대_질문 = ChatMessage(3L, OPPONENT_ID, "직거래 되나요", "2026-07-26T15:25:00.123456")
    private val 내_확답 = ChatMessage(4L, MY_MEMBER_ID, "네 가능해요", "2026-07-26T15:26:11")
    private val 대화_네건 = listOf(상대_인사, 내_답장, 상대_질문, 내_확답)

    private val 상품헤더 = ChatRoomHeader(
        roomId = 1L,
        opponentId = OPPONENT_ID,
        opponentNickname = "당근이",
        productId = 12L,
        productTitle = "아이패드 프로 11인치 팝니다",
        productPrice = BigDecimal("800000.00"),
        productTradeStatus = TradeStatus.ON_SALE,
        productThumbnailUrl = null,
    )

    /**
     * 첫 로딩이 끝나고 내 memberId 도 확보된 '정상 대화 중' 상태.
     * 여기서 필드 하나씩만 바꿔 가며 각 시나리오를 만든다.
     */
    private fun 대화중(
        messages: List<ChatMessage> = 대화_네건,
        header: ChatRoomHeader? = 상품헤더,
        input: String = "",
        isSending: Boolean = false,
        isPartnerWithdrawn: Boolean = false,
    ) = ChatRoomUiState(
        header = header,
        messages = messages,
        myMemberId = MY_MEMBER_ID,
        input = input,
        isSending = isSending,
        isLoading = false,
        isPartnerWithdrawn = isPartnerWithdrawn,
    )

    /** 채팅방을 주어진 상태로 띄운다. 관찰하고 싶은 람다만 넘긴다. */
    private fun 채팅방을_띄운다(
        uiState: ChatRoomUiState,
        onInputChange: (String) -> Unit = {},
        onSendClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MarketOnTheme {
                ChatRoomScreenContent(
                    uiState = uiState,
                    onBackClick = {},
                    onInputChange = onInputChange,
                    onSendClick = onSendClick,
                    onLoadOlder = {},
                    onRetry = {},
                    onErrorShown = {},
                )
            }
        }
    }

    // ==================================================================
    // 1. 대화 목록
    // ==================================================================

    @Test
    fun `대화_네_건이_모두_화면에_보인다`() {
        // Given: 오래된 것 → 최신 순으로 정렬된 메시지 4건
        // When
        채팅방을_띄운다(대화중())

        // Then: 4건이 하나도 빠지지 않고 그려진다
        대화_네건.forEach { message ->
            composeRule.onNodeWithText(message.content).assertIsDisplayed()
        }
    }

    @Test
    fun `같은_메시지가_두_번_그려지지_않는다`() {
        // Given: 폴링이 매번 첫 페이지를 통째로 다시 받으므로 중복 렌더가 가장 흔한 사고다
        // When
        채팅방을_띄운다(대화중())

        // Then: 각 메시지 말풍선은 정확히 하나씩만 존재한다
        대화_네건.forEach { message ->
            composeRule.onAllNodesWithText(message.content).assertCountEquals(1)
        }
    }

    @Test
    fun `내_메시지는_오른쪽에_상대_메시지는_왼쪽에_그려진다`() {
        // Given: senderId 가 내 memberId 인 메시지와 상대 memberId 인 메시지가 한 건씩.
        //        서버는 발신자 닉네임을 주지 않으므로 좌/우가 유일한 구분 수단이다.
        채팅방을_띄운다(대화중(messages = listOf(상대_인사, 내_답장)))

        // When: 두 말풍선의 화면 위치를 잰다
        val 상대_왼쪽 = composeRule.onNodeWithText(상대_인사.content).getUnclippedBoundsInRoot().left
        val 내것_왼쪽 = composeRule.onNodeWithText(내_답장.content).getUnclippedBoundsInRoot().left

        // Then: 내 말풍선이 상대 말풍선보다 오른쪽에서 시작한다
        assertTrue(
            "내 메시지가 오른쪽으로 정렬되지 않았다 (상대=${상대_왼쪽}, 내것=${내것_왼쪽})",
            내것_왼쪽 > 상대_왼쪽,
        )
    }

    @Test
    fun `메시지가_한_건도_없으면_빈_상태_안내가_보인다`() {
        // Given: 방을 막 만들고 아직 아무 말도 하지 않은 정상 상태(에러가 아니다)
        // When
        채팅방을_띄운다(대화중(messages = emptyList()))

        // Then
        composeRule.onNodeWithText("아직 대화가 없어요", substring = true).assertIsDisplayed()
    }

    @Test
    fun `메시지가_한_건도_없으면_메시지_목록_자체가_그려지지_않는다`() {
        // Given: 빈 방
        // When
        채팅방을_띄운다(대화중(messages = emptyList()))

        // Then: 빈 리스트가 아니라 빈 상태 화면으로 대체된다
        composeRule.onNodeWithTag(TAG_MESSAGE_LIST).assertDoesNotExist()
    }

    // ==================================================================
    // 2. 상단 헤더 — 우회 조회라 실패해도 대화를 막지 않아야 한다
    // ==================================================================

    @Test
    fun `헤더가_있으면_상단에_상대_닉네임이_보인다`() {
        // Given: 방 목록에서 조합한 헤더가 있는 상태
        // When
        채팅방을_띄운다(대화중(header = 상품헤더))

        // Then
        composeRule.onNodeWithText("당근이").assertIsDisplayed()
    }

    @Test
    fun `헤더_조회가_실패해도_대화는_그대로_보인다`() {
        // Given: 방 단건 상세 API 가 없어 헤더는 우회 조회다 → 실패할 수 있다(header = null)
        // When
        채팅방을_띄운다(대화중(header = null))

        // Then: 헤더 줄만 접히고 대화는 살아 있다
        composeRule.onNodeWithText(내_확답.content).assertIsDisplayed()
    }

    @Test
    fun `헤더가_없으면_상단_제목이_채팅_으로_대체된다`() {
        // Given
        // When
        채팅방을_띄운다(대화중(header = null))

        // Then: 빈 제목이 아니라 중립 문구가 들어간다
        composeRule.onNodeWithText("채팅").assertIsDisplayed()
    }

    // ==================================================================
    // 3. 입력창 · 전송 버튼
    // ==================================================================

    @Test
    fun `입력창에_타이핑하면_onInputChange_로_입력한_글자가_전달된다`() {
        // Given: 입력이 비어 있는 채팅방. 입력값은 화면 밖(여기)에서 들고 있다 —
        //        ChatInputBar 가 stateless 라 되돌려 주지 않으면 글자가 화면에 남지 않는다.
        var 입력값 by mutableStateOf("")
        val 전달된값들 = mutableListOf<String>()
        composeRule.setContent {
            MarketOnTheme {
                ChatRoomScreenContent(
                    uiState = 대화중(input = 입력값),
                    onBackClick = {},
                    onInputChange = { 전달된값들 += it; 입력값 = it },
                    onSendClick = {},
                    onLoadOlder = {},
                    onRetry = {},
                    onErrorShown = {},
                )
            }
        }

        // When: 사용자가 문장을 친다
        composeRule.onNodeWithTag(TAG_INPUT).performTextInput("케이스도 주시나요")

        // Then: 친 문장이 그대로 위로 올라온다
        assertEquals("케이스도 주시나요", 전달된값들.lastOrNull())
    }

    @Test
    fun `전송_버튼을_누르면_onSendClick_이_호출된다`() {
        // Given: 보낼 문장이 입력돼 있어 전송이 가능한 상태
        var 전송횟수 = 0
        채팅방을_띄운다(
            uiState = 대화중(input = "네 좋아요"),
            onSendClick = { 전송횟수++ },
        )

        // When
        composeRule.onNodeWithTag(TAG_SEND).performClick()

        // Then
        assertEquals(1, 전송횟수)
    }

    @Test
    fun `입력이_비어_있으면_전송_버튼이_비활성이다`() {
        // Given: 서버 @NotBlank 에 걸리기 전에 클라이언트가 먼저 막는다(400 은 원인을 알려주지 않는다)
        // When
        채팅방을_띄운다(대화중(input = ""))

        // Then
        composeRule.onNodeWithTag(TAG_SEND).assertIsNotEnabled()
    }

    @Test
    fun `입력이_공백뿐이면_전송_버튼이_비활성이다`() {
        // Given: 스페이스만 친 경우도 서버에서는 @NotBlank 위반이다
        // When
        채팅방을_띄운다(대화중(input = "   "))

        // Then: 길이가 0이 아니어도 막힌다(isNotBlank 판정)
        composeRule.onNodeWithTag(TAG_SEND).assertIsNotEnabled()
    }

    @Test
    fun `입력에_글자가_있으면_전송_버튼이_활성이다`() {
        // Given
        // When
        채팅방을_띄운다(대화중(input = "네"))

        // Then
        composeRule.onNodeWithTag(TAG_SEND).assertIsEnabled()
    }

    @Test
    fun `전송_왕복_중에는_전송_버튼이_비활성이라_같은_문장이_두_번_가지_않는다`() {
        // Given: 문장이 채워져 있지만 이미 전송 요청이 진행 중이다
        // When
        채팅방을_띄운다(대화중(input = "네 좋아요", isSending = true))

        // Then
        composeRule.onNodeWithTag(TAG_SEND).assertIsNotEnabled()
    }

    @Test
    fun `전송_왕복_중에는_입력창이_disabled_가_된다`() {
        // Given: 현재 구현은 전송 버튼뿐 아니라 입력창까지 enabled=false 로 내린다.
        //        ⚠ 이 테스트는 '현재 동작을 고정'하는 것이지 이 동작이 옳다고 말하는 것이 아니다 —
        //        disabled 는 포커스와 키보드를 함께 내리므로 한 문장 보낼 때마다 키보드가 닫힌다(bugsFound 참고).
        //        중복 전송 방지는 canSend(전송 버튼 잠금)만으로 이미 충족된다.
        // When
        채팅방을_띄운다(대화중(input = "네 좋아요", isSending = true))

        // Then
        composeRule.onNodeWithTag(TAG_INPUT).assertIsNotEnabled()
    }

    // ==================================================================
    // 4. 상대 탈퇴 — 읽기는 되고 전송만 막힌다
    // ==================================================================

    @Test
    fun `상대가_탈퇴한_방이면_입력창이_비활성이다`() {
        // Given: 전송 시 CHAT_PARTNER_WITHDRAWN(400) 을 받아 확정된 상태
        // When
        채팅방을_띄운다(대화중(isPartnerWithdrawn = true))

        // Then
        composeRule.onNodeWithTag(TAG_INPUT).assertIsNotEnabled()
    }

    @Test
    fun `상대가_탈퇴한_방이면_이유를_알려주는_안내_문구가_보인다`() {
        // Given
        // When
        채팅방을_띄운다(대화중(isPartnerWithdrawn = true))

        // Then: 입력창이 왜 잠겼는지 화면이 설명한다
        composeRule.onNodeWithText("상대방이 탈퇴해 메시지를 보낼 수 없어요.").assertIsDisplayed()
    }

    @Test
    fun `상대가_탈퇴한_방이면_입력이_채워져_있어도_전송_버튼이_비활성이다`() {
        // Given: 탈퇴가 확인되기 전에 쓰던 문장이 입력창에 남아 있는 경우(입력은 지우지 않는 정책)
        // When
        채팅방을_띄운다(대화중(input = "혹시 아직 계신가요", isPartnerWithdrawn = true))

        // Then: 글자가 있어도 전송은 영구 차단이다
        composeRule.onNodeWithTag(TAG_SEND).assertIsNotEnabled()
    }

    @Test
    fun `상대가_탈퇴해도_지난_대화는_계속_읽을_수_있다`() {
        // Given: 탈퇴는 '전송만' 막는다는 것이 이 화면의 규칙이다
        // When
        채팅방을_띄운다(대화중(isPartnerWithdrawn = true))

        // Then
        composeRule.onNodeWithText(상대_인사.content).assertIsDisplayed()
    }
}
