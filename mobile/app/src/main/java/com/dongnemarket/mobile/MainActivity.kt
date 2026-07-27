package com.dongnemarket.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dongnemarket.mobile.ui.navigation.MarketOnNavHost
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 유일한 Activity. 화면 전환은 Activity를 갈아 끼우지 않고
 * [MarketOnNavHost] 안에서 Composable 을 바꿔 끼우는 방식(Single-Activity 구조)으로 한다.
 *
 * @AndroidEntryPoint: 이 Activity(와 그 안의 @HiltViewModel)가 Hilt 컨테이너에서
 * 의존성을 받을 수 있게 만든다. 이게 없으면 화면의 ViewModel 주입이 런타임에 실패한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarketOnTheme {
                MarketOnNavHost()
            }
        }
    }
}
