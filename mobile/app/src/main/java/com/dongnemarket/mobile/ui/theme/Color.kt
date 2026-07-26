package com.dongnemarket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// 웹 globals.css(:root)의 MarketON 디자인 토큰 이식.
// 모바일은 라이트 전용이므로 [prefers-color-scheme: dark] 블록은 옮기지 않는다.

// 브랜드 로즈
val MarketPink = Color(0xFFE85D9E)      // --primary
val MarketPinkDark = Color(0xFFD4488A)  // --primary-d
val MarketPinkSoft = Color(0xFFFBE3EF)  // --primary-soft

// 표면·텍스트
val PageBg = Color(0xFFFFFFFF)          // --bg
val CreamSurface = Color(0xFFFDF7F3)    // --surface
val CreamSurface2 = Color(0xFFF6ECE6)   // --surface-2
val InkText = Color(0xFF2A1F25)         // --text
val MutedText = Color(0xFF9A8590)       // --text-muted
val BorderLine = Color(0xFFF1E2EA)      // --border

// 의미 색 (Danger / OK / Blue / Amber / Neutral) — 배지·알림에 사용
val Danger = Color(0xFFC43D62)          // --danger
val DangerSoft = Color(0xFFFDEBEF)      // --danger-soft
val Ok = Color(0xFF1F8A59)              // --ok
val OkSoft = Color(0xFFEAF6EF)          // --ok-soft
val InfoBlue = Color(0xFF2F77E0)        // --blue
val InfoBlueSoft = Color(0xFFE7F0FD)    // --blue-soft
val Amber = Color(0xFFB9792C)           // --amber
val AmberSoft = Color(0xFFFDF3E6)       // --amber-soft
val Neutral = Color(0xFF7A7079)         // --neutral
val NeutralSoft = Color(0xFFF1EEF1)     // --neutral-soft

/**
 * 상품 상태 배지 4종의 (글자색, 배경색) 짝.
 * 웹 카드와 같은 규칙: 판매중=초록 / 예약중=amber / 거래완료=회색 / 나눔=로즈.
 */
object StatusBadgeColors {
    val OnSale = Ok to OkSoft                  // 판매중
    val Reserved = Amber to AmberSoft          // 예약중
    val SoldOut = Neutral to NeutralSoft       // 거래완료
    val Share = MarketPinkDark to MarketPinkSoft // 나눔
}
