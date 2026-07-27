package com.dongnemarket.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱의 단일 색 구성. **라이트 전용**(다크모드는 모바일 범위에서 제외 — workflow §0).
 *
 * Material3 의 이름 규칙: `xxx` = 그 위에 얹히는 배경, `onXxx` = 그 위에 올리는 글자/아이콘 색.
 * 즉 primary 버튼 위 글자색은 onPrimary 가 된다.
 */
private val MarketOnColorScheme = lightColorScheme(
    primary = MarketPink,
    onPrimary = Color.White,
    primaryContainer = MarketPinkSoft,
    onPrimaryContainer = MarketPinkDark,

    secondary = MarketPinkDark,
    onSecondary = Color.White,

    background = CreamSurface,
    onBackground = InkText,

    surface = Color.White,
    onSurface = InkText,
    surfaceVariant = CreamSurface2,
    onSurfaceVariant = MutedText,

    outline = BorderLine,
    outlineVariant = BorderLine,

    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
)

/** 웹 `--radius: 18px` 를 카드/버튼 기본 모서리로 이식. */
private val MarketOnShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * 앱 전체를 감싸는 테마. 하위 Composable 은 `MaterialTheme.colorScheme.primary` 처럼
 * 색·모양·타이포를 여기서 내려받는다(색을 화면마다 하드코딩하지 않는 이유).
 *
 * 다크모드 분기를 없앴으므로 `isSystemInDarkTheme()` 을 보지 않는다 → 기기 설정과 무관하게 항상 같은 화면.
 */
@Composable
fun MarketOnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MarketOnColorScheme,
        shapes = MarketOnShapes,
        typography = Typography,
        content = content,
    )
}
