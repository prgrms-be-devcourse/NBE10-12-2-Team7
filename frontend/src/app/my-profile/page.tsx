'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { clearAccessToken } from '@/lib/auth'
import TradeHistorySection from '@/components/TradeHistorySection'
import styles from './page.module.css'

const NEW_PW_RE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{10,64}$/

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
  const [isEditingNickname, setIsEditingNickname] = useState(false)

  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [saving, setSaving] = useState(false)
  const [withdrawing, setWithdrawing] = useState(false)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [newPasswordHint, setNewPasswordHint] = useState<{ text: string; kind?: string }>({ text: '영문·숫자·특수문자를 모두 포함해 10~64자로 입력하세요.' })
  const [newPasswordConfirmHint, setNewPasswordConfirmHint] = useState<{ text: string; kind?: string }>({ text: '' })
  const [pwMsg, setPwMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [pwSaving, setPwSaving] = useState(false)

  const [regionOptions, setRegionOptions] = useState<RegionOption[]>([])
  const [selectedRegions, setSelectedRegions] = useState<string[]>([])
  const [regionModalOpen, setRegionModalOpen] = useState(false)
  const [regionQuery, setRegionQuery] = useState('')
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

  function openRegionModal() { setRegionModalOpen(true); setRegionQuery('') }
  function closeRegionModal() { setRegionModalOpen(false); setRegionQuery('') }

  function addSelectedRegion(region: string) {
    if (selectedRegions.length >= 2) { showToast('동네는 최대 2개까지 설정할 수 있어요'); return }
    if (selectedRegions.includes(region)) { showToast('이미 추가된 동네예요'); return }
    setSelectedRegions(prev => [...prev, region])
    closeRegionModal()
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

  function startEditingNickname() {
    setFormMsg(null)
    setNicknameHint({ text: '2~20자로 입력하세요. 다른 이웃에게 보여져요.' })
    setIsEditingNickname(true)
  }

  function cancelEditingNickname() {
    setNickname(member?.nickname ?? '')
    setFormMsg(null)
    setNicknameHint({ text: '2~20자로 입력하세요. 다른 이웃에게 보여져요.' })
    setIsEditingNickname(false)
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
      setIsEditingNickname(false)
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
    } finally {
      setSaving(false)
    }
  }

  function validateNewPassword() {
    if (!NEW_PW_RE.test(newPassword)) {
      setNewPasswordHint({ text: '영문·숫자·특수문자를 모두 포함해 10~64자로 입력하세요.', kind: 'err' })
      return false
    }
    setNewPasswordHint({ text: '사용 가능한 비밀번호입니다.', kind: 'ok' })
    return true
  }

  function validateNewPasswordConfirm() {
    if (newPasswordConfirm !== newPassword) {
      setNewPasswordConfirmHint({ text: '비밀번호가 일치하지 않습니다.', kind: 'err' })
      return false
    }
    setNewPasswordConfirmHint({ text: '비밀번호가 일치합니다.', kind: 'ok' })
    return true
  }

  async function handleChangePassword(e: React.FormEvent) {
    e.preventDefault()
    setPwMsg(null)

    if (!currentPassword) {
      setPwMsg({ text: '현재 비밀번호를 입력해주세요.', type: 'error' })
      return
    }
    const valid = [validateNewPassword(), validateNewPasswordConfirm()].every(Boolean)
    if (!valid) {
      setPwMsg({ text: '입력값을 다시 확인해주세요.', type: 'error' })
      return
    }

    setPwSaving(true)
    try {
      const res = await apiFetch('/api/members/me/password', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ currentPassword, newPassword }),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        setPwMsg({ text: data?.message ?? '비밀번호 변경 중 오류가 발생했습니다.', type: 'error' })
        return
      }
      showToast('비밀번호를 변경했어요.\n다시 로그인해주세요.')
      clearAccessToken()
      setTimeout(() => { window.location.href = '/login' }, 1200)
    } catch {
      setPwMsg({ text: '서버에 연결할 수 없습니다.', type: 'error' })
    } finally {
      setPwSaving(false)
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

  const regionSearchResults = useMemo(() => {
    const pool = regionOptions.filter(r => !selectedRegions.includes(r.name))
    const q = regionQuery.trim()
    return q ? pool.filter(r => r.name.includes(q)) : pool
  }, [regionOptions, selectedRegions, regionQuery])

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

      {/* 거래내역 */}
      <TradeHistorySection />

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
              disabled={!isEditingNickname}
              aria-invalid={nicknameHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(nicknameHint.kind)}>
              {nicknameHint.text || '2~20자로 입력하세요. 다른 이웃에게 보여져요.'}
            </div>
          </div>

          {isEditingNickname ? (
            <div style={{ display: 'flex', gap: 10 }}>
              <button
                type="button"
                className="btn ghost"
                style={{ flex: 1 }}
                onClick={cancelEditingNickname}
                disabled={saving}
              >
                취소
              </button>
              <button
                type="submit"
                className="btn"
                style={{ flex: 1 }}
                disabled={saving || nickname.trim() === member.nickname}
              >
                {saving ? '저장 중...' : '저장'}
              </button>
            </div>
          ) : (
            <button type="button" className="btn block" onClick={startEditingNickname}>
              수정하기
            </button>
          )}
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
          <button type="button" className={styles.addRegionBtn} onClick={openRegionModal}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6"><path d="M12 5v14M5 12h14" /></svg>
            동네 추가
          </button>
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
      </div>

      {/* 동네 추가 모달 */}
      {regionModalOpen && (
        <div className={styles.modalOverlay} onClick={closeRegionModal}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <div className={styles.modalHead}>
              <div>
                <div className={styles.modalTitle}>동네 추가</div>
                <div className={styles.modalDesc}>등록하고 싶은 동네를 검색해서 선택하세요.</div>
              </div>
              <button type="button" className={styles.modalClose} aria-label="닫기" onClick={closeRegionModal}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"><path d="M18 6L6 18M6 6l12 12" /></svg>
              </button>
            </div>

            <div className={styles.searchWrap}>
              <div className={styles.searchInputRow}>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="2.2"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4-4" /></svg>
                <input
                  placeholder="동네 이름을 검색하세요 (예: 강남구)"
                  autoFocus
                  value={regionQuery}
                  onChange={e => setRegionQuery(e.target.value)}
                />
              </div>
              <div className={styles.resultsList}>
                {regionSearchResults.map(r => (
                  <button key={r.regionId} type="button" className={styles.resultItem} onClick={() => addSelectedRegion(r.name)}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2.2"><path d="M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11z" /><circle cx="12" cy="10" r="2.4" fill="var(--primary)" stroke="none" /></svg>
                    {r.name}
                  </button>
                ))}
                {regionSearchResults.length === 0 && (
                  <div className={styles.noResults}>검색 결과가 없어요.</div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 비밀번호 변경 카드 */}
      <div className={styles.card}>
        <h2>비밀번호 변경</h2>
        {pwMsg && (
          <div className={[styles.formMsg, styles.show, styles[pwMsg.type]].join(' ')} role="alert">
            {pwMsg.text}
          </div>
        )}
        <form onSubmit={handleChangePassword} noValidate>
          <div className={styles.field}>
            <label htmlFor="currentPassword">현재 비밀번호<span style={{ color: 'var(--primary)', marginLeft: 3 }}>*</span></label>
            <input
              type="password"
              id="currentPassword"
              autoComplete="current-password"
              value={currentPassword}
              onChange={e => setCurrentPassword(e.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="newPassword">새 비밀번호<span style={{ color: 'var(--primary)', marginLeft: 3 }}>*</span></label>
            <input
              type="password"
              id="newPassword"
              autoComplete="new-password"
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              onBlur={validateNewPassword}
              aria-invalid={newPasswordHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(newPasswordHint.kind)}>{newPasswordHint.text}</div>
          </div>

          <div className={styles.field}>
            <label htmlFor="newPasswordConfirm">새 비밀번호 확인<span style={{ color: 'var(--primary)', marginLeft: 3 }}>*</span></label>
            <input
              type="password"
              id="newPasswordConfirm"
              autoComplete="new-password"
              value={newPasswordConfirm}
              onChange={e => setNewPasswordConfirm(e.target.value)}
              onBlur={validateNewPasswordConfirm}
              aria-invalid={newPasswordConfirmHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(newPasswordConfirmHint.kind)}>{newPasswordConfirmHint.text}</div>
          </div>

          <button type="submit" className="btn block" disabled={pwSaving}>
            {pwSaving ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
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
      </div>

      {/* 토스트 */}
      <div className={`toast${toastVisible ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
