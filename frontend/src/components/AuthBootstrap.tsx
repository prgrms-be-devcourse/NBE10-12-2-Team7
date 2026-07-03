'use client'

import { useEffect } from 'react'
import { bootstrapAutoLogin } from '@/lib/apiClient'

/**
 * 앱 진입 시 1회, Refresh Token이 남아있으면 Access Token 재발급을 시도해 자동 로그인한다.
 * 화면에는 아무것도 렌더링하지 않는다.
 */
export default function AuthBootstrap() {
  useEffect(() => {
    bootstrapAutoLogin()
  }, [])

  return null
}
