package com.dongnemarket.mobile.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 하단 탭 4종.
 *
 * [enabled] = false 인 탭(찜·내정보)은 **Phase 1에 화면 자체가 없다.**
 * 탭을 아예 지우지 않고 비활성으로 남긴 이유: 앱의 최종 정보구조를 사용자에게 미리 보여 주면서도,
 * 눌러도 아무 일이 안 나는 버튼(사용자는 앱이 고장 났다고 생각한다)을 만들지 않기 위해서다.
 * 화면이 생기면 여기 `enabled = true` 로 바꾸고 라우팅만 붙이면 된다.
 */
enum class MarketOnTab(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
) {
    HOME("홈", Icons.Outlined.Home, enabled = true),
    CHAT("채팅", Icons.Outlined.ChatBubbleOutline, enabled = true),

    /** Phase 1 미구현 — '내 찜 목록' 화면이 없다(찜 토글 자체는 상품 상세에서 된다). */
    FAVORITE("찜", Icons.Outlined.FavoriteBorder, enabled = false),

    /** Phase 1 미구현 — 프로필/설정 화면이 없다(로그아웃은 홈 상단에서 처리). */
    PROFILE("내정보", Icons.Outlined.PersonOutline, enabled = false),
}

/**
 * 앱 하단 내비게이션 바. **stateless** 다 — 어느 탭이 선택됐는지 모르고, 이동도 직접 하지 않는다.
 * 현재 탭([selected])을 받고 눌린 탭을 [onSelect] 로 알려 주기만 한다(이동은 NavHost 소유자가 결정).
 *
 * @param chatBadgeCount 채팅 탭에 찍을 안 읽은 메시지 수. 0이면 배지를 그리지 않는다.
 *   서버에 '전체 안읽음 합계' API 가 없으므로 `GET /api/chat-rooms` 의 `unreadCount` 를 합산해 넘겨라.
 */
@Composable
fun MarketOnBottomBar(
    selected: MarketOnTab,
    onSelect: (MarketOnTab) -> Unit,
    modifier: Modifier = Modifier,
    chatBadgeCount: Int = 0,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        MarketOnTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                enabled = tab.enabled,
                onClick = { onSelect(tab) },
                icon = {
                    if (tab == MarketOnTab.CHAT && chatBadgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ) {
                                    // 99를 넘으면 배지가 아이콘보다 넓어져 레이아웃이 밀린다.
                                    Text(if (chatBadgeCount > 99) "99+" else "$chatBadgeCount")
                                }
                            },
                        ) {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Preview(name = "하단 탭 (홈 선택)", showBackground = true)
@Composable
private fun MarketOnBottomBarPreview() {
    MarketOnTheme {
        MarketOnBottomBar(selected = MarketOnTab.HOME, onSelect = {})
    }
}

@Preview(name = "하단 탭 (채팅 선택 + 배지)", showBackground = true)
@Composable
private fun MarketOnBottomBarChatPreview() {
    MarketOnTheme {
        MarketOnBottomBar(selected = MarketOnTab.CHAT, onSelect = {}, chatBadgeCount = 3)
    }
}
