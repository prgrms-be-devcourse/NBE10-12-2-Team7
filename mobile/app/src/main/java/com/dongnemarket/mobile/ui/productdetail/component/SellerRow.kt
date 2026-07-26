package com.dongnemarket.mobile.ui.productdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 판매자 행. **닉네임 하나뿐인 것이 정상이다.**
 *
 * 서버에 판매자 프로필 이미지 컬럼이 없고 평점·거래횟수·판매자 프로필 조회 API 도 없다(계약 §8-11).
 * 그래서 아바타는 **닉네임 첫 글자 이니셜 원형**으로 대체하고, 없는 정보를 위한 빈 자리(별점 칸 등)를
 * 만들지 않는다 — 비어 있는 칸은 "로딩이 덜 됐나?" 라는 오해를 만든다.
 *
 * @param nickname 상세 응답의 `sellerNickname`. 탈퇴 회원이면 서버가 "탈퇴한 사용자" 를 준다.
 */
@Composable
fun SellerRow(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialAvatar(nickname = nickname)

        Column {
            Text(
                text = nickname.ifBlank { "알 수 없음" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "판매자",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 닉네임 첫 글자를 넣은 원형 아바타. 프로필 이미지가 서버에 없어서 쓰는 대체 표현이다. */
@Composable
private fun InitialAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    // 첫 글자가 없을 수 있다(공백 닉네임) → "?" 로 폴백하고 크래시하지 않는다.
    val initial = nickname.trim().firstOrNull()?.toString() ?: "?"

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview(name = "판매자 행", showBackground = true)
@Composable
private fun SellerRowPreview() {
    MarketOnTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            SellerRow(nickname = "동네주민")
            SellerRow(nickname = "탈퇴한 사용자")
            SellerRow(nickname = " ")
        }
    }
}
