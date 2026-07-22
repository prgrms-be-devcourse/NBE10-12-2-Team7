'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import styles from './MyMarketOnTabs.module.css'

const TABS = [
  { href: '/my-products', label: '내상품' },
  { href: '/favorites', label: '관심상품' },
]

export default function MyMarketOnTabs() {
  const pathname = usePathname()

  return (
    <div className={styles.wrap}>
      <h1 className={styles.title}>나의 마켓온</h1>
      <nav className={styles.tabs}>
        {TABS.map(tab => (
          <Link
            key={tab.href}
            href={tab.href}
            className={`${styles.tab}${pathname === tab.href ? ' ' + styles.on : ''}`}
          >
            {tab.label}
          </Link>
        ))}
      </nav>
    </div>
  )
}
