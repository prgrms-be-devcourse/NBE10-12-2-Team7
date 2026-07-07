'use client'

import { memo, useCallback, useEffect, useState } from 'react'
import PolicyDocumentView from '@/components/PolicyDocumentView'
import { PRIVACY_COLLECTION_CONSENT, PRIVACY_POLICY, TERMS_OF_SERVICE } from '@/data/policies'
import type { PolicyDocument } from '@/data/policies'
import styles from './AgreementSection.module.css'

type AgreementTarget = 'terms' | 'consent' | 'privacyPolicy'

// 체크박스 2개(이용약관/개인정보 수집·이용 동의) + 상시 열람용 개인정보처리방침 링크, 총 3개 문서를 같은 모달로 보여준다.
const AGREEMENT_CONTENT: Record<AgreementTarget, PolicyDocument> = {
  terms: TERMS_OF_SERVICE,
  consent: PRIVACY_COLLECTION_CONSENT,
  privacyPolicy: PRIVACY_POLICY,
}

type AgreementItemProps = {
  id: string
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
  onView: () => void
}

/**
 * "보기" 버튼은 label 밖의 형제 요소라 클릭해도 체크박스를 토글하지 않는다.
 * 그래도 구조가 바뀌어도 안전하도록 stopPropagation을 명시적으로 건다.
 */
const AgreementItem = memo(function AgreementItem({ id, label, checked, onChange, onView }: AgreementItemProps) {
  return (
    <div className={styles.itemRow}>
      <div className={styles.itemCheck}>
        <input
          id={id}
          type="checkbox"
          className={styles.checkbox}
          checked={checked}
          onChange={e => onChange(e.target.checked)}
        />
        <label htmlFor={id} className={styles.itemLabel}>
          <span className={styles.req}>[필수]</span> {label}
        </label>
      </div>
      <button
        type="button"
        className={styles.viewBtn}
        onClick={e => { e.stopPropagation(); onView() }}
      >
        보기 <span className={styles.chevron}>›</span>
      </button>
    </div>
  )
})

type AgreementModalProps = {
  target: AgreementTarget
  onClose: () => void
}

/** 오버레이 클릭/ESC/닫기 버튼 세 가지 모두로 닫을 수 있는 스크롤 가능한 약관 본문 모달. */
const AgreementModal = memo(function AgreementModal({ target, onClose }: AgreementModalProps) {
  const doc = AGREEMENT_CONTENT[target]

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="agreement-modal-title"
        onClick={e => e.stopPropagation()}
      >
        <div className={styles.modalHead}>
          <h2 id="agreement-modal-title" className={styles.modalTitle}>{doc.title}</h2>
          <button type="button" className={styles.modalClose} aria-label="닫기" onClick={onClose}>✕</button>
        </div>
        <div className={styles.modalBody}>
          <PolicyDocumentView doc={doc} />
        </div>
      </div>
    </div>
  )
})

export type AgreementSectionProps = {
  termsAgreed: boolean
  personalInfoCollectionAgreed: boolean
  onTermsChange: (checked: boolean) => void
  onPersonalInfoCollectionChange: (checked: boolean) => void
  errorText?: string
}

function AgreementSection({
  termsAgreed,
  personalInfoCollectionAgreed,
  onTermsChange,
  onPersonalInfoCollectionChange,
  errorText,
}: AgreementSectionProps) {
  // allAgreed는 별도 state가 아니라 termsAgreed/personalInfoCollectionAgreed의 파생값이다.
  // useEffect로 두 state를 동기화하는 방식은 클릭 한 번에 커밋이 두 번(렌더 2회) 일어나
  // 체감 버벅거림의 원인이 되므로 쓰지 않는다.
  const allAgreed = termsAgreed && personalInfoCollectionAgreed

  // "보기" 모달 열림 상태는 이 컴포넌트 안에서만 관리한다. SignupPage로 올리면
  // 모달을 열고 닫을 때마다 이메일/비밀번호 입력까지 포함한 폼 전체가 리렌더 대상이 된다.
  const [modalTarget, setModalTarget] = useState<AgreementTarget | null>(null)

  // AgreementItem/버튼에 넘기는 핸들러는 useCallback으로 참조를 고정해, 형제 항목이
  // 다른 항목의 체크 변경 때문에 불필요하게 리렌더되지 않도록 한다(React.memo가 실제로 동작하려면
  // props 참조가 매 렌더 새로 생기면 안 된다).
  const handleAllChange = useCallback((checked: boolean) => {
    onTermsChange(checked)
    onPersonalInfoCollectionChange(checked)
  }, [onTermsChange, onPersonalInfoCollectionChange])

  const openTermsModal = useCallback(() => setModalTarget('terms'), [])
  const openConsentModal = useCallback(() => setModalTarget('consent'), [])
  const openPrivacyPolicyModal = useCallback(() => setModalTarget('privacyPolicy'), [])
  const closeModal = useCallback(() => setModalTarget(null), [])

  return (
    <div className={styles.box}>
      <div className={styles.allRow}>
        <input
          id="agreement-all"
          type="checkbox"
          className={styles.checkbox}
          checked={allAgreed}
          onChange={e => handleAllChange(e.target.checked)}
        />
        <label htmlFor="agreement-all" className={styles.allLabel}>전체 동의하기</label>
      </div>

      <div className={styles.divider} />

      <AgreementItem
        id="agreement-terms"
        label="이용약관"
        checked={termsAgreed}
        onChange={onTermsChange}
        onView={openTermsModal}
      />
      <div className={styles.itemDivider} />
      <AgreementItem
        id="agreement-consent"
        label="개인정보 수집 및 이용 동의"
        checked={personalInfoCollectionAgreed}
        onChange={onPersonalInfoCollectionChange}
        onView={openConsentModal}
      />

      {errorText && <div className={styles.errorText}>{errorText}</div>}

      {/* 체크박스가 아닌 상시 열람용 고지 링크. 개인정보처리방침은 필수 동의 항목이 아니라
          법령상 항상 확인 가능해야 하는 상시 공개 문서라 별도 체크박스를 두지 않는다. */}
      <div className={styles.noticeRow}>
        <button type="button" className={styles.noticeLink} onClick={openPrivacyPolicyModal}>
          개인정보처리방침 보기 <span className={styles.chevron}>›</span>
        </button>
      </div>

      {modalTarget && <AgreementModal target={modalTarget} onClose={closeModal} />}
    </div>
  )
}

// termsAgreed/personalInfoCollectionAgreed/errorText가 그대로고 onChange 핸들러(setState) 참조도 고정이면
// 부모가 이메일/비밀번호 입력 때문에 리렌더돼도 이 컴포넌트는 다시 렌더링하지 않는다.
export default memo(AgreementSection)
