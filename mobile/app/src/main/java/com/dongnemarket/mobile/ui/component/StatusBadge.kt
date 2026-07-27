package com.dongnemarket.mobile.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import com.dongnemarket.mobile.ui.theme.StatusBadgeColors
import java.math.BigDecimal

/**
 * 화면에 그릴 배지 종류. **서버 enum([TradeStatus])과 1:1이 아니다.**
 *
 * 서버 거래 상태는 `ON_SALE`/`RESERVED`/`COMPLETED` 3개뿐이고 '나눔' 이라는 상태는 없다.
 * 나눔은 `ON_SALE` + `price == 0` 이라는 **앱 측 규약**(계약 §6.2)이라서,
 * "서버 상태" 와 "화면 배지" 를 분리한 타입이 하나 더 필요하다.
 */
enum class StatusBadge {
    /** 판매중 (ON_SALE 이고 가격이 0보다 큼) */
    ON_SALE,

    /** 예약중 */
    RESERVED,

    /**
     * 거래완료.
     * 홈 목록·검색·카테고리에는 절대 오지 않는다(서버가 필터로 제외). 채팅방 상품에서만 관측된다.
     */
    COMPLETED,

    /** 나눔 (ON_SALE 이고 가격이 0원) */
    GIVEAWAY,

    /** 배지를 그리지 않는다. 서버가 앱이 모르는 상태를 보냈을 때의 안전판. */
    NONE,
}

/**
 * (서버 상태 + 가격) → 화면 배지. 계약 §6.2 매핑표를 코드로 옮긴 것이다.
 *
 * 화면마다 이 `when` 을 다시 쓰면 나눔 규칙(가격 0원)을 빼먹은 화면이 생기므로 여기 한 곳에만 둔다.
 * [TradeStatus.UNKNOWN] 은 배지를 숨긴다 — 서버에 상태가 추가돼도 앱은 "모르는 배지" 를 그리지 않고 그냥 지나간다.
 */
fun badgeOf(tradeStatus: TradeStatus, price: BigDecimal): StatusBadge = when (tradeStatus) {
    TradeStatus.RESERVED -> StatusBadge.RESERVED
    TradeStatus.COMPLETED -> StatusBadge.COMPLETED
    // signum() == 0 으로 비교하는 이유: 서버가 같은 0원을 "0" / "0.00" 두 가지 스케일로 보내는데
    // BigDecimal 의 equals 는 스케일까지 비교해서 BigDecimal.ZERO == 0.00 이 false 가 된다.
    TradeStatus.ON_SALE -> if (price.signum() == 0) StatusBadge.GIVEAWAY else StatusBadge.ON_SALE
    TradeStatus.UNKNOWN -> StatusBadge.NONE
}

/** 배지에 찍히는 한국어 라벨. [StatusBadge.NONE] 은 라벨이 없다(null). */
val StatusBadge.label: String?
    get() = when (this) {
        StatusBadge.ON_SALE -> "판매중"
        StatusBadge.RESERVED -> "예약중"
        StatusBadge.COMPLETED -> "거래완료"
        StatusBadge.GIVEAWAY -> "나눔"
        StatusBadge.NONE -> null
    }

/** (글자색, 배경색) 짝. 색 값은 [StatusBadgeColors] 토큰만 쓴다(hex 하드코딩 금지). */
private val StatusBadge.colors: Pair<Color, Color>?
    get() = when (this) {
        StatusBadge.ON_SALE -> StatusBadgeColors.OnSale
        StatusBadge.RESERVED -> StatusBadgeColors.Reserved
        StatusBadge.COMPLETED -> StatusBadgeColors.SoldOut
        StatusBadge.GIVEAWAY -> StatusBadgeColors.Share
        StatusBadge.NONE -> null
    }

/**
 * 상품 상태 배지. 카드 썸네일 위·상세 제목 옆·채팅 목록 상품줄에서 같은 모양으로 쓴다.
 *
 * [StatusBadge.NONE] 이면 **아무것도 그리지 않는다**(빈 칸도 차지하지 않는다).
 */
@Composable
fun StatusBadge(
    badge: StatusBadge,
    modifier: Modifier = Modifier,
) {
    val text = badge.label ?: return
    val (fg, bg) = badge.colors ?: return

    Text(
        text = text,
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 서버 값(거래상태 + 가격)을 그대로 넘기는 편의 오버로드.
 * 화면에서 [badgeOf] 를 따로 부르지 않아도 되게 한 것이고, 내부는 위 Composable 과 동일하다.
 */
@Composable
fun StatusBadge(
    tradeStatus: TradeStatus,
    price: BigDecimal,
    modifier: Modifier = Modifier,
) {
    StatusBadge(badge = badgeOf(tradeStatus, price), modifier = modifier)
}

@Preview(name = "상태 배지 5종", showBackground = true)
@Composable
private fun StatusBadgePreview() {
    MarketOnTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            // NONE 은 아무것도 그려지지 않는 것이 정상이다(칸이 비지 않고 사라진다).
            StatusBadge(TradeStatus.ON_SALE, BigDecimal("800000.00"))
            StatusBadge(TradeStatus.ON_SALE, BigDecimal("0.00"))
            StatusBadge(TradeStatus.RESERVED, BigDecimal("12000"))
            StatusBadge(TradeStatus.COMPLETED, BigDecimal("12000"))
            StatusBadge(TradeStatus.UNKNOWN, BigDecimal.ZERO)
        }
    }
}
