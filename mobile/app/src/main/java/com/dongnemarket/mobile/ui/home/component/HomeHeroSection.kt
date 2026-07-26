package com.dongnemarket.mobile.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 홈 최상단 히어로. 브랜드 카피 + 내 동네 이름만 담는 크림색 카드다.
 *
 * ⚠️ **숫자 통계(거래 건수·회원 수·총 상품 수)를 넣지 않는다.**
 * 백엔드에 집계/통계 엔드포인트가 없고 상품 목록도 커서 페이징이라 전체 건수를 셀 방법조차 없다
 * (계약 §8-1). 하드코딩한 숫자는 데모에서 바로 거짓임이 드러나므로 아예 자리를 만들지 않았다.
 *
 * @param region 대표 동네 이름(`"서울 강남구"`). **null 이면 동네 줄을 그리지 않는다** —
 *   "동네 미설정" 같은 빈 칸을 남기면 히어로가 고장 난 것처럼 보인다.
 *   (Phase 1 에는 동네 설정 화면이 없어서 여기서 설정을 유도할 곳도 없다.)
 */
@Composable
fun HomeHeroSection(
    region: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "우리 동네 중고거래",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "이웃이 내놓은 물건을 가까운 곳에서 만나 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (region != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = region,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(name = "히어로 (동네 있음)", showBackground = true)
@Composable
private fun HomeHeroSectionPreview() {
    MarketOnTheme {
        HomeHeroSection(region = "서울 강남구", modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "히어로 (동네 미설정)", showBackground = true)
@Composable
private fun HomeHeroSectionNoRegionPreview() {
    MarketOnTheme {
        HomeHeroSection(region = null, modifier = Modifier.padding(16.dp))
    }
}
