'use client'

import { useEffect, useRef, useState } from 'react'
import { getAccessToken } from '@/lib/auth'
import styles from '../admin.module.css'

type Role = 'ai' | 'user' | 'error'

interface ChatMessage {
  id: number
  role: Role
  text: string
}

const SUGGESTED_QUESTIONS = [
  '대시보드 현황 알려줘',
  '정지된 회원 목록 보여줘',
  '접수 대기 신고 몇 건이야?',
  '숨김 처리된 상품 알려줘',
]

let nextMessageId = 1

export default function AdminAiPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: nextMessageId++, role: 'ai', text: '안녕하세요. 관리 데이터를 조회해 드립니다. 예: "오늘 접수 대기 신고 몇 건이야?"' },
  ])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const logRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const el = logRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, sending])

  async function handleSend(e: React.FormEvent) {
    e.preventDefault()
    const text = input.trim()
    if (!text || sending) return

    setMessages(prev => [...prev, { id: nextMessageId++, role: 'user', text }])
    setInput('')

    const token = getAccessToken()
    if (!token) {
      setMessages(prev => [...prev, { id: nextMessageId++, role: 'error', text: '로그인이 만료됐어요. 다시 로그인해주세요.' }])
      return
    }

    setSending(true)
    try {
      const res = await fetch('/api/admin/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ message: text }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setMessages(prev => [...prev, { id: nextMessageId++, role: 'error', text: data?.message ?? '답변을 가져오지 못했어요.' }])
        return
      }
      setMessages(prev => [...prev, { id: nextMessageId++, role: 'ai', text: data?.data?.answer ?? '답변이 비어있어요.' }])
    } catch {
      setMessages(prev => [...prev, { id: nextMessageId++, role: 'error', text: '서버에 연결할 수 없습니다.' }])
    } finally {
      setSending(false)
    }
  }

  function fillChip(question: string) {
    setInput(question)
  }

  return (
    <>
      <div className={styles.ptitle}>AI 어시스턴트 <span className={`${styles.tag} ${styles.tagPink}`}>읽기 전용</span></div>
      <div className={styles.pdesc}>자연어로 관리 데이터를 조회합니다 · POST /api/admin/ai/chat</div>

      <div className={styles.note}>
        조회 전용(v1) 어시스턴트예요. 회원 정지, 상품 삭제 같은 변경 작업은 처리할 수 없고,
        그런 요청을 하면 AI가 권한이 없다고 안내해드려요.
      </div>

      <div className={styles.chatWrap}>
        <div className={styles.chatLog} ref={logRef}>
          {messages.map(m => (
            <div
              key={m.id}
              className={`${styles.chatMsg}${m.role === 'user' ? ' ' + styles.user : ''}${m.role === 'error' ? ' ' + styles.error : ''}`}
            >
              <div className={styles.chatAvatar}>{m.role === 'user' ? '나' : 'AI'}</div>
              <div className={styles.chatBubble}>{m.text}</div>
            </div>
          ))}
          {sending && (
            <div className={styles.chatMsg}>
              <div className={styles.chatAvatar}>AI</div>
              <div className={styles.chatBubble}>답변을 생각하는 중...</div>
            </div>
          )}
        </div>

        <div className={styles.chatChips}>
          {SUGGESTED_QUESTIONS.map(q => (
            <button key={q} type="button" className={styles.chatChip} onClick={() => fillChip(q)}>
              {q}
            </button>
          ))}
        </div>

        <form className={styles.chatInputRow} onSubmit={handleSend}>
          <input
            placeholder="질문을 입력하세요… (예: 최근 신고 목록 보여줘)"
            value={input}
            onChange={e => setInput(e.target.value)}
            disabled={sending}
          />
          <button type="submit" className={styles.chatSendBtn} disabled={sending || !input.trim()}>
            전송
          </button>
        </form>
      </div>
    </>
  )
}
