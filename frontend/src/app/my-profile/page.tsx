'use client'

import { useEffect, useRef, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { clearAccessToken } from '@/lib/auth'
import styles from './page.module.css'

type MsgType = 'success' | 'error'
type PageStatus = 'loading' | 'ready' | 'error'

interface Member {
  memberId: number
  email: string
  nickname: string
  role: string
  status: string
  createdAt: string
}

interface MemberLocation {
  region: string
  sortOrder: number
  active: boolean
}

interface RegionOption {
  regionId: number
  name: string
}

export default function MyProfilePage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')
  const [member, setMember] = useState<Member | null>(null)

  const [nickname, setNickname] = useState('')
  const [nicknameHint, setNicknameHint] = useState<{ text: string; kind?: string }>({ text: '2~20자로 입력하세요. 다른 이웃에게 보여져요.' })

  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [saving, setSaving] = useState(false)
  const [withdrawing, setWithdrawing] = useState(false)

  const [regionOptions, setRegionOptions] = useState<RegionOption[]>([])
  const [selectedRegions, setSelectedRegions] = useState<string[]>([])
  const [addRegion, setAddRegion] = useState('')
  const [locMsg, setLocMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [locSaving, setLocSaving] = useState(false)

  const [toastText, setToastText]       = useState('')
  const [toastVisible, setToastVisible] = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showToast(msg: string) {
    setToastText(msg)
    setToastVisible(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastVisible(false), 1900)
  }

  useEffect(() => {
    let cancelled = false
    apiFetch('/api/members/me')
      .then(async res => {
        const data = await res.json().catch(() => null)
        if (!res.ok) throw new Error(data?.message ?? '회원 정보를 불러오지 못했습니다.')
        if (!cancelled) {
          setMember(data?.data)
          setNickname(data?.data?.nickname ?? '')
          setStatus('ready')
        }
      })
      .catch(err => {
        if (cancelled) return
        setErrorMsg(err instanceof Error ? err.message : '회원 정보를 불러오지 못했습니다.')
        setStatus('error')
      })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    let cancelled = false
    Promise.all([
      apiFetch('/api/members/me/locations').then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })),
      fetch('/api/regions').then(r => r.json()).catch(() => null),
    ]).then(([locRes, regionsRes]) => {
      if (cancelled) return
      if (locRes.ok) {
        const list: MemberLocation[] = locRes.data?.data ?? []
        setSelectedRegions(list.slice().sort((a, b) => a.sortOrder - b.sortOrder).map(l => l.region))
      }
      setRegionOptions(regionsRes?.data ?? [])
    }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  function addSelectedRegion() {
    if (!addRegion) return
    if (selectedRegions.length >= 2) { showToast('동네는 최대 2개까지 설정할 수 있어요'); return }
    if (selectedRegions.includes(addRegion)) { showToast('이미 추가된 동네예요'); return }
    setSelectedRegions(prev => [...prev, addRegion])
    setAddRegion('')
  }

  function removeSelectedRegion(region: string) {
    setSelectedRegions(prev => prev.filter(r => r !== region))
  }

  function makePrimary(region: string) {
    setSelectedRegions(prev => [region, ...prev.filter(r => r !== region)])
  }

  async function saveLocations() {
    setLocMsg(null)
    if (selectedRegions.length === 0) {
      setLocMsg({ text: '동네를 1개 이상 설정해주세요.', type: 'error' })
      return
    }
    setLocSaving(true)
    try {
      const res = await apiFetch('/api/members/me/locations', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ regions: selectedRegions }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setLocMsg({ text: data?.message ?? '동네 설정 중 오류가 발생했습니다.', type: 'error' })
        return
      }
      const list: MemberLocation[] = data?.data ?? []
      setSelectedRegions(list.slice().sort((a, b) => a.sortOrder - b.sortOrder).map(l => l.region))
      showToast('동네를 설정했어요')
    } catch {
      setLocMsg({ text: '서버에 연결할 수 없습니다.', type: 'error' })
    } finally {
      setLocSaving(false)
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)

    const nv = nickname.trim()
    if (nv.length < 2 || nv.length > 20) {
      setNicknameHint({ text: '닉네임은 2~20자로 입력하세요.', kind: 'err' })
      setFormMsg({ text: '입력값을 다시 확인해주세요.', type: 'error' })
      return
    }
    setNicknameHint({ text: '', kind: 'ok' })

    setSaving(true)
    try {
      const res = await apiFetch('/api/members/me', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname: nv }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '수정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
        return
      }
      setMember(data?.data)
      setFormMsg({ text: '회원 정보를 수정했어요.', type: 'success' })
      showToast('수정 완료')
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
    } finally {
      setSaving(false)
    }
  }

  async function handleWithdraw() {
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return

    setWithdrawing(true)
    try {
      const res = await apiFetch('/api/members/me', { method: 'DELETE' })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '탈퇴 처리 중 오류가 발생했습니다.')
        setWithdrawing(false)
        return
      }
      clearAccessToken()
      showToast('탈퇴 처리되었습니다')
      setTimeout(() => { window.location.href = '/login' }, 1200)
    } catch {
      showToast('서버에 연결할 수 없습니다.')
      setWithdrawing(false)
    }
  }

  const hintClass = (kind?: string) =>
    [styles.hint, kind ? styles[kind] : ''].filter(Boolean).join(' ')

  const msgClass = formMsg
    ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ')
    : styles.formMsg

  if (status === 'loading') {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <h1>내 정보</h1>
          <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
        </div>
        <div className={styles.notice}><p>불러오는 중...</p></div>
      </main>
    )
  }

  if (status === 'error' || !member) {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <h1>내 정보</h1>
          <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
        </div>
        <div className={styles.notice}><p>{errorMsg}</p></div>
      </main>
    )
  }

  return (
    <main className={styles.wrap}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <h1>내 정보</h1>
        <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
      </div>

      {/* 프로필 요약 */}
      <div className={styles.profile}>
        <div className={styles.avatar}>{member.nickname.charAt(0)}</div>
        <div className={styles.who}>
          <div className={styles.nm}>{member.nickname}</div>
          <div className={styles.em}>{member.email}</div>
          <div className={styles.st}>
            <span className="tag active">{member.status === 'ACTIVE' ? '정상 회원' : member.status}</span>
          </div>
        </div>
      </div>

      {/* 프로필 수정 카드 */}
      <div className={styles.card}>
        <h2>프로필 수정</h2>
        <div className={msgClass} role="alert">
          {formMsg?.text}
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className={styles.field}>
            <label>이메일<span className={styles.lock}>🔒 변경 불가</span></label>
            <input type="email" value={member.email} disabled />
            <div className={styles.hint}>이메일은 계정 식별자로 변경할 수 없어요.</div>
          </div>

          <div className={styles.field}>
            <label htmlFor="nickname">
              닉네임<span style={{ color: 'var(--primary)', marginLeft: 3 }}>*</span>
            </label>
            <input
              type="text"
              id="nickname"
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              maxLength={20}
              aria-invalid={nicknameHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(nicknameHint.kind)}>
              {nicknameHint.text || '2~20자로 입력하세요. 다른 이웃에게 보여져요.'}
            </div>
          </div>

          <button type="submit" className="btn block" disabled={saving}>
            {saving ? '저장 중...' : '정보 수정'}
          </button>
          <div className={styles.apiNote}>GET /api/members/me · PATCH /api/members/me</div>
        </form>
      </div>

      {/* 내 동네 설정 카드 */}
      <div className={styles.card}>
        <h2>내 동네 설정</h2>
        <p className={styles.withdraw}>최대 2개까지 설정할 수 있어요. 첫 번째 동네가 대표 동네가 돼요.</p>

        {locMsg && (
          <div className={[styles.formMsg, styles.show, styles[locMsg.type]].join(' ')} role="alert">
            {locMsg.text}
          </div>
        )}

        {selectedRegions.length > 0 ? (
          <div className={styles.locList}>
            {selectedRegions.map((region, i) => (
              <div key={region} className={styles.locChip}>
                {i === 0 && <span className={styles.locPrimary}>대표</span>}
                <span className={styles.locName}>{region}</span>
                {i !== 0 && (
                  <button type="button" className={styles.locAction} onClick={() => makePrimary(region)}>
                    대표로
                  </button>
                )}
                <button
                  type="button"
                  className={styles.locRemove}
                  onClick={() => removeSelectedRegion(region)}
                  aria-label={`${region} 삭제`}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p className={styles.locEmpty}>설정된 동네가 없어요.</p>
        )}

        {selectedRegions.length < 2 && (
          <div className={styles.locAddRow}>
            <select
              value={addRegion}
              onChange={e => setAddRegion(e.target.value)}
              className={styles.locSelect}
              aria-label="동네 선택"
            >
              <option value="">동네 선택</option>
              {regionOptions.filter(r => !selectedRegions.includes(r.name)).map(r => (
                <option key={r.regionId} value={r.name}>{r.name}</option>
              ))}
            </select>
            <button type="button" className="btn ghost" onClick={addSelectedRegion} disabled={!addRegion}>
              추가
            </button>
          </div>
        )}

        <button
          type="button"
          className="btn block"
          onClick={saveLocations}
          disabled={locSaving}
          style={{ marginTop: 14 }}
        >
          {locSaving ? '저장 중...' : '동네 저장'}
        </button>
        <div className={styles.apiNote}>GET/PUT /api/members/me/locations · GET /api/regions</div>
      </div>

      {/* 계정 관리 카드 */}
      <div className={styles.card}>
        <h2>계정 관리</h2>
        <p className={styles.withdraw}>
          회원 탈퇴 시 계정 상태가 <b>탈퇴(DELETED)</b>로 변경되고 등록한 상품과 정보에 접근할 수
          없게 돼요. 이 작업은 되돌릴 수 없습니다.
        </p>
        <button className="btn danger" onClick={handleWithdraw} type="button" disabled={withdrawing}>
          {withdrawing ? '탈퇴 처리 중...' : '회원 탈퇴'}
        </button>
        <div className={styles.apiNote}>DELETE /api/members/me</div>
      </div>

      {/* 토스트 */}
      <div className={`toast${toastVisible ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
