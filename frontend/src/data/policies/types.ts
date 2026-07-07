export type PolicyTable = {
  headers: string[]
  rows: string[][]
}

export type PolicySection = {
  heading: string
  paragraphs?: string[]
  list?: string[]
  table?: PolicyTable
}

export type PolicyDocument = {
  id: string
  title: string
  version: string
  effectiveDate: string
  /** 제목 아래, 첫 조항 전에 붙는 안내 문단(있을 때만) */
  intro?: string[]
  sections: PolicySection[]
}
