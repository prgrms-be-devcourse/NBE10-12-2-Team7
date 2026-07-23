'use client'

import { useEffect, useState } from 'react'
import { mannerScoreBand, MANNER_SCORE_BAND_COLOR } from '@/lib/mannerScoreBand'
import styles from './MannerScoreBadge.module.css'

export default function MannerScoreBadge({ memberId }: { memberId: number }) {
  const [score, setScore] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    fetch(`/api/members/${memberId}/manner-score`)
      .then(res => res.ok ? res.json() : null)
      .then(data => { if (!cancelled) setScore(data?.data?.score ?? null) })
      .catch(() => {})
    return () => { cancelled = true }
  }, [memberId])

  if (score == null) return null

  const color = MANNER_SCORE_BAND_COLOR[mannerScoreBand(score)]

  return (
    <span className={styles.badge} style={{ background: `${color}22`, color }}>
      <span className={styles.therm}>🌡</span>
      {score.toFixed(1)}
    </span>
  )
}
