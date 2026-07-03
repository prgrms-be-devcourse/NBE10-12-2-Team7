import { clearAccessToken, getAccessToken, setAccessToken } from './auth'

let reissuePromise: Promise<string | null> | null = null

/**
 * HttpOnly Refresh Token 쿠키로 Access Token 재발급을 시도한다.
 * Refresh Token은 JS에서 읽을 수 없으므로(HttpOnly) 존재 여부를 먼저 확인하지 않고 바로 호출하며,
 * 브라우저가 쿠키를 자동으로 실어 보내도록 credentials:'include'를 사용한다.
 * 동시에 여러 곳에서 호출돼도 진행 중인 요청 하나를 공유해 중복 reissue 호출을 막는다.
 */
function reissueAccessToken(): Promise<string | null> {
  if (reissuePromise) return reissuePromise

  reissuePromise = (async () => {
    try {
      const res = await fetch('/api/auth/reissue', {
        method: 'POST',
        credentials: 'include',
      })
      if (!res.ok) return null

      const data = await res.json().catch(() => null)
      const accessToken = data?.data?.accessToken
      if (!accessToken) return null

      setAccessToken(accessToken)
      return accessToken
    } catch {
      return null
    }
  })().finally(() => {
    reissuePromise = null
  })

  return reissuePromise
}

/**
 * 앱/웹 진입 시 1회 호출. Refresh Token 쿠키가 유효하면 Access Token을 재발급받고
 * `/api/members/me`로 로그인 상태를 복구한다.
 * 실패해도 이 함수는 리다이렉트하지 않는다 — 공개 페이지는 비로그인 상태로도 정상 동작해야 하기 때문이다.
 * (인증이 실제로 필요한 페이지는 apiFetch가 401을 겪을 때 로그인 페이지로 이동시킨다.)
 */
export async function bootstrapAutoLogin(): Promise<boolean> {
  if (getAccessToken()) return true

  const accessToken = await reissueAccessToken()
  if (!accessToken) return false

  try {
    const res = await fetch('/api/members/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    return res.ok
  } catch {
    return false
  }
}

function withAuthHeader(init: RequestInit, token: string | null): RequestInit {
  return {
    ...init,
    headers: {
      ...(init.headers ?? {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  }
}

/**
 * 인증이 필요한 API 호출 공용 wrapper.
 * Access Token을 자동으로 붙이고, 401을 받으면 Refresh Token 쿠키로 재발급을 1회 시도해 원 요청을 재시도한다.
 * 재발급도 실패하면 Access Token을 지우고 로그인 페이지로 이동한다.
 */
export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const res = await fetch(input, withAuthHeader(init, getAccessToken()))
  if (res.status !== 401) return res

  const newAccessToken = await reissueAccessToken()
  if (!newAccessToken) {
    clearAccessToken()
    if (typeof window !== 'undefined') window.location.href = '/login'
    return res
  }

  return fetch(input, withAuthHeader(init, newAccessToken))
}
