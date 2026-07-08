'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'
import { apiFetch, bootstrapAutoLogin, logout } from '@/lib/apiClient'
import { getAccessToken, isAdmin } from '@/lib/auth'
import styles from './admin.module.css'

const NAV = [
  { href: '/admin/dashboard', label: '📊 대시보드' },
  { href: '/admin/members',   label: '👤 회원 관리' },
  { href: '/admin/products',  label: '📦 상품 관리' },
  { href: '/admin/reports',   label: '🚨 신고 관리' },
  { href: '/admin/comments',  label: '💬 댓글 관리' },
  { href: '/admin/ai',        label: '🤖 AI 어시스턴트' },
]

type GuardStatus = 'checking' | 'ok' | 'unauthenticated' | 'forbidden'

interface Me { nickname: string; role: string }

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const [status, setStatus] = useState<GuardStatus>('checking')
  const [me, setMe] = useState<Me | null>(null)
  const [retryKey, setRetryKey] = useState(0)
  const [dark, setDark] = useState(false)

  useEffect(() => {
    const saved = (() => {
      try { return localStorage.getItem('marketon-theme') } catch { return null }
    })()
    const system = window.matchMedia?.('(prefers-color-scheme: dark)').matches
    const isDark = saved ? saved === 'dark' : system
    // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage/matchMedia는 클라이언트에서만 읽을 수 있어 SSR 하이드레이션 이후에만 계산 가능
    setDark(isDark)
    applyTheme(isDark)

    const mq = window.matchMedia?.('(prefers-color-scheme: dark)')
    if (!mq) return
    const handler = (e: MediaQueryListEvent) => {
      if (!localStorage.getItem('marketon-theme')) {
        setDark(e.matches)
        applyTheme(e.matches)
      }
    }
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [])

  function applyTheme(isDark: boolean) {
    if (isDark) document.documentElement.setAttribute('data-theme', 'dark')
    else document.documentElement.removeAttribute('data-theme')
  }

  function toggleTheme() {
    const next = !dark
    setDark(next)
    applyTheme(next)
    try { localStorage.setItem('marketon-theme', next ? 'dark' : 'light') } catch {}
  }

  useEffect(() => {
    let cancelled = false

    async function init() {
      setStatus('checking')

      // accessToken이 없어도 refreshToken 쿠키가 남아있으면 재발급을 먼저 시도한다(자동 로그인/새로고침 복구).
      if (!getAccessToken()) await bootstrapAutoLogin()
      if (cancelled) return
      if (!getAccessToken()) { setStatus('unauthenticated'); return }
      if (!isAdmin()) { setStatus('forbidden'); return }

      try {
        const res = await apiFetch('/api/members/me')
        if (cancelled) return
        if (res.status === 401) { setStatus('unauthenticated'); return }
        const data = await res.json().catch(() => null)
        if (!res.ok) { setStatus('forbidden'); return }
        setMe(data?.data)
        setStatus('ok')
      } catch {
        if (!cancelled) setStatus('forbidden')
      }
    }

    init()
    return () => { cancelled = true }
  }, [retryKey])

  async function handleLogout() {
    try {
      await logout()
    } finally {
      window.location.href = '/login'
    }
  }

  if (status === 'checking') {
    return <div className={styles.guardWrap}><p>확인 중...</p></div>
  }
  if (status === 'unauthenticated') {
    return (
      <div className={styles.guardWrap}>
        <p>관리자 로그인이 필요해요.</p>
        <div className={styles.btnRow}>
          <button type="button" className="btn ghost" onClick={() => setRetryKey(k => k + 1)}>다시 확인</button>
          <Link href="/login" className="btn">로그인하기</Link>
        </div>
      </div>
    )
  }
  if (status === 'forbidden') {
    return (
      <div className={styles.guardWrap}>
        <p>관리자만 접근할 수 있어요.</p>
        <div className={styles.btnRow}>
          <button type="button" className="btn ghost" onClick={() => setRetryKey(k => k + 1)}>다시 확인</button>
          <Link href="/products" className="btn">홈으로</Link>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.alayout}>
      <aside className={styles.aside}>
        <div className={styles.brand}>MarketON ADMIN<small>운영 관리 콘솔</small></div>
        <nav>
          {NAV.map(item => (
            <Link
              key={item.href}
              href={item.href}
              className={`${styles.navLink}${pathname?.startsWith(item.href) ? ' ' + styles.navLinkOn : ''}`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <div className={styles.amain}>
        <div className={styles.atop}>
          <div className={styles.nm}>{me?.nickname ?? '관리자'} <span>권한: 관리자</span></div>
          <button className="icon-btn" onClick={toggleTheme} aria-label="테마 전환" type="button">
            {dark ? (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 14.5A8 8 0 0 1 9.5 4a7 7 0 1 0 10.5 10.5z" />
              </svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="4.2" />
                <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4" />
              </svg>
            )}
          </button>
          <button type="button" className={styles.logoutBtn} onClick={handleLogout}>로그아웃</button>
        </div>
        <div className={styles.acontent}>{children}</div>
      </div>
    </div>
  )
}
