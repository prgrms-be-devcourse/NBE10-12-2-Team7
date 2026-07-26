package com.dongnemarket.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import com.dongnemarket.mobile.ui.theme.MarketPink

// 앱의 진입점 Activity. 앱이 켜지면 여기가 가장 먼저 실행된다.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {                 // 이 안이 Compose로 그리는 화면
            MarketOnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HomePlaceholder()
                }
            }
        }
    }
}

// Phase 0 확인용 임시 화면. 화면 가운데에 "MarketON"을 표시.
@Composable
fun HomePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "MarketON",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MarketPink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "우리 동네 중고거래 · 모바일",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// Android Studio 오른쪽에서 앱 실행 없이 미리보는 창.
@Preview(showBackground = true)
@Composable
fun HomePlaceholderPreview() {
    MarketOnTheme {
        HomePlaceholder()
    }
}
