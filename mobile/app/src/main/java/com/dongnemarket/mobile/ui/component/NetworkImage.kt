package com.dongnemarket.mobile.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dongnemarket.mobile.BuildConfig
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 서버 이미지 한 장. Coil 의 `AsyncImage` 를 감싸 **"이미지가 없는 상태"를 정상 취급**하게 만든 것이다.
 *
 * 왜 래퍼가 필요한가:
 *  1. **URL 이 null·빈 문자열인 경우가 정상이다.** 시드/데모 데이터에는 이미지가 아예 없어서
 *     목록 `thumbnailUrl = null`, 상세 `imageUrls = []` 로 온다(계약 §7-6) → 플레이스홀더가 필수다.
 *  2. 로딩 중·실패도 같은 회색 플레이스홀더로 그린다. 화면마다 다른 실패 UI 를 만들지 않게 한다.
 *  3. 상대경로 방어(아래 [resolveImageUrl] 주석).
 *
 * 4개 화면이 썸네일을 그릴 때는 `Image`/`AsyncImage` 를 직접 쓰지 말고 이걸 써라.
 */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Outlined.Image,
) {
    val resolved = resolveImageUrl(url)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (resolved == null) {
            // 네트워크 요청을 아예 걸지 않는다 — URL 이 없는 것은 실패가 아니라 정상 상태다.
            ImagePlaceholder(placeholderIcon)
        } else {
            // SubcomposeAsyncImage 를 쓰는 이유: 로딩·실패 자리에 Painter 가 아니라
            // '아이콘 + 배경' 조합의 Composable 을 넣을 수 있다(AsyncImage 는 Painter 만 받는다).
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(resolved)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                loading = { ImagePlaceholder(placeholderIcon) },
                error = { ImagePlaceholder(placeholderIcon) },
            )
        }
    }
}

/** 이미지 자리에 들어가는 중립 플레이스홀더. 배경색은 부모 [Box] 가 이미 칠했다. */
@Composable
private fun ImagePlaceholder(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(28.dp),
    )
}

/**
 * 표시 가능한 절대 URL 로 정규화한다. 못 만들면 `null`(= 플레이스홀더).
 *
 * Data 계층(`ImageUrlMapper`)이 이미 절대 URL 로 바꿔서 넘겨 주므로 **보통은 그냥 통과한다.**
 * 그런데도 여기서 한 번 더 보정하는 이유:
 *  - 서버가 주는 원본은 상대경로(`/api/products/images/{uuid}.jpg`)라서 그대로 Coil 에 주면 실패한다.
 *  - 서버가 `imageUrls` 문자열을 검증 없이 저장해 **절대 URL 이 섞여 있다**(테스트 데이터에 존재)
 *    → `startsWith("http")` 분기가 없으면 `baseUrl` 을 두 번 붙여 깨진다.
 *  - 이 함수는 멱등이다(이미 절대 URL 이면 손대지 않는다) → 이중 보정 사고가 나지 않는다.
 */
private fun resolveImageUrl(raw: String?): String? {
    val value = raw?.trim()
    if (value.isNullOrEmpty()) return null
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        // BuildConfig.BASE_URL 은 "/" 로 끝난다(Retrofit 규약) → 이어 붙일 때 중복 "/" 를 제거.
        value.startsWith("/") -> BuildConfig.BASE_URL.trimEnd('/') + value
        else -> BuildConfig.BASE_URL.trimEnd('/') + "/" + value
    }
}

@Preview(name = "네트워크 이미지 (플레이스홀더)", showBackground = true)
@Composable
private fun NetworkImagePreview() {
    MarketOnTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            // Preview 에서는 네트워크가 없으므로 세 경우 모두 플레이스홀더로 보이는 것이 정상이다.
            NetworkImage(
                url = null,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.medium,
            )
            NetworkImage(
                url = "",
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.small,
            )
            NetworkImage(
                url = "/api/products/images/sample.jpg",
                contentDescription = "상품 이미지",
                modifier = Modifier.size(96.dp),
            )
        }
    }
}
