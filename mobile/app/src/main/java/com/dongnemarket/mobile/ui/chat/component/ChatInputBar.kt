package com.dongnemarket.mobile.ui.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 채팅방 하단 입력바.
 *
 * stateless 다 — 글자를 스스로 들고 있지 않고 [value] 를 받아 그리고 [onValueChange] 로 알린다.
 * (입력값을 여기서 `remember` 로 갖고 있으면 화면 회전에 날아가고, ViewModel 이 전송할 문장을 모른다.)
 *
 * @param enabled 입력·전송 자체가 가능한지. 전송 중이거나 상대가 탈퇴한 경우 false.
 * @param canSend 전송 버튼만 따로 잠근다(빈 문자열은 서버 `@NotBlank` 에 걸리므로 미리 막는다).
 * @param hint 입력이 막힌 이유를 사용자에게 알려 주는 한 줄(예: 상대 탈퇴). null 이면 그리지 않는다.
 * @param isSending 전송 왕복 중이면 버튼 자리에 작은 스피너를 돌려 "먹은 것 같은데?" 를 없앤다.
 */
@OptIn(ExperimentalMaterial3Api::class) // FilledIconButton 이 버전에 따라 실험 API 로 표시된다
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canSend: Boolean = value.isNotBlank(),
    isSending: Boolean = false,
    hint: String? = null,
) {
    // modifier 를 Surface(가장 바깥)에 붙인다 — 호출부가 넘기는 imePadding 이
    // 배경까지 함께 밀어 올려야 키보드 위에 바가 얹힌 모양이 된다.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    placeholder = { Text("메시지를 입력하세요") },
                    // 긴 문장은 최대 4줄까지 늘어나고 그 뒤로는 안에서 스크롤된다.
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium,
                )

                FilledIconButton(
                    onClick = onSendClick,
                    enabled = enabled && canSend,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("chat_send"),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "전송",
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "입력바 · 기본", showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    MarketOnTheme {
        Column {
            ChatInputBar(value = "", onValueChange = {}, onSendClick = {})
            ChatInputBar(value = "네 오늘 저녁 7시에 뵐게요", onValueChange = {}, onSendClick = {})
            ChatInputBar(
                value = "전송 중인 문장",
                onValueChange = {},
                onSendClick = {},
                enabled = false,
                isSending = true,
            )
            ChatInputBar(
                value = "",
                onValueChange = {},
                onSendClick = {},
                enabled = false,
                hint = "상대방이 탈퇴해 메시지를 보낼 수 없어요.",
            )
        }
    }
}
