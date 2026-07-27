package com.dongnemarket.mobile.ui.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.domain.model.ChatRoomHeader
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.component.NetworkImage
import com.dongnemarket.mobile.ui.component.ProductPrice
import com.dongnemarket.mobile.ui.component.StatusBadge
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.math.BigDecimal

/**
 * 채팅방 상단의 상품 요약 한 줄. "무엇에 대한 대화인지" 를 잊지 않게 고정으로 붙인다.
 *
 * 이 줄을 눌러 상품 상세로 가는 동작은 **넣지 않았다** — 이 화면이 받는 이동 람다는
 * `onBackClick` 뿐이고, 상세로 갔다가 다시 채팅으로 오는 백스택 정책이 정해지지 않았다.
 * (거래완료 상품은 상세가 404 라서 눌렀을 때 빈 화면으로 떨어지는 문제도 있다.)
 *
 * 배지에 `COMPLETED`(거래완료)가 실제로 나타나는 화면이다 — 홈 목록은 서버가 거래완료를
 * 걸러 주지만 채팅방의 상품은 걸러 주지 않는다(계약 §6.2).
 */
@Composable
fun ChatProductHeader(
    header: ChatRoomHeader,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NetworkImage(
            url = header.productThumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.extraSmall,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header.productTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 가격은 BigDecimal 그대로 넘긴다(서버 스케일이 800000.00 / 800000 로 달라 Int 변환 금지).
            ProductPrice(
                price = header.productPrice,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        StatusBadge(header.productTradeStatus, header.productPrice)
    }
}

@Preview(name = "채팅방 상품 헤더", showBackground = true)
@Composable
private fun ChatProductHeaderPreview() {
    MarketOnTheme {
        Column {
            ChatProductHeader(
                header = ChatRoomHeader(
                    roomId = 1L,
                    opponentId = 22L,
                    opponentNickname = "당근이",
                    productId = 12L,
                    productTitle = "아이패드 프로 11인치 셀룰러 256GB 팝니다",
                    productPrice = BigDecimal("800000.00"),
                    productTradeStatus = TradeStatus.ON_SALE,
                    productThumbnailUrl = null,
                ),
            )
            ChatProductHeader(
                header = ChatRoomHeader(
                    roomId = 2L,
                    opponentId = 33L,
                    opponentNickname = "몽실이",
                    productId = 15L,
                    productTitle = "책상 나눔합니다",
                    productPrice = BigDecimal("0.00"),
                    productTradeStatus = TradeStatus.COMPLETED,
                    productThumbnailUrl = null,
                ),
            )
        }
    }
}
