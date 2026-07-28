package com.dongnemarket.mobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 로딩·에러·빈 상태 3종 세트.
 *
 * 4개 화면(로그인·홈·상세·채팅)이 각자 만들면 스피너 크기와 재시도 버튼 문구가 화면마다 달라진다.
 * ViewModel 의 `UiState` 가 어떤 모양이든, 화면은 이 세 Composable 중 하나를 그리기만 하면 된다.
 *
 * 세 함수 모두 **부모가 준 영역 안에서 가운데 정렬**한다(스스로 fillMaxSize 하지 않는다) —
 * 전체 화면에도 쓰고 리스트 하단·카드 안에도 쓸 수 있게 하려는 것이다.
 * 화면 전체를 덮고 싶으면 호출부에서 `modifier = Modifier.fillMaxSize()` 를 넘겨라.
 */
@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * 에러 상태. [message] 에는 **반드시 `AppError.userMessage`** 를 넣어라 —
 * 백엔드 `error` 코드 문자열(`INVALID_TOKEN` 같은 ErrorCode 상수명)을 사용자에게 노출하지 않는다.
 *
 * 호출 형태(둘 다 가능):
 * ```
 * ErrorView(message = state.message) { viewModel.retry() }   // 재시도 버튼 있음
 * ErrorView(message = state.message)                          // 버튼 없음
 * ```
 *
 * @param onRetry `null` 이면 재시도 버튼을 그리지 않는다. 자동 재시도를 넣지 말고
 *   **사용자가 누르는 버튼**만 두는 이유: 상품 상세 GET 은 조회수를 +1 하는 쓰기 동작이다(계약 §7-11).
 */
@Composable
fun ErrorView(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

/**
 * 빈 상태. "에러" 가 아니라 "정상인데 결과가 0건" 인 경우다.
 *
 * 이 구분이 중요한 케이스들: 내 동네 미설정(`[]` 정상 성공), 검색 결과 0건,
 * 채팅방 없음, 메시지 없는 새 방. 서버가 에러를 준 게 아니므로 [ErrorView] 를 쓰면 사용자가 오해한다.
 *
 * @param actionLabel·onAction 둘 다 주면 행동 버튼이 붙는다(예: 빈 채팅 목록에서 "상품 둘러보기").
 */
@Composable
fun EmptyView(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(name = "로딩/에러/빈 상태", showBackground = true, heightDp = 720)
@Composable
private fun StateViewsPreview() {
    MarketOnTheme {
        Column {
            LoadingView()
            ErrorView(message = "인터넷 연결을 확인해 주세요.") { }
            ErrorView(message = "삭제되었거나 거래가 끝난 상품입니다.")
            EmptyView(message = "아직 채팅이 없어요.")
            EmptyView(
                message = "내 동네를 설정하면 가까운 상품만 볼 수 있어요.",
                actionLabel = "동네 설정하기",
                onAction = { },
            )
        }
    }
}
