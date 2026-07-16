"""인제스트 필터용 LLM 클라이언트 (NIM, OpenAI 호환).

런타임 답변 생성(llm.py, 로컬 qwen3:4b)과 분리 — 이건 오프라인 인제스트 전용(클라우드).
judge_relevance: 텍스트가 중고거래 민사 분쟁에 유용한지 (relevant, reason)으로 판정.
base_url만 바꾸면 다른 OpenAI 호환 프로바이더로 교체 가능.
"""

from __future__ import annotations

import json

from openai import OpenAI

from agent.config import settings

_client = OpenAI(base_url=settings.filter_base_url, api_key=settings.filter_api_key)

_SYSTEM = (
    "너는 중고거래(C2C) 플랫폼의 법률 지식 큐레이터다. "
    "주어진 법률 Q&A가 '개인 간 중고거래에서 생기는 민사 분쟁'(계약·환불·하자·미배송·사기 등)에 "
    "실제로 도움이 되는지 판정한다. 형사·행정·부동산등기·집행절차 등 거래와 무관하면 관련 없음이다. "
    'JSON만 출력: {"relevant": true 또는 false, "reason": "한 줄 이유"}'
)


def judge_relevance(text: str) -> tuple[bool, str]:
    """텍스트의 거래 관련성을 NIM LLM으로 판정 → (relevant, reason)."""
    resp = _client.chat.completions.create(
        model=settings.filter_model,
        temperature=settings.filter_temperature,
        messages=[
            {"role": "system", "content": _SYSTEM},
            {"role": "user", "content": text[:1500]},
        ],
    )
    content = resp.choices[0].message.content or ""
    return _parse(content)


def _parse(content: str) -> tuple[bool, str]:
    try:
        start = content.index("{")
        end = content.rindex("}") + 1
        data = json.loads(content[start:end])
        return bool(data.get("relevant", False)), str(data.get("reason", ""))
    except (ValueError, json.JSONDecodeError):
        return False, "parse_failed"
