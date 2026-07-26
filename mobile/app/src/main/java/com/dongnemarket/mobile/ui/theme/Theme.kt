package com.dongnemarket.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 라이트 색 구성 — 마켓온 핑크 + 크림 배경
private val LightColors = lightColorScheme(
    primary = MarketPink,
    onPrimary = Color.White,
    primaryContainer = MarketPinkSoft,
    onPrimaryContainer = MarketPinkDark,
    background = CreamSurface,
    onBackground = InkText,
    surface = Color.White,
    onSurface = InkText,
)

// 다크 색 구성 — 순검정 배경(웹과 동일)
private val DarkColors = darkColorScheme(
    primary = MarketPink,
    onPrimary = Color.White,
    primaryContainer = MarketPinkDark,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
)

// 앱 전체를 감싸는 테마. 시스템 다크모드에 따라 색 구성을 자동 선택.
@Composable
fun MarketOnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
