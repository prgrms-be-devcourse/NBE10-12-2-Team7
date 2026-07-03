'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'

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

  useEffect(() => {
    const saved = (() => {
      try { return localStorage.getItem('marketon-theme') } catch { return null }
    })()
    const system = window.matchMedia?.('(prefers-color-scheme: dark)').matches
    const isDark = saved ? saved === 'dark' : system
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
        <Link className="ghost-link" href="/login">로그인</Link>
      </div>
    </header>
  )
}
