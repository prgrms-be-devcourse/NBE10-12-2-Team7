package com.dongnemarket.mobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * 가격 → 사람이 읽는 문자열. 예) `800000.00` → `"800,000원"`, `0` → `"나눔"`.
 *
 * **왜 [BigDecimal] 인가**: 서버가 같은 `price` 필드를 엔드포인트마다 다른 스케일로 보낸다
 * (GET 계열 `800000.00`, POST 응답 `800000`). Int/Long 으로 파싱하면 GET 응답에서 깨진다.
 *
 * **왜 소수부를 버리는가**: `800000.00` 을 그대로 찍으면 `"800,000.00원"` 이 되어 이상하다.
 * 반올림이 아니라 [RoundingMode.DOWN] 으로 **잘라 낸다** — 가격을 앱이 올려 보여 주는 일이 없게 한다.
 *
 * @param giveawayLabel 0원일 때 대신 보여 줄 문구. `null` 을 주면 0원도 `"0원"` 으로 찍는다.
 */
fun formatWon(price: BigDecimal, giveawayLabel: String? = "나눔"): String {
    if (giveawayLabel != null && price.signum() == 0) return giveawayLabel

    // NumberFormat 은 thread-safe 하지 않다 → 공유 인스턴스를 두지 않고 호출마다 만든다.
    val formatter = NumberFormat.getIntegerInstance(Locale.KOREA)
    return formatter.format(price.setScale(0, RoundingMode.DOWN)) + "원"
}

/**
 * 상품 가격 표시. 카드·상세·채팅방 상품줄이 같은 서식을 쓰게 하는 자리.
 *
 * 크기/색은 화면마다 다르므로 [style]·[color] 로 열어 두었다(카드는 titleSmall, 상세는 headlineSmall 등).
 *
 * @param showGiveaway 0원을 "나눔" 으로 바꿔 표시할지. 서버에 '나눔' 상태가 없어 가격으로만 판정한다.
 */
@Composable
fun ProductPrice(
    price: BigDecimal,
    modifier: Modifier = Modifier,
    showGiveaway: Boolean = true,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = formatWon(price, giveawayLabel = if (showGiveaway) "나눔" else null),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}

@Preview(name = "가격 표시", showBackground = true)
@Composable
private fun ProductPricePreview() {
    MarketOnTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            ProductPrice(BigDecimal("800000.00"))                    // 800,000원
            ProductPrice(BigDecimal("12000"))                        // 12,000원
            ProductPrice(BigDecimal("0.00"))                         // 나눔
            ProductPrice(BigDecimal("0.00"), showGiveaway = false)   // 0원
            ProductPrice(
                price = BigDecimal("1234567.89"),                    // 1,234,567원 (소수부 버림)
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
