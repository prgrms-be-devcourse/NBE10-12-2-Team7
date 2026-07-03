'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'
import { ACCESS_TOKEN_KEY, getAccessToken, isAdmin } from '@/lib/auth'
import styles from './admin.module.css'

const NAV = [
  { href: '/admin/dashboard', label: '📊 대시보드' },
  { href: '/admin/members',   label: '👤 회원 관리' },
  { href: '/admin/products',  label: '📦 상품 관리' },
  { href: '/admin/reports',   label: '🚨 신고 관리' },
  { href: '/admin/comments',  label: '💬 댓글 관리' },
]

type GuardStatus = 'checking' | 'ok' | 'unauthenticated' | 'forbidden'

interface Me { nickname: string; role: string }

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const [status, setStatus] = useState<GuardStatus>('checking')
  const [me, setMe] = useState<Me | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const token = getAccessToken()
    if (!token) { setStatus('unauthenticated'); return }
    if (!isAdmin()) { setStatus('forbidden'); return }

    let cancelled = false
    setStatus('checking')
    fetch('/api/members/me', { headers: { Authorization: `Bearer ${token}` } })
      .then(async r => {
        if (r.status === 401) { if (!cancelled) setStatus('unauthenticated'); return }
        const data = await r.json().catch(() => null)
        if (!r.ok) { if (!cancelled) setStatus('forbidden'); return }
        if (!cancelled) {
          setMe(data?.data)
          setStatus('ok')
        }
      })
      .catch(() => { if (!cancelled) setStatus('forbidden') })
    return () => { cancelled = true }
  }, [retryKey])

  function handleLogout() {
    try { localStorage.removeItem(ACCESS_TOKEN_KEY) } catch {}
    window.location.href = '/login'
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
          <button type="button" className="btn ghost" onClick={handleLogout}>로그아웃</button>
        </div>
        <div className={styles.acontent}>{children}</div>
      </div>
    </div>
  )
}
