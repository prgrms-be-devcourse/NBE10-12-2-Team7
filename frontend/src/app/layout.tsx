import type { Metadata } from 'next'
import Header from '@/components/Header'
import './globals.css'

export const metadata: Metadata = {
  title: 'MarketON',
  description: '동네 중고 거래 플랫폼',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link
          rel="stylesheet"
          as="style"
          crossOrigin="anonymous"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css"
        />
      </head>
      <body>
        <Header />
        {children}
      </body>
    </html>
  )
}
