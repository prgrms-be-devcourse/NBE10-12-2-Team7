package com.dongnemarket.mobile.ui.productdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.component.NetworkImage
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 상세 상단의 상품 이미지 영역. 여러 장이면 좌우로 넘기고, 아래에 현재 위치 점을 찍는다.
 *
 * **이미지가 0장인 경우가 정상이다.** 시드/데모 데이터에는 이미지가 아예 없어서
 * 상세 응답의 `imageUrls` 가 `[]` 로 온다(계약 §7-6) → 그때는 페이저를 만들지 않고
 * 공용 [NetworkImage] 의 회색 플레이스홀더 한 장으로 대체한다(페이저는 페이지 수가 0이면 그릴 것이 없다).
 *
 * @param imageUrls 이미 절대 URL 로 변환된 목록(Data 계층이 변환해 준다). 순서는 서버의 `sortOrder ASC`.
 */
@Composable
fun ProductImagePager(
    imageUrls: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.size <= 1) {
        NetworkImage(
            url = imageUrls.firstOrNull(),
            contentDescription = contentDescription,
            modifier = modifier,
        )
        return
    }

    // pageCount 를 값이 아니라 람다로 받는다 — 목록이 바뀌어도 PagerState 를 다시 만들지 않게 하는 API 규약이다.
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            NetworkImage(
                url = imageUrls[page],
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }

        PagerDots(
            pageCount = imageUrls.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
    }
}

/** 현재 페이지 표시용 점. 색은 테마 토큰만 쓴다(hex 하드코딩 금지). */
@Composable
private fun PagerDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { index ->
            val isCurrent = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Preview(name = "이미지 여러 장", showBackground = true)
@Composable
private fun ProductImagePagerMultiPreview() {
    MarketOnTheme {
        // Preview 에는 네트워크가 없으므로 세 장 모두 플레이스홀더로 보이는 것이 정상이다.
        ProductImagePager(
            imageUrls = listOf(
                "/api/products/images/a.jpg",
                "/api/products/images/b.jpg",
                "/api/products/images/c.jpg",
            ),
            contentDescription = "상품 이미지",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
    }
}

@Preview(name = "이미지 없음(데모 데이터)", showBackground = true)
@Composable
private fun ProductImagePagerEmptyPreview() {
    MarketOnTheme {
        ProductImagePager(
            imageUrls = emptyList(),
            contentDescription = "상품 이미지",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
    }
}
