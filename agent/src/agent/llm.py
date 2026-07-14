"""qwen3:4b(Ollama) 클라이언트 팩토리.

모든 LLM 호출은 이 팩토리로 만든 클라이언트를 통해서만 한다(설정 분산 방지).
운용값은 기존 백엔드 admin.ai에서 실측 검증된 것을 계승한다:
- thinking 비활성: qwen3 계열은 thinking이 기본 켜져 최종 답변이 새어 content가 빈다 → 반드시 끈다.
- temperature 낮게(결정성), num_predict 상한(느린 CPU 추론의 최악 응답시간 bound),
  keep_alive(모델 상주로 콜드로드 제거).
"""

from langchain_ollama import ChatOllama

from agent.config import settings


def build_chat_model() -> ChatOllama:
    """설정에 맞춘 qwen3:4b ChatOllama 인스턴스를 만든다."""
    return ChatOllama(
        model=settings.ollama_model,
        base_url=settings.ollama_base_url,
        temperature=settings.agent_temperature,
        num_predict=settings.agent_num_predict,
        keep_alive=settings.agent_keep_alive,
        # qwen3 thinking 비활성 (langchain-ollama의 reasoning 플래그).
        reasoning=False,
    )
