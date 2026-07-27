package com.dongnemarket.mobile.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoomHeader
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.chat.component.ChatInputBar
import com.dongnemarket.mobile.ui.chat.component.ChatMessageBubble
import com.dongnemarket.mobile.ui.chat.component.ChatProductHeader
import com.dongnemarket.mobile.ui.chat.component.PollingEffect
import com.dongnemarket.mobile.ui.component.EmptyView
import com.dongnemarket.mobile.ui.component.ErrorView
import com.dongnemarket.mobile.ui.component.LoadingView
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.math.BigDecimal

/**
 * 자동 스크롤 판정 여유 칸수.
 * "맨 아래 근처" 를 정확히 마지막 항목으로 잡으면 한 칸만 올려 봐도 자동 스크롤이 꺼져 불편하고,
 * 너무 넉넉하면 과거를 읽는 중에 끌려 내려간다.
 */
private const val NEAR_BOTTOM_THRESHOLD = 3

/** 이 인덱스 이하까지 올라오면 더 오래된 페이지를 불러온다. */
private const val LOAD_OLDER_TRIGGER_INDEX = 1

/**
 * 채팅방 화면(상태 연결 버전).
 *
 * `roomId` 를 파라미터로 받지 않는다 — 경로 인자는 ViewModel 이 `SavedStateHandle` 로 꺼낸다.
 * 화면은 "뒤로 가고 싶다" 만 [onBackClick] 으로 알린다.
 */
@Composable
fun ChatRoomScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면이 보이는 동안만 3초마다 새 메시지를 확인한다(자세한 이유는 PollingEffect 주석).
    PollingEffect(
        onStart = viewModel::startPolling,
        onStop = viewModel::stopPolling,
    )

    ChatRoomScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onInputChange = viewModel::onInputChange,
        onSendClick = viewModel::onSendClick,
        onLoadOlder = viewModel::loadOlderMessages,
        onRetry = viewModel::retry,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier,
    )
}

/**
 * 채팅방 화면의 **stateless 본체**.
 *
 * 화면 쪽이 책임지는 것 세 가지가 여기 모여 있다(ViewModel 은 리스트 위치를 모른다):
 *  1. 새 메시지가 오면 맨 아래로 스크롤 — 단 **사용자가 위로 올려 과거를 보는 중이면 하지 않는다.**
 *  2. 리스트 최상단에 닿으면 과거 더보기 요청.
 *  3. 에러는 스낵바로만 — 대화를 덮지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreenContent(
    uiState: ChatRoomUiState,
    onBackClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onLoadOlder: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    // 스마트 캐스트가 확실하게 걸리도록 지역 변수로 한 번 받는다(null 이면 좌우 판정 불가 상태).
    val myMemberId = uiState.myMemberId

    // (3) 폴링·전송 실패 안내. 한 번 띄운 뒤 지워서 화면 회전 때 다시 뜨지 않게 한다.
    LaunchedEffect(uiState.errorMessage, myMemberId) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        // 아직 내 정보를 못 받은 단계에서는 아래 ErrorView 가 같은 문장을 이미 보여 준다.
        if (myMemberId == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onErrorShown()
    }

    // (1) 새 메시지 도착·전송 성공 → 맨 아래로. key 를 '마지막 messageId' 로 두면
    //     내용이 그대로인 리컴포지션에서는 스크롤이 다시 일어나지 않는다.
    val lastMessageId = uiState.messages.lastOrNull()?.messageId
    LaunchedEffect(lastMessageId) {
        if (lastMessageId == null) return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        // 아직 아무것도 배치되지 않은 첫 진입(null)은 최신을 보여 주는 것이 맞다.
        val nearBottom = lastVisibleIndex == null ||
            lastVisibleIndex >= uiState.messages.size - NEAR_BOTTOM_THRESHOLD
        if (nearBottom) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // (2) 최상단 근처에 닿으면 과거 페이지 요청. derivedStateOf 로 감싸는 이유:
    //     firstVisibleItemIndex 는 스크롤 한 픽셀마다 바뀌므로 그대로 key 로 쓰면
    //     LaunchedEffect 가 초당 수십 번 재시작된다. 여기서는 true/false 로 바뀔 때만 반응한다.
    val atTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex <= LOAD_OLDER_TRIGGER_INDEX }
    }
    LaunchedEffect(atTop) {
        if (atTop) onLoadOlder()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // 헤더 조회가 실패했으면(우회 API 라 실패 가능) 이름 없이 "채팅" 으로 둔다.
                        text = uiState.header?.opponentNickname ?: "채팅",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            // 내 memberId 를 모르면 대화를 그리지 못하므로 입력창도 내린다.
            if (myMemberId != null) {
                ChatInputBar(
                    value = uiState.input,
                    onValueChange = onInputChange,
                    onSendClick = onSendClick,
                    enabled = !uiState.isSending && !uiState.isPartnerWithdrawn,
                    canSend = uiState.canSend,
                    isSending = uiState.isSending,
                    hint = if (uiState.isPartnerWithdrawn) {
                        "상대방이 탈퇴해 메시지를 보낼 수 없어요."
                    } else {
                        null
                    },
                    // 인셋은 소비되며 겹치지 않는다: 내비게이션 바를 먼저 피하고,
                    // 키보드가 올라오면 그만큼 '추가로' 더 밀린다.
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // 헤더는 실패해도 대화를 막지 않는다 — 있으면 붙이고 없으면 접는다.
            uiState.header?.let { header ->
                ChatProductHeader(header = header)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            when {
                uiState.isLoading ->
                    LoadingView(modifier = Modifier.fillMaxSize())

                // 좌/우를 확정할 수 없는 상태(내 정보 조회 실패). 잘못된 쪽에 그리지 않고 재시도를 권한다.
                myMemberId == null ->
                    ErrorView(
                        message = uiState.errorMessage ?: "내 정보를 불러오지 못했어요.",
                        modifier = Modifier.fillMaxSize(),
                    ) { onRetry() }

                // 대화가 0건인 것은 정상이다(방을 만들고 아직 말을 안 한 경우).
                // 조회가 실패해서 0건인 경우도 같은 화면인데, 그때 원인은 스낵바가 이미 알려 줬고
                // 여기 '다시 불러오기' 버튼이 재시도 수단이 된다.
                uiState.messages.isEmpty() ->
                    EmptyView(
                        message = "아직 대화가 없어요.\n먼저 인사를 보내 보세요.",
                        modifier = Modifier.fillMaxSize(),
                        icon = Icons.Outlined.ChatBubbleOutline,
                        actionLabel = "다시 불러오기",
                        onAction = onRetry,
                    )

                else ->
                    MessageList(
                        messages = uiState.messages,
                        myMemberId = myMemberId,
                        listState = listState,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

/**
 * 메시지 리스트.
 *
 * `reverseLayout` 을 **쓰지 않는다**: Data 계층이 서버의 `id DESC`(최신 먼저)를 이미 뒤집어
 * 오래된 것 → 최신 순으로 주기 때문에, 여기서 또 뒤집으면 대화가 거꾸로 보인다.
 * 즉 리스트의 마지막 항목이 가장 최근 메시지이고, 자동 스크롤 목표도 `lastIndex` 다.
 *
 * `key = messageId` 가 중요한 이유: 과거 더보기로 **앞쪽에 항목이 끼어들 때** 키가 있으면
 * Compose 가 "지금 보고 있던 항목" 을 그대로 붙잡아 화면이 튀지 않는다(키가 없으면 인덱스가
 * 밀리면서 갑자기 옛 대화로 점프한다).
 */
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    myMemberId: Long,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.testTag("chat_message_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = messages,
            key = { it.messageId },
        ) { message ->
            ChatMessageBubble(
                message = message,
                // 서버는 발신자 닉네임을 주지 않는다 — 내 memberId 와 비교하는 것이 유일한 판정법.
                isMine = message.senderId == myMemberId,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

private const val PREVIEW_MY_ID = 11L

private val previewHeader = ChatRoomHeader(
    roomId = 1L,
    opponentId = 22L,
    opponentNickname = "당근이",
    productId = 12L,
    productTitle = "아이패드 프로 11인치 셀룰러 256GB 팝니다",
    productPrice = BigDecimal("800000.00"),
    productTradeStatus = TradeStatus.ON_SALE,
    productThumbnailUrl = null,
)

private val previewMessages = listOf(
    ChatMessage(1L, 22L, "안녕하세요! 상품 아직 있나요?", "2026-07-26T15:20:10"),
    ChatMessage(2L, PREVIEW_MY_ID, "네 있습니다. 오늘 저녁에 강남역 근처에서 직거래 가능하세요?", "2026-07-26T15:24:05.7"),
    ChatMessage(3L, 22L, "좋아요! 7시쯤 괜찮으세요?", "2026-07-26T15:25:00.123456"),
    ChatMessage(4L, PREVIEW_MY_ID, "네 그때 뵙겠습니다", "2026-07-26T15:26:11"),
)

@Preview(name = "채팅방 · 대화 중", showBackground = true, heightDp = 760)
@Composable
private fun ChatRoomPreview() {
    MarketOnTheme {
        ChatRoomScreenContent(
            uiState = ChatRoomUiState(
                header = previewHeader,
                messages = previewMessages,
                myMemberId = PREVIEW_MY_ID,
                input = "",
                isLoading = false,
            ),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "채팅방 · 전송 중", showBackground = true, heightDp = 760)
@Composable
private fun ChatRoomSendingPreview() {
    MarketOnTheme {
        ChatRoomScreenContent(
            uiState = ChatRoomUiState(
                header = previewHeader,
                messages = previewMessages,
                myMemberId = PREVIEW_MY_ID,
                input = "혹시 케이스도 같이 주시나요?",
                isSending = true,
                isLoading = false,
            ),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "채팅방 · 상대 탈퇴 + 헤더 없음", showBackground = true, heightDp = 760)
@Composable
private fun ChatRoomWithdrawnPreview() {
    MarketOnTheme {
        ChatRoomScreenContent(
            // 헤더 조회(우회 API)가 실패한 경우: 제목이 "채팅" 이 되고 상품 줄이 접히지만
            // 대화는 그대로 보인다 — 헤더 때문에 대화를 못 보면 안 된다.
            uiState = ChatRoomUiState(
                header = null,
                messages = previewMessages,
                myMemberId = PREVIEW_MY_ID,
                isLoading = false,
                isPartnerWithdrawn = true,
            ),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "채팅방 · 대화 없음", showBackground = true, heightDp = 760)
@Composable
private fun ChatRoomEmptyPreview() {
    MarketOnTheme {
        ChatRoomScreenContent(
            uiState = ChatRoomUiState(
                header = previewHeader,
                messages = emptyList(),
                myMemberId = PREVIEW_MY_ID,
                isLoading = false,
            ),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "채팅방 · 로딩", showBackground = true, heightDp = 760)
@Composable
private fun ChatRoomLoadingPreview() {
    MarketOnTheme {
        ChatRoomScreenContent(
            uiState = ChatRoomUiState(isLoading = true),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}
