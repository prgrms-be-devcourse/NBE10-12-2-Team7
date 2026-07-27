package com.dongnemarket.mobile.ui.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoom
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.chat.component.ChatRoomListItem
import com.dongnemarket.mobile.ui.chat.component.PollingEffect
import com.dongnemarket.mobile.ui.component.EmptyView
import com.dongnemarket.mobile.ui.component.ErrorView
import com.dongnemarket.mobile.ui.component.LoadingView
import com.dongnemarket.mobile.ui.component.MarketOnBottomBar
import com.dongnemarket.mobile.ui.component.MarketOnTab
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.math.BigDecimal

/**
 * 채팅 목록 화면(상태 연결 버전).
 *
 * 화면은 `NavController` 를 받지 않는다. "어디로 갈지"는 이 화면의 관심사가 아니고
 * **어떤 일이 일어났는지만** 람다로 알린다([onRoomClick]·[onBackClick]).
 * 그래서 이 화면은 내비게이션 없이도 단독으로 띄우고 테스트할 수 있다.
 *
 * @param onRoomClick 방 한 줄을 눌렀다. 인자는 `roomId`.
 * @param onBackClick 뒤로(상단 화살표) 또는 하단 탭의 '홈' 을 눌렀다.
 *   채팅 목록은 홈에서 들어오는 화면이라 두 동작의 목적지가 같아서 하나로 합쳤다.
 */
@Composable
fun ChatListScreen(
    onRoomClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // 화면이 보이는 동안만 목록을 다시 받아 온다(자세한 이유는 PollingEffect 주석).
    PollingEffect(
        onStart = viewModel::startPolling,
        onStop = viewModel::stopPolling,
    )

    ChatListScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRoomClick = onRoomClick,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * 채팅 목록 화면의 **stateless 본체**. ViewModel 을 모르고 받은 값만 그린다.
 *
 * 이렇게 분리하면 `@Preview` 로 로딩·에러·빈 목록·정상 목록을 각각 눈으로 검수할 수 있고,
 * Compose 테스트에서 Hilt 없이 상태를 직접 넣어 볼 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreenContent(
    uiState: ChatListUiState,
    isRefreshing: Boolean,
    onRoomClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("채팅") },
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
            MarketOnBottomBar(
                selected = MarketOnTab.CHAT,
                onSelect = { tab ->
                    when (tab) {
                        MarketOnTab.HOME -> onBackClick()
                        // 이미 채팅 화면이므로 같은 곳으로 다시 이동하지 않는다.
                        MarketOnTab.CHAT -> Unit
                        // 찜·내정보는 enabled = false 라 호출되지 않지만,
                        // 탭이 늘어나도 컴파일이 깨지지 않게 else 를 남긴다.
                        else -> Unit
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when (uiState) {
                ChatListUiState.Loading ->
                    LoadingView(modifier = Modifier.fillMaxSize())

                is ChatListUiState.Error ->
                    // message 는 AppError.userMessage 다 — 백엔드 ErrorCode 는 화면에 찍지 않는다.
                    ErrorView(
                        message = uiState.message,
                        modifier = Modifier.fillMaxSize(),
                    ) { onRetry() }

                is ChatListUiState.Success ->
                    if (uiState.rooms.isEmpty()) {
                        // 서버가 정상적으로 "방이 없다"고 답한 것이다 → 에러가 아니라 빈 상태.
                        EmptyView(
                            message = "아직 채팅이 없어요.\n마음에 드는 상품에서 판매자에게 말을 걸어 보세요.",
                            modifier = Modifier.fillMaxSize(),
                            icon = Icons.Outlined.ChatBubbleOutline,
                            actionLabel = "상품 둘러보기",
                            onAction = onBackClick,
                        )
                    } else {
                        ChatRoomList(
                            rooms = uiState.rooms,
                            onRoomClick = onRoomClick,
                        )
                    }
            }
        }
    }
}

/**
 * 방 목록.
 *
 * 무한스크롤이 없는 이유: `GET /api/chat-rooms` 는 **페이징 없이 전량**을 준다(계약 §4-2).
 * 커서도 총 건수도 없으므로 붙일 수 있는 것이 없고, 대신 당겨서 새로고침 + 폴링으로 최신을 유지한다.
 *
 * `key = roomId` 를 주는 이유: 폴링으로 목록 순서가 바뀔 때(마지막 메시지 시각 DESC)
 * Compose 가 같은 방을 같은 항목으로 알아보고 스크롤 위치·애니메이션을 유지한다.
 */
@Composable
private fun ChatRoomList(
    rooms: List<ChatRoom>,
    onRoomClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_list"),
    ) {
        itemsIndexed(
            items = rooms,
            key = { _, room -> room.roomId },
        ) { index, room ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            ChatRoomListItem(
                room = room,
                onClick = { onRoomClick(room.roomId) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview — 사용자가 미리보기로 검수하는 4가지 상태
// ---------------------------------------------------------------------------

private fun previewRoom(
    roomId: Long,
    nickname: String,
    title: String,
    unread: Long,
    lastContent: String?,
    status: TradeStatus = TradeStatus.ON_SALE,
    price: String = "800000.00",
) = ChatRoom(
    roomId = roomId,
    opponentId = roomId + 100L,
    opponentNickname = nickname,
    productId = roomId + 10L,
    productTitle = title,
    productPrice = BigDecimal(price),
    productTradeStatus = status,
    productThumbnailUrl = null,
    createdAt = "2026-07-26T09:00:00",
    lastMessage = lastContent?.let {
        ChatMessage(
            messageId = roomId * 10,
            senderId = roomId + 100L,
            content = it,
            createdAt = "2026-07-26T17:40:00.123",
        )
    },
    unreadCount = unread,
)

@Preview(name = "채팅 목록 · 정상", showBackground = true, heightDp = 760)
@Composable
private fun ChatListSuccessPreview() {
    MarketOnTheme {
        ChatListScreenContent(
            uiState = ChatListUiState.Success(
                rooms = listOf(
                    previewRoom(1L, "당근이", "아이패드 프로 11인치 셀룰러 256GB", 3L, "오늘 저녁 강남역에서 직거래 가능하신가요?"),
                    previewRoom(2L, "몽실이", "무선 청소기 (거의 새것)", 0L, "네 그때 뵐게요!", TradeStatus.RESERVED, "120000.00"),
                    previewRoom(3L, "탈퇴한 사용자", "책상 나눔합니다", 0L, null, TradeStatus.COMPLETED, "0.00"),
                ),
            ),
            isRefreshing = false,
            onRoomClick = {},
            onBackClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(name = "채팅 목록 · 비어 있음", showBackground = true, heightDp = 760)
@Composable
private fun ChatListEmptyPreview() {
    MarketOnTheme {
        ChatListScreenContent(
            uiState = ChatListUiState.Success(rooms = emptyList()),
            isRefreshing = false,
            onRoomClick = {},
            onBackClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(name = "채팅 목록 · 로딩", showBackground = true, heightDp = 760)
@Composable
private fun ChatListLoadingPreview() {
    MarketOnTheme {
        ChatListScreenContent(
            uiState = ChatListUiState.Loading,
            isRefreshing = false,
            onRoomClick = {},
            onBackClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(name = "채팅 목록 · 에러", showBackground = true, heightDp = 760)
@Composable
private fun ChatListErrorPreview() {
    MarketOnTheme {
        ChatListScreenContent(
            uiState = ChatListUiState.Error(message = "네트워크 연결을 확인해 주세요."),
            isRefreshing = false,
            onRoomClick = {},
            onBackClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
