export const ACCESS_TOKEN_KEY = 'marketon-access-token'

export function getAccessToken(): string | null {
  try {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
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
