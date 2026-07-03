export const ACCESS_TOKEN_KEY = 'marketon-access-token'

export function getAccessToken(): string | null {
  try {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

/** JWT의 subject(memberId) 클레임을 디코드한다. 서버 호출 없이 "이 댓글이 내 것인가" 같은 판단에 쓴다. */
export function getCurrentMemberId(): number | null {
  const token = getAccessToken()
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    const id = Number(json.sub)
    return Number.isFinite(id) ? id : null
  } catch {
    return null
  }
}
