/**
 * Access Token은 모듈 스코프 변수에만 보관한다(localStorage/sessionStorage 사용 안 함).
 * 새로고침하면 사라지는 게 의도된 동작 — apiClient.bootstrapAutoLogin()이
 * HttpOnly Refresh Token 쿠키로 재발급받아 복구한다.
 */
let accessToken: string | null = null

/** Access Token이 설정/해제될 때마다 발생하는 이벤트. Header 등 여러 컴포넌트가 로그인 상태를 반응형으로 구독하는 데 쓴다. */
export const AUTH_CHANGED_EVENT = 'marketon-auth-changed'

function notifyAuthChanged(): void {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string): void {
  accessToken = token
  notifyAuthChanged()
}

export function clearAccessToken(): void {
  accessToken = null
  notifyAuthChanged()
}

function decodeTokenPayload(): Record<string, unknown> | null {
  const token = getAccessToken()
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

/** JWT의 subject(memberId) 클레임을 디코드한다. 서버 호출 없이 "이 댓글이 내 것인가" 같은 판단에 쓴다. */
export function getCurrentMemberId(): number | null {
  const json = decodeTokenPayload()
  if (!json) return null
  const id = Number(json.sub)
  return Number.isFinite(id) ? id : null
}

/** JWT의 role 클레임("ROLE_USER" | "ROLE_ADMIN")을 디코드한다. 서버 호출 없이 관리자 화면 접근 여부를 판단하는 데 쓴다. */
export function getCurrentRole(): string | null {
  const json = decodeTokenPayload()
  const role = json?.role
  return typeof role === 'string' ? role : null
}

export function isAdmin(): boolean {
  return getCurrentRole() === 'ROLE_ADMIN'
}
