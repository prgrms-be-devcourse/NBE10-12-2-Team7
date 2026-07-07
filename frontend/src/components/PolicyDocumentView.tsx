import type { PolicyDocument } from '@/data/policies'
import styles from './PolicyDocumentView.module.css'

/**
 * 정책 문서(PolicyDocument) 데이터를 제목/소제목/본문 구조로 그대로 렌더링한다.
 * 모달, 별도 정책 페이지 등 어디서든 재사용할 수 있도록 문서 데이터와 렌더링을 분리했다.
 */
export default function PolicyDocumentView({ doc }: { doc: PolicyDocument }) {
  return (
    <div className={styles.doc}>
      <div className={styles.meta}>버전 {doc.version} · 시행일 {doc.effectiveDate}</div>

      {doc.intro?.map((paragraph, i) => (
        <p key={i} className={styles.intro}>{paragraph}</p>
      ))}

      {doc.sections.map((section, i) => (
        <section key={i} className={styles.section}>
          <h3 className={styles.heading}>{section.heading}</h3>

          {section.paragraphs?.map((paragraph, j) => (
            <p key={j} className={styles.paragraph}>{paragraph}</p>
          ))}

          {section.list && (
            <ul className={styles.list}>
              {section.list.map((item, j) => <li key={j}>{item}</li>)}
            </ul>
          )}

          {section.table && (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    {section.table.headers.map((header, j) => <th key={j}>{header}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {section.table.rows.map((row, j) => (
                    <tr key={j}>
                      {row.map((cell, k) => <td key={k}>{cell}</td>)}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ))}
    </div>
  )
}
