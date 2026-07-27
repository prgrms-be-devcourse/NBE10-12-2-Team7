"""ingest CLI — ETL 파이프라인 실행·검색 스모크·저장 현황 확인.

  python -m agent.ingest.cli ingest [--root PATH] [--batch-size N]
  python -m agent.ingest.cli search "중고 노트북 환불" -k 5
  python -m agent.ingest.cli stats
"""

from __future__ import annotations

import typer

from agent.config import settings
from agent.ingest import pipeline
from agent.ingest.tools import store

app = typer.Typer(help="마켓온 민사 법률 지식 인제스트 도구", no_args_is_help=True)


@app.command()
def ingest(
    root: str = typer.Option(None, help="원본 JSON 루트 (기본: settings.raw_dir)"),
    batch_size: int = typer.Option(128, help="임베딩·저장 배치 크기"),
) -> None:
    """원본을 적재→정규화→키워드필터→청킹→저장하고 단계별 건수를 출력한다."""
    result = pipeline.run_ingest(root=root, batch_size=batch_size)
    print("인제스트 완료:")
    for stage, n in result.items():
        print(f"  {stage:<11} {n:>8,}")
    if result["loaded"] == 0:
        print(f"⚠️  '{root or settings.raw_dir}'에서 읽은 레코드가 없습니다. 경로를 확인하세요.")


@app.command()
def search(
    query: str = typer.Argument(..., help="검색할 질문"),
    k: int = typer.Option(5, "-k", help="상위 k개"),
) -> None:
    """질문으로 유사 청크 top-k를 검색해 출력(검색 스모크)."""
    results = store.search(query, k=k)
    if not results:
        print("결과 없음 — 먼저 ingest로 데이터를 넣었는지 확인하세요.")
        return
    for i, r in enumerate(results, start=1):
        meta = r["metadata"]
        title = meta.get("title") or meta.get("doc_id") or "(제목 없음)"
        snippet = (meta.get("answer") or r["text"])[:80].replace("\n", " ")
        print(f"[{i}] dist={r['distance']:.3f} · {meta.get('category', '')} · {title}")
        print(f"    {snippet}")


@app.command()
def stats() -> None:
    """저장된 청크 수와 컬렉션 정보를 출력한다."""
    print(f"컬렉션 : {settings.chroma_collection}")
    print(f"경로   : {settings.chroma_path}")
    print(f"청크 수 : {store.count():,}")


if __name__ == "__main__":
    app()
