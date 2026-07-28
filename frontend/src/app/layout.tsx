import type { Metadata } from 'next'
import AuthBootstrap from '@/components/AuthBootstrap'
import Header from '@/components/Header'
import LegalChatWidget from '@/components/LegalChatWidget'
import './globals.css'

const SITE_URL = 'https://marketon.inyeon.io'
const OG_TITLE = 'MarketOn'
const OG_DESCRIPTION = '우리 동네에서 쉽고 따뜻하게 거래해요'

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: 'MarketON',
  description: '동네 중고 거래 플랫폼',
  openGraph: {
    title: OG_TITLE,
    description: OG_DESCRIPTION,
    url: SITE_URL,
    siteName: OG_TITLE,
    images: [{ url: '/og-image.png', width: 1200, height: 630 }],
    locale: 'ko_KR',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: OG_TITLE,
    description: OG_DESCRIPTION,
    images: ['/og-image.png'],
  },
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
        <AuthBootstrap />
        <Header />
        {children}
        <LegalChatWidget />
      </body>
    </html>
  )
}
