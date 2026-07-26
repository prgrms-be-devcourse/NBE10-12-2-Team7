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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 서버 시각의 타임존. 서버는 오프셋 없는 문자열만 주고 DB 접속 URL 이 `serverTimezone=Asia/Seoul` 이라
 * **KST 로 간주**한다(계약 §0.6). 이 가정이 틀리면 "3시간 전" 이 9시간 어긋난다.
 */
private val ServerZone: ZoneId = ZoneId.of("Asia/Seoul")

/** 하루보다 오래된 시각에 쓰는 절대 표기. */
private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)

/** 채팅 말풍선용 시계 표기. 예) "오후 3:24" */
private val ClockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA)

/**
 * 서버 시각 문자열 파싱. 실패하면 예외를 던지지 않고 `null` 을 준다.
 *
 * ⚠ `Instant.parse` / `OffsetDateTime.parse` / `SimpleDateFormat` 은 **반드시 실패한다** —
 * 서버 문자열에 오프셋(`Z`)이 없고 소수부 자릿수가 가변이다
 * (`2026-07-26T13:45:30` / `...:30.123` / `...:30.123456` 이 모두 가능).
 * [DateTimeFormatter.ISO_LOCAL_DATE_TIME] 은 이 세 형태를 모두 받아 준다.
 */
fun parseServerDateTime(raw: String?): LocalDateTime? {
    if (raw.isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
}

/**
 * 서버 시각 문자열 → "3분 전 / 2시간 전 / 3일 전 / 2026.07.01".
 *
 * **파싱 실패 시 크래시하지 않고 원문을 그대로 돌려준다.** 서버가 포맷을 바꾸면 표기가 못생겨질 뿐
 * 앱은 계속 돈다 — 채팅 목록 한 줄 때문에 화면 전체가 죽는 것이 최악이다.
 *
 * ⚠ **상품에는 시간 필드가 아예 없다**(계약 §7-10). 이 함수는 채팅 메시지·채팅방 목록·찜 전용이고,
 * 상품 카드/상세에서 시간이 들어갈 자리에는 `region`(동네 이름)을 쓴다.
 *
 * @param now 비교 기준 시각. 테스트·Preview 에서 고정값을 넣을 수 있게 파라미터로 뺐다.
 */
fun formatRelativeTime(raw: String?, now: Instant = Instant.now()): String {
    if (raw.isNullOrBlank()) return ""
    val parsed = parseServerDateTime(raw) ?: return raw

    val seconds = Duration.between(parsed.atZone(ServerZone).toInstant(), now).seconds
    return when {
        // 음수(= 서버 시각이 미래)도 여기로 흡수한다. 기기 시계가 서버보다 느리면 "-1분 전" 이 나온다.
        seconds < 60 -> "방금 전"
        seconds < 3_600 -> "${seconds / 60}분 전"
        seconds < 86_400 -> "${seconds / 3_600}시간 전"
        seconds < 7 * 86_400 -> "${seconds / 86_400}일 전"
        else -> parsed.format(DateFormatter)
    }
}

/**
 * 서버 시각 문자열 → "오후 3:24". 채팅 말풍선 옆 시각처럼 **상대시간이 어울리지 않는 자리**에 쓴다.
 * 파싱 실패 시 원문을 그대로 돌려주는 정책은 [formatRelativeTime] 과 같다.
 */
fun formatClockTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val parsed = parseServerDateTime(raw) ?: return raw
    return parsed.format(ClockFormatter)
}

/**
 * 상대시간 텍스트. 목록 한 줄의 보조 정보라서 기본 색이 흐린 [MaterialTheme.colorScheme.onSurfaceVariant] 다.
 *
 * 주의: 기본값 `Instant.now()` 는 **리컴포지션 시점에 다시 읽힌다**. 즉 스스로 1분마다 갱신되지는 않고
 * 화면이 다시 그려질 때만 갱신된다. 채팅은 폴링으로 어차피 자주 다시 그려지므로 타이머를 두지 않았다.
 */
@Composable
fun RelativeTime(
    raw: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = formatRelativeTime(raw),
        style = style,
        color = color,
        modifier = modifier,
    )
}

@Preview(name = "상대시간", showBackground = true)
@Composable
private fun RelativeTimePreview() {
    // Preview 가 매번 같은 그림이 되도록 기준 시각을 고정한다.
    val now = LocalDateTime.of(2026, 7, 26, 18, 0, 0).atZone(ServerZone).toInstant()
    val samples = listOf(
        "2026-07-26T17:59:30" to "30초 전 → 방금 전",
        "2026-07-26T17:57:00.123" to "3분 전",
        "2026-07-26T16:00:00.123456" to "2시간 전",
        "2026-07-23T18:00:00" to "3일 전",
        "2026-07-01T09:30:00" to "1주일 이상 → 날짜",
        "26/07/26 18:00" to "파싱 실패 → 원문 그대로",
    )

    MarketOnTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            samples.forEach { (raw, note) ->
                Text(
                    text = "${formatRelativeTime(raw, now)}    ($note)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${formatClockTime("2026-07-26T15:24:05.7")}    (시계 표기)",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
