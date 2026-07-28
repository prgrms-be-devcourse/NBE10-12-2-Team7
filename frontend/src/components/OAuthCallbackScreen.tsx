'use client'

import Link from 'next/link'
import { Suspense, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { completeOAuthLogin, type OAuthProviderKey } from '@/lib/oauth'
import styles from './OAuthCallbackScreen.module.css'

type CallbackState = { phase: 'processing' } | { phase: 'error'; message: string }

const SUCCESS_REDIRECT = '/products'

function OAuthCallbackInner({ provider, label }: { provider: OAuthProviderKey; label: string }) {
  const searchParams = useSearchParams()
  const startedRef = useRef(false)
  const [state, setState] = useState<CallbackState>({ phase: 'processing' })

  useEffect(() => {
    // React Strict Mode(개발 환경)에서 effect가 두 번 실행돼도 로그인 완료 API가 중복 호출되지 않도록 막는다.
    if (startedRef.current) return
    startedRef.current = true

    completeOAuthLogin(provider, searchParams).then(result => {
      if (result.status === 'success') {
        window.location.href = SUCCESS_REDIRECT
        return
      }
      setState({ phase: 'error', message: result.message })
    })
  }, [provider, searchParams])

  return (
    <main className={styles.stage}>
      <div className={styles.card}>
        {state.phase === 'error' ? (
          <>
            <p className={styles.message}>{state.message}</p>
            <Link href="/login" className="btn block">로그인 페이지로 돌아가기</Link>
          </>
        ) : (
          <p className={styles.message}>{label} 로그인을 완료하는 중이에요...</p>
        )}
      </div>
    </main>
  )
}

/** useSearchParams는 Suspense 경계 안에서만 사용 가능 */
export default function OAuthCallbackScreen(props: { provider: OAuthProviderKey; label: string }) {
  return (
    <Suspense>
      <OAuthCallbackInner {...props} />
    </Suspense>
  )
}
