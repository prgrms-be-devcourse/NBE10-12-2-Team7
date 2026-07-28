import { setAccessToken } from './auth'

export type OAuthProviderKey = 'kakao' | 'google'

/**
 * 카카오/구글 로그인 시작: 인가 URL을 발급받아 브라우저를 그 URL로 이동시킨다.
 * 성공하면 페이지 자체가 이동하므로 호출자에게 돌아오지 않는다 — 반환값은 실패했을 때의 에러 메시지다.
 */
export async function startOAuthLogin(provider: OAuthProviderKey): Promise<string | null> {
  try {
    const res = await fetch(`/api/auth/oauth/${provider}/authorization`, {
      method: 'POST',
      credentials: 'include',
    })
    const data = await res.json().catch(() => null)
    if (!res.ok) {
      return data?.message ?? '소셜 로그인을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.'
    }

    const authorizationUrl: unknown = data?.data?.authorizationUrl
    // 서버가 준 값이라도 스킴을 확인한다 — 예상치 못한 값으로 브라우저를 이동시키지 않기 위한 최소 방어.
    if (typeof authorizationUrl !== 'string' || !/^https:\/\//.test(authorizationUrl)) {
      return '소셜 로그인 응답이 올바르지 않습니다.'
    }

    window.location.href = authorizationUrl
    return null
  } catch {
    return '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
  }
}

export type OAuthCallbackResult =
  | { status: 'success' }
  | { status: 'error'; message: string }

/**
 * 카카오/구글 callback 완료 처리. provider가 error를 돌려줬거나 code/state가 없으면
 * 백엔드를 호출하지 않는다. 정상일 때만 로그인 완료 API를 호출해 Access Token을 저장한다.
 * Refresh Token은 백엔드 응답의 HttpOnly Set-Cookie로만 내려오므로 여기서 다루지 않는다.
 */
export async function completeOAuthLogin(
  provider: OAuthProviderKey,
  params: URLSearchParams,
): Promise<OAuthCallbackResult> {
  const providerError = params.get('error')
  if (providerError) {
    const description = params.get('error_description')
    return { status: 'error', message: description || '소셜 로그인이 취소되었거나 실패했습니다.' }
  }

  const code = params.get('code')
  const state = params.get('state')
  if (!code || !state) {
    return { status: 'error', message: '소셜 로그인 응답이 올바르지 않습니다. 처음부터 다시 시도해주세요.' }
  }

  try {
    const res = await fetch(`/api/auth/oauth/${provider}/login`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, state }),
    })
    const data = await res.json().catch(() => null)
    if (!res.ok) {
      return { status: 'error', message: data?.message ?? '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }
    }

    const accessToken = data?.data?.accessToken
    if (!accessToken) {
      return { status: 'error', message: '로그인 응답이 올바르지 않습니다.' }
    }

    setAccessToken(accessToken)
    return { status: 'success' }
  } catch {
    return { status: 'error', message: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.' }
  }
}
