'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import type { TradeStatus } from '@/lib/tradeStatus'
import styles from './page.module.css'

interface ChatProductSummary {
  productId: number
  title: string
  price: number
  tradeStatus: TradeStatus
  thumbnailUrl: string | null
}

interface ChatMemberSummary {
  memberId: number
  nickname: string
}

interface ChatMessage {
  messageId: number
  senderId: number
  content: string
  createdAt: string
}

interface ChatRoom {
  roomId: number
  product: ChatProductSummary
  opponent: ChatMemberSummary
  createdAt: string
  lastMessage: ChatMessage | null
  unreadCount: number
}

type PageStatus = 'loading' | 'ready' | 'error'

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

function formatTime(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }
  return iso.slice(5, 10).replace('-', '.')
}

export default function ChatListPage() {
  const [rooms, setRooms] = useState<ChatRoom[]>([])
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    let cancelled = false

    apiFetch('/api/chat-rooms')
      .then(async res => {
        const data = await res.json().catch(() => null)
        if (!res.ok) throw new Error(data?.message ?? '채팅 목록을 불러오지 못했습니다.')
        if (!cancelled) { setRooms(data?.data ?? []); setStatus('ready') }
      })
      .catch(err => {
        if (cancelled) return
        setErrorMsg(err instanceof Error ? err.message : '채팅 목록을 불러오지 못했습니다.')
        setStatus('error')
      })

    return () => { cancelled = true }
  }, [])

  return (
    <main className={styles.wrap}>
      <div className={styles.headRow}>
        <h1>채팅</h1>
        <p>거래 상대와 나눈 대화를 확인할 수 있어요.</p>
      </div>

      {status === 'loading' && (
        <div className={styles.empty}><p>불러오는 중...</p></div>
      )}

      {status === 'error' && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <p>{errorMsg}</p>
          <Link href="/login" className={`btn ${styles.loginBtn}`}>로그인하기</Link>
        </div>
      )}

      {status === 'ready' && (
        rooms.length > 0 ? (
          <div className={styles.list}>
            {rooms.map(room => (
              <Link key={room.roomId} href={`/chat/${room.roomId}`} className={styles.rcard}>
                <div className={styles.thumb}>
                  {room.product.thumbnailUrl
                    ? <img src={room.product.thumbnailUrl} alt="" />
                    : <span className={styles.thumbPh}>NO IMG</span>}
                </div>
                <div className={styles.body}>
                  <div className={styles.top}>
                    <span className={styles.nick}>{room.opponent.nickname}</span>
                  </div>
                  <div className={styles.pname}>{room.product.title} · {priceText(room.product.price)}</div>
                  <div className={styles.last}>{room.lastMessage ? room.lastMessage.content : '대화를 시작해보세요'}</div>
                </div>
                <div className={styles.meta}>
                  {room.lastMessage && <span className={styles.time}>{formatTime(room.lastMessage.createdAt)}</span>}
                  {room.unreadCount > 0 && (
                    <span className={styles.unreadBadge}>
                      {room.unreadCount > 99 ? '99+' : room.unreadCount}
                    </span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className={styles.empty}>
            <div className={styles.emptyIcon}>💬</div>
            <p>아직 채팅방이 없어요.</p>
          </div>
        )
      )}

      <div className={styles.apiNote}>GET /api/chat-rooms</div>
    </main>
  )
}
