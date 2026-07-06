'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'
import { logout } from '@/lib/apiClient'
import { AUTH_CHANGED_EVENT, getAccessToken } from '@/lib/auth'

const NAV_LINKS = [
  { href: '/products',     label: '상품목록' },
  { href: '/products/new', label: '상품등록' },
  { href: '/my-products',  label: '내상품' },
  { href: '/favorites',    label: '관심상품' },
  { href: '/my-reports',   label: '내신고내역' },
  { href: '/my-profile',   label: '내정보' },
]

export default function Header() {
  const pathname = usePathname()
  const [dark, setDark] = useState(false)
  const [loggedIn, setLoggedIn] = useState(() => !!getAccessToken())
  /* 실제 알림 API가 없어 아직은 항상 false — 알림 기능이 생기면 이 값을 실제 미확인 알림 여부로 채운다. */
  const [hasUnreadNotification] = useState(false)

  useEffect(() => {
    const handler = () => setLoggedIn(!!getAccessToken())
    window.addEventListener(AUTH_CHANGED_EVENT, handler)
    return () => window.removeEventListener(AUTH_CHANGED_EVENT, handler)
  }, [])

  async function handleLogout() {
    await logout()
  }

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

  /* 관리자 화면은 자체 사이드바 레이아웃을 쓰므로 고객용 헤더를 숨긴다.
     (테마 초기화 useEffect는 계속 실행되도록 훅 아래에서 분기한다) */
  if (pathname?.startsWith('/admin')) return null

  return (
    <header>
      <div className="head-in">
        <Link className="logo" href="/products">
          Market<span>ON</span>
        </Link>
        <nav className="main-nav">
          {NAV_LINKS.map(({ href, label }) => (
            <Link key={href} href={href} className={pathname === href ? 'on' : ''}>
              {label}
            </Link>
          ))}
        </nav>
        <div className="sp" />
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
        <button className="icon-btn notif-btn" aria-label="알림" type="button">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9z" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
          {hasUnreadNotification && <span className="notif-dot" />}
        </button>
        <Link className="icon-btn" href="/chat" aria-label="채팅">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5z" />
          </svg>
        </Link>
        {loggedIn ? (
          <button className="ghost-link" onClick={handleLogout} type="button">로그아웃</button>
        ) : (
          <Link className="ghost-link" href="/login">로그인</Link>
        )}
      </div>
    </header>
  )
}
