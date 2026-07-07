'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { getCurrentMemberId } from '@/lib/auth'
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
}

type PageStatus = 'loading' | 'ready' | 'error'

const POLL_MS = 3000

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

export default function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const myMemberId = getCurrentMemberId()

  const [room, setRoom] = useState<ChatRoom | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)

  const [toastText, setToastText] = useState('')
  const [toastOn, setToastOn] = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const listRef = useRef<HTMLDivElement | null>(null)
  const seenIds = useRef<Set<number>>(new Set())

  function showToast(msg: string) {
    setToastText(msg); setToastOn(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastOn(false), 1900)
  }

  function scrollToBottom() {
    requestAnimationFrame(() => {
      const el = listRef.current
      if (el) el.scrollTop = el.scrollHeight
    })
  }

  function markRead() {
    apiFetch(`/api/chat-rooms/${roomId}/read`, { method: 'POST' }).catch(() => {})
  }

  function mergeMessages(incoming: ChatMessage[]) {
    incoming.forEach(m => seenIds.current.add(m.messageId))
    setMessages(prev => {
      const map = new Map(prev.map(m => [m.messageId, m]))
      for (const m of incoming) map.set(m.messageId, m)
      return Array.from(map.values()).sort((a, b) => a.messageId - b.messageId)
    })
  }

  useEffect(() => {
    let cancelled = false

    Promise.all([
      apiFetch('/api/chat-rooms').then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })),
      apiFetch(`/api/chat-rooms/${roomId}/messages`).then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })),
    ]).then(([roomsRes, messagesRes]) => {
      if (cancelled) return
      if (!roomsRes.ok) { setErrorMsg(roomsRes.data?.message ?? '채팅방을 불러오지 못했습니다.'); setStatus('error'); return }

      const found: ChatRoom | undefined = (roomsRes.data?.data ?? [])
        .find((r: ChatRoom) => String(r.roomId) === String(roomId))
      if (!found) { setErrorMsg('채팅방을 찾을 수 없어요.'); setStatus('error'); return }
      setRoom(found)

      if (!messagesRes.ok) { setErrorMsg(messagesRes.data?.message ?? '메시지를 불러오지 못했습니다.'); setStatus('error'); return }
      const list: ChatMessage[] = (messagesRes.data?.data?.messages ?? []).slice().reverse()
      mergeMessages(list)
      setStatus('ready')
      scrollToBottom()
      markRead()
    }).catch(() => {
      if (!cancelled) { setErrorMsg('서버에 연결할 수 없습니다.'); setStatus('error') }
    })

    return () => { cancelled = true }
  }, [roomId])

  useEffect(() => {
    if (status !== 'ready') return
    const timer = setInterval(() => {
      apiFetch(`/api/chat-rooms/${roomId}/messages`)
        .then(r => r.ok ? r.json() : null)
        .then(data => {
          const list: ChatMessage[] = (data?.data?.messages ?? []).slice().reverse()
          if (list.some(m => !seenIds.current.has(m.messageId))) {
            mergeMessages(list)
            scrollToBottom()
            markRead()
          }
        })
        .catch(() => {})
    }, POLL_MS)
    return () => clearInterval(timer)
  }, [status, roomId])

  async function handleSend(e: React.FormEvent) {
    e.preventDefault()
    const v = input.trim()
    if (!v || sending) return
    setSending(true)
    try {
      const res = await apiFetch(`/api/chat-rooms/${roomId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: v }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) { showToast(data?.message ?? '전송 중 오류가 발생했습니다.'); return }
      mergeMessages([data.data])
      setInput('')
      scrollToBottom()
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    } finally {
      setSending(false)
    }
  }

  if (status === 'loading') {
    return <main className={styles.wrap}><div className={styles.empty}><p>불러오는 중...</p></div></main>
  }
  if (status === 'error') {
    return (
      <main className={styles.wrap}>
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <p>{errorMsg}</p>
          <Link href="/chat" className={`btn ghost ${styles.backBtn}`}>채팅 목록으로</Link>
        </div>
      </main>
    )
  }

  return (
    <main className={styles.wrap}>
      <div className={styles.head}>
        <Link href="/chat" className={styles.back} aria-label="채팅 목록으로">←</Link>
        <div className={styles.thumb}>
          {room?.product.thumbnailUrl
            ? <img src={room.product.thumbnailUrl} alt="" />
            : <span className={styles.thumbPh}>NO IMG</span>}
        </div>
        <div className={styles.headInfo}>
          <div className={styles.nick}>{room?.opponent.nickname}</div>
          <div className={styles.pname}>{room?.product.title} · {room ? priceText(room.product.price) : ''}</div>
        </div>
        {room && (
          <Link href={`/products/${room.product.productId}`} className={styles.viewProduct}>상품보기</Link>
        )}
      </div>

      <div className={styles.messages} ref={listRef}>
        {messages.length === 0 && (
          <div className={styles.emptyMsg}>대화를 시작해보세요</div>
        )}
        {messages.map(m => (
          <div
            key={m.messageId}
            className={`${styles.bubbleRow}${m.senderId === myMemberId ? ' ' + styles.mine : ''}`}
          >
            <div className={styles.bubble}>{m.content}</div>
            <div className={styles.mtime}>{formatTime(m.createdAt)}</div>
          </div>
        ))}
      </div>

      <form className={styles.inputRow} onSubmit={handleSend}>
        <input
          className={styles.input}
          placeholder="메시지를 입력하세요"
          value={input}
          onChange={e => setInput(e.target.value)}
          maxLength={1000}
        />
        <button type="submit" className={styles.sendBtn} disabled={sending || !input.trim()}>전송</button>
      </form>

      <div className={`toast${toastOn ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
