package com.dongnemarket.mobile.ui.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.ui.component.formatClockTime
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/** 말풍선 최대 폭. 화면 폭 전체를 채우면 누가 보낸 말인지 좌/우로 구분하기 어려워진다. */
private val BubbleMaxWidth = 280.dp

/**
 * 메시지 말풍선 하나.
 *
 * @param isMine `message.senderId == 내 memberId` 의 결과. **이 컴포넌트가 직접 판단하지 않는다** —
 *   내 memberId 는 `GET /api/members/me` 로 얻는 값이고, 메시지에는 발신자 닉네임조차 없다.
 *   판단을 화면(ViewModel) 쪽에 두면 로그인 사용자가 바뀌어도 이 컴포넌트는 그대로다.
 *
 * ## 읽음 표시가 없는 이유
 * 서버가 읽음 지점을 DB 컬럼으로만 관리하고 **어떤 응답에도 노출하지 않는다**(계약 §8-5).
 * 그래서 "상대가 읽음" 을 그릴 방법이 구조적으로 없다 — 대신 전송 시각만 찍고,
 * 안읽음은 채팅 목록의 배지로만 보여 준다. (있는 척 그리면 거짓 정보가 된다.)
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        // 시각을 말풍선 아래쪽 선에 맞춘다(여러 줄 메시지에서도 시각이 바닥에 붙는다).
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isMine) {
            MessageTime(message.createdAt)
            Spacer(Modifier.width(4.dp))
        }

        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = BubbleMaxWidth),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (!isMine) {
            Spacer(Modifier.width(4.dp))
            MessageTime(message.createdAt)
        }
    }
}

/**
 * 말풍선 옆 시각. 여기서는 "3분 전" 같은 상대시간이 아니라 **시계 표기**("오후 3:24")를 쓴다 —
 * 대화는 앞뒤 메시지와의 시간 간격이 중요해서 절대 시각이 읽기 쉽다.
 *
 * `formatClockTime` 은 파싱 실패 시 원문을 그대로 돌려준다(서버 시각 문자열은 오프셋이 없고
 * 소수부 자릿수가 가변이라 `Instant.parse` 류는 반드시 실패한다 → 직접 파싱하지 마라).
 */
@Composable
private fun MessageTime(raw: String) {
    Text(
        text = formatClockTime(raw),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "말풍선 (내 것 / 상대 것)", showBackground = true, backgroundColor = 0xFFFDF7F3)
@Composable
private fun ChatMessageBubblePreview() {
    MarketOnTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            ChatMessageBubble(
                message = ChatMessage(1L, 22L, "안녕하세요! 상품 아직 있나요?", "2026-07-26T15:20:10"),
                isMine = false,
            )
            ChatMessageBubble(
                message = ChatMessage(2L, 11L, "네 있습니다. 오늘 저녁에 강남역 근처에서 직거래 가능하세요?", "2026-07-26T15:24:05.7"),
                isMine = true,
            )
            ChatMessageBubble(
                message = ChatMessage(3L, 22L, "좋아요", "2026-07-26T15:25:00.123456"),
                isMine = false,
            )
            // 파싱 실패 케이스: 시각 자리에 원문이 그대로 나오고 크래시하지 않는다.
            ChatMessageBubble(
                message = ChatMessage(4L, 11L, "그럼 7시에 뵐게요", "26/07/26 15:30"),
                isMine = true,
            )
        }
    }
}
