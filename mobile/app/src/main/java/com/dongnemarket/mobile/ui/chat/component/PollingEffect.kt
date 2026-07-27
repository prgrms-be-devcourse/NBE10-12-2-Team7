package com.dongnemarket.mobile.ui.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation

/**
 * "화면이 보이는 동안만" 폴링을 켜 주는 효과(effect).
 *
 * ## 왜 이런 장치가 필요한가
 * 채팅은 실시간 수신 경로(WebSocket/SSE)가 서버에 없어서 REST 를 주기적으로 다시 부르는
 * 폴링으로 새 메시지를 얻는다(계약 §7-22). 문제는 **끄는 타이밍**이다.
 * ViewModel 은 화면이 보이는지 모르므로, ViewModel 안에서 무한 루프를 띄우면
 * 사용자가 홈 버튼을 눌러 앱을 내려도 3초마다 요청이 계속 나간다(배터리·데이터·서버 부담).
 *
 * [Lifecycle.State.STARTED] 는 "화면이 사용자에게 보이는 상태"다.
 * [repeatOnLifecycle] 은 STARTED 로 올라올 때 블록을 실행하고 STARTED 아래로 내려가면
 * 그 블록의 코루틴을 **취소**한다. 그 취소를 [awaitCancellation] + `finally` 로 붙잡아
 * [onStop] 을 부르는 것이 이 함수의 전부다.
 *
 * ```
 * PollingEffect(onStart = viewModel::startPolling, onStop = viewModel::stopPolling)
 * ```
 *
 * ## 왜 [rememberUpdatedState] 를 쓰는가
 * `viewModel::startPolling` 같은 메서드 참조는 리컴포지션마다 **다른 객체**가 될 수 있다.
 * 그걸 `LaunchedEffect` 의 key 로 쓰면 화면이 다시 그려질 때마다 효과가 재시작되면서
 * 폴링이 끊기고 다시 붙는다. 그래서 key 는 lifecycleOwner 하나만 두고,
 * 람다는 항상 최신 것을 읽도록 이렇게 감싼다.
 */
@Composable
fun PollingEffect(
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val start by rememberUpdatedState(onStart)
    val stop by rememberUpdatedState(onStop)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            start()
            try {
                // 여기서 영원히 기다린다. 화면이 가려지면 이 코루틴이 취소되고 finally 로 떨어진다.
                awaitCancellation()
            } finally {
                stop()
            }
        }
    }
}
