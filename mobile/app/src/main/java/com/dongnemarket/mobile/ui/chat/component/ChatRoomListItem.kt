package com.dongnemarket.mobile.ui.chat.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoom
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.component.NetworkImage
import com.dongnemarket.mobile.ui.component.RelativeTime
import com.dongnemarket.mobile.ui.component.StatusBadge
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.math.BigDecimal

/**
 * 채팅 목록의 한 줄.
 *
 * 담는 정보는 4가지다: 상품 썸네일 / 상대 닉네임 + 마지막 메시지 / 표시 시각 / 안읽음 배지.
 *
 * 화면 이동은 [onClick] 으로만 알린다(이 컴포넌트는 `roomId` 로 무엇을 할지 모른다) —
 * 그래야 목록 화면이 바뀌어도 이 줄은 그대로 재사용된다.
 */
@Composable
fun ChatRoomListItem(
    room: ChatRoom,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("chat_room_item")
            // clickable 뒤에 padding 을 두는 이유: 터치 영역이 여백까지 포함되어 누르기 쉬워진다.
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 데모/시드 데이터에는 상품 이미지가 없어 대부분 null 이다 → NetworkImage 가 플레이스홀더를 그린다.
        NetworkImage(
            url = room.productThumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.small,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.opponentNickname,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false: 닉네임이 짧으면 시각이 바로 옆에 붙고, 길면 잘린다.
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                // 상품에는 시간 필드가 없지만 채팅에는 있다 → 상대시간 표기가 가능한 화면이다.
                RelativeTime(raw = room.displayTimeRaw)
            }

            Text(
                // lastMessage 는 null 일 수 있다 — 방을 만들고 아직 아무 말도 안 한 상태(정상).
                text = room.lastMessage?.content ?: "아직 대화가 없어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (room.lastMessage == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // 채팅방의 상품은 거래완료(COMPLETED)도 온다 — 홈 목록과 달리 서버가 필터하지 않는다.
                StatusBadge(room.productTradeStatus, room.productPrice)
                Text(
                    text = room.productTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        if (room.hasUnread) {
            UnreadBadge(count = room.unreadCount)
        }
    }
}

/**
 * 안읽음 개수 배지. 서버 `unreadCount` 는 **상대가 보낸** 안읽은 메시지 수다(내 메시지는 제외).
 * 방에 들어가 `markAsRead` 를 부르면 다음 폴링에서 0으로 내려와 배지가 사라진다.
 */
@Composable
private fun UnreadBadge(count: Long) {
    Badge(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    ) {
        // 3자리가 되면 배지가 줄 높이를 밀어낸다.
        Text(text = if (count > 99L) "99+" else count.toString())
    }
}

@Preview(name = "채팅 목록 한 줄", showBackground = true)
@Composable
private fun ChatRoomListItemPreview() {
    MarketOnTheme {
        Column {
            ChatRoomListItem(
                room = ChatRoom(
                    roomId = 1L,
                    opponentId = 22L,
                    opponentNickname = "당근이",
                    productId = 12L,
                    productTitle = "아이패드 프로 11인치 5세대 셀룰러",
                    productPrice = BigDecimal("800000.00"),
                    productTradeStatus = TradeStatus.ON_SALE,
                    productThumbnailUrl = null,
                    createdAt = "2026-07-26T09:00:00",
                    lastMessage = ChatMessage(
                        messageId = 90L,
                        senderId = 22L,
                        content = "혹시 오늘 저녁에 직거래 가능하신가요? 강남역 근처면 좋겠어요.",
                        createdAt = "2026-07-26T17:57:00.123",
                    ),
                    unreadCount = 3L,
                ),
                onClick = {},
            )
            ChatRoomListItem(
                room = ChatRoom(
                    roomId = 2L,
                    opponentId = 33L,
                    opponentNickname = "탈퇴한 사용자",
                    productId = 15L,
                    productTitle = "무선 청소기",
                    productPrice = BigDecimal("0.00"),
                    productTradeStatus = TradeStatus.COMPLETED,
                    productThumbnailUrl = null,
                    createdAt = "2026-07-25T11:00:00",
                    lastMessage = null,
                    unreadCount = 0L,
                ),
                onClick = {},
            )
        }
    }
}
