package com.dongnemarket.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context 하나당 "marketon_prefs" 파일을 여는 DataStore 인스턴스를 만들어 주는 위임(delegate).
 * 파일이 중복 생성되지 않도록 최상위(top-level)에 딱 한 번만 선언한다.
 */
private val Context.tokenPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "marketon_prefs",
)

/**
 * 로그인해서 받은 JWT accessToken 을 기기에 저장·조회·삭제한다.
 *
 * 왜 SharedPreferences가 아니고 DataStore인가:
 * DataStore는 코루틴/Flow 기반이라 디스크 읽기가 메인 스레드를 막지 않고,
 * 값이 바뀌면 [accessToken] 을 구독한 쪽에 자동으로 흘러간다.
 *
 * 왜 refreshToken이 없는가:
 * 백엔드는 refreshToken 을 HttpOnly 쿠키로 내려준다 → 앱이 직접 다룰 값이 아니다.
 * 토큰 재발급(reissue)·자동 로그인은 Phase 3 과제로 분리했다.
 */
@Singleton
class TokenDataStore @Inject constructor(
    // @ApplicationContext = Hilt가 들고 있는 Application 수준 Context.
    // Activity Context를 쓰면 화면이 사라질 때 함께 죽어 버리므로 앱 전역 저장소에는 부적합하다.
    @ApplicationContext private val context: Context,
) {
    private val keyAccessToken = stringPreferencesKey("access_token")

    /** 저장된 토큰의 흐름. 없으면 null 이 흘러온다. */
    val accessToken: Flow<String?> =
        context.tokenPreferences.data.map { prefs -> prefs[keyAccessToken] }

    suspend fun saveAccessToken(token: String) {
        context.tokenPreferences.edit { prefs -> prefs[keyAccessToken] = token }
    }

    /** 로그아웃·401 처리용. */
    suspend fun clear() {
        context.tokenPreferences.edit { prefs -> prefs.remove(keyAccessToken) }
    }
}
