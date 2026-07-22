import MyMarketOnTabs from '@/components/MyMarketOnTabs'

export default function MyMarketOnLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <MyMarketOnTabs />
      {children}
    </>
  )
}
