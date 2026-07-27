"""파이프라인 공통 데이터 계약. 모든 tool은 이 Document를 주고받는다."""

from dataclasses import dataclass, field


@dataclass
class Document:
    """정규화된 법률 지식 한 조각.

    text     : 임베딩·검색 대상 (질의응답=질문, 요약=요약문)
    metadata : 답변·원문 근거·출처 등 (검색 후 근거로 사용)
    """

    text: str
    metadata: dict = field(default_factory=dict)
