package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.local.TokenDataStore
import com.dongnemarket.mobile.data.remote.AuthApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.data.remote.apiCallForUnit
import com.dongnemarket.mobile.data.remote.dto.LoginRequestDto
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [AuthRepository] 구현. 원격(Retrofit)과 로컬 저장소(DataStore) 두 개를 조합한다.
 *
 * "로그인 = 서버에서 토큰을 받아 기기에 저장하는 것"이라는 한 덩어리의 일을
 * 여기서 끝내는 것이 핵심이다. ViewModel 이 토큰 문자열을 만지면
 * 저장 누락·중복 저장 같은 버그가 화면 개수만큼 생긴다.
 */
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenDataStore: TokenDataStore,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> =
        tokenDataStore.accessToken.map { token -> !token.isNullOrBlank() }

    override suspend fun login(email: String, password: String): Result<Unit> {
        val request = LoginRequestDto(
            // 키보드 자동완성으로 앞뒤 공백이 붙으면 서버 @Email 검증에서 400이 난다.
            email = email.trim(),
            // 비밀번호는 공백도 유효 문자일 수 있으므로 절대 trim 하지 않는다.
            password = password,
            // autoLogin=false 면 refreshToken 이 세션 쿠키가 되어 앱 재시작 시 사라진다 → 항상 true.
            autoLogin = true,
        )

        val tokenDto = apiCall { api.login(request) }
            .getOrElse { error -> return Result.failure(error) }

        // 계약상 올 수 없는 값이지만, 빈 토큰을 저장하면 "로그인된 것처럼 보이는데
        // 모든 인증 요청이 401" 이라는 가장 디버깅하기 어려운 상태가 된다 → 여기서 막는다.
        if (tokenDto.accessToken.isBlank()) return Result.failure(AppError.EmptyBody())

        tokenDataStore.saveAccessToken(tokenDto.accessToken)
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        // 서버 호출 결과를 일부러 무시한다.
        // 이유: 서버가 하는 일은 refreshToken 삭제뿐이고 accessToken 은 무효화하지 않는다.
        // 즉 "로그아웃됐다"의 실체는 로컬 토큰 삭제다. accessToken 이 이미 만료돼
        // 이 호출이 401로 실패하는 경우가 오히려 흔한데, 그때 실패를 반환하면
        // 사용자는 로그아웃 버튼을 눌러도 로그인 상태에 갇힌다.
        apiCallForUnit { api.logout() }

        // 성공/실패와 무관하게 반드시 지운다.
        tokenDataStore.clear()
        return Result.success(Unit)
    }
}
