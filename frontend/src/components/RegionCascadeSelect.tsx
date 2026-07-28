'use client'

import { useEffect, useRef, useState } from 'react'
import styles from './RegionCascadeSelect.module.css'

export interface RegionNode {
  regionId: number
  code: string
  level: number
  parentCode: string | null
  fullName: string
  displayName: string
}

interface Props {
  /** 현재 선택된 동(leaf) regionCode. 빈 문자열이면 미선택. */
  value: string
  /**
   * 수정 화면 등에서 이미 저장된 regionCode를 계단식 select에 복원할 때 사용.
   * regionCode 단건 조회 API가 없어 "서울특별시 강남구 역삼동" 같은 전체 이름을
   * 공백 기준으로 나눠 각 단계의 displayName과 매칭하는 방식으로 복원한다.
   */
  initialFullName?: string
  onChange: (code: string, fullName: string) => void
  disabled?: boolean
}

async function fetchRegions(parentCode?: string): Promise<RegionNode[]> {
  const qs = parentCode ? `?parentCode=${encodeURIComponent(parentCode)}` : ''
  const res = await fetch(`/api/regions${qs}`)
  const data = await res.json().catch(() => null)
  return data?.data ?? []
}

const STAGE_LABELS = ['시/도', '시/군/구', '동/읍/면']

export default function RegionCascadeSelect({ value, initialFullName, onChange, disabled }: Props) {
  const [stages, setStages] = useState<RegionNode[][]>([])
  const [selected, setSelected] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const resolvedInitialRef = useRef(false)

  useEffect(() => {
    let cancelled = false

    async function resolveInitial(level1: RegionNode[]) {
      const segments = (initialFullName ?? '').trim().split(/\s+/).filter(Boolean)
      const stagesAcc: RegionNode[][] = [level1]
      const selectedAcc: string[] = []
      let currentList = level1

      for (let i = 0; i < segments.length; i++) {
        const match =
          currentList.find(r => r.code === value) ??
          currentList.find(r => r.displayName === segments[i] || r.fullName === segments[i])
        if (!match) break
        selectedAcc.push(match.code)
        if (match.code === value) break

        const children = await fetchRegions(match.code)
        if (cancelled) return
        if (children.length === 0) break
        stagesAcc.push(children)
        currentList = children
      }

      if (cancelled) return
      setStages(stagesAcc)
      setSelected(selectedAcc)
      setLoading(false)
    }

    async function init() {
      const level1 = await fetchRegions()
      if (cancelled) return

      if (value && initialFullName && !resolvedInitialRef.current) {
        resolvedInitialRef.current = true
        await resolveInitial(level1)
        return
      }

      setStages([level1])
      setSelected([])
      setLoading(false)
    }

    init()
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 마운트 시 1회만 초기 목록/복원을 수행한다.
  }, [])

  async function handleSelect(stageIdx: number, code: string) {
    if (!code) {
      setSelected(prev => prev.slice(0, stageIdx))
      setStages(prev => prev.slice(0, stageIdx + 1))
      onChange('', '')
      return
    }

    const currentStageOptions = stages[stageIdx] ?? []
    const nextSelected = [...selected.slice(0, stageIdx), code]

    const children = await fetchRegions(code)
    setSelected(nextSelected)

    if (children.length === 0) {
      setStages(prev => prev.slice(0, stageIdx + 1))
      const names = nextSelected
        .map((c, i) => (i === stageIdx ? currentStageOptions : stages[i])?.find(r => r.code === c)?.displayName)
        .filter(Boolean)
      onChange(code, names.join(' '))
      return
    }

    setStages(prev => [...prev.slice(0, stageIdx + 1), children])
    onChange('', '')
  }

  return (
    <div className={styles.wrap}>
      {stages.map((options, i) => (
        <select
          key={i}
          className={styles.select}
          value={selected[i] ?? ''}
          disabled={disabled || loading}
          onChange={e => handleSelect(i, e.target.value)}
        >
          <option value="">{STAGE_LABELS[i] ?? '선택'}</option>
          {options.map(r => <option key={r.regionId} value={r.code}>{r.displayName}</option>)}
        </select>
      ))}
    </div>
  )
}
