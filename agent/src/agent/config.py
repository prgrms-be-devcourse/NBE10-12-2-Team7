"""에이전트 설정. 환경변수(.env)에서 로드한다. 값의 단일 소스."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # --- 생성 LLM (런타임 답변 · 클라우드 NVIDIA NIM · OpenAI 호환) ---
    # 포폴/학습 목적이라 로컬 qwen3:4b 대신 NIM으로 생성(질문이 클라우드로 감).
    nim_base_url: str = "https://integrate.api.nvidia.com/v1"
    nim_model: str = "meta/llama-3.1-8b-instruct"
    nim_api_key: str = ""                 # .env에만 — 커밋 금지
    agent_temperature: float = 0.1        # 생성 결정성
    agent_num_predict: int = 1024         # 최대 토큰(4단 답변이 512에 잘려 상향)

    # --- 서버 ---
    agent_host: str = "0.0.0.0"
    agent_port: int = 8000

    # --- 임베딩 · 벡터스토어 (로컬 Ollama bge-m3, 인제스트 + 런타임 검색 공유) ---
    ollama_base_url: str = "http://localhost:11434"
    embed_model: str = "bge-m3"           # Ollama 임베딩 모델
    chroma_path: str = "data/chroma"      # Chroma 영속 디렉터리
    chroma_collection: str = "minsa_legal"
    retrieval_top_k: int = 5
    retrieval_min_score: float = 0.5      # score=1-distance 기준. 실측: 관련 0.62+, 무관 0.54↓

    # --- 인제스트 경로 ---
    raw_dir: str = "data/raw"
    cache_dir: str = ".cache"             # 필터 판정 캐시(멱등·resumable)


settings = Settings()
