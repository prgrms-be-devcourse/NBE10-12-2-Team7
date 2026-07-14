"""에이전트 설정. 환경변수(.env)에서 로드한다. 값의 단일 소스."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # --- Ollama (런타임 생성 · qwen3:4b) ---
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "qwen3:4b"
    agent_temperature: float = 0.1
    agent_num_predict: int = 512
    agent_keep_alive: str = "30m"

    # --- 서버 ---
    agent_host: str = "0.0.0.0"
    agent_port: int = 8000

    # --- 임베딩 · 벡터스토어 (인제스트 + 런타임 검색 공유) ---
    embed_model: str = "bge-m3"           # Ollama 임베딩 모델
    chroma_path: str = "data/chroma"      # Chroma 영속 디렉터리
    chroma_collection: str = "minsa_legal"
    retrieval_top_k: int = 5
    retrieval_min_score: float = 0.0      # 0=임계 off (실측 후 조정)

    # --- 인제스트 필터 LLM (오프라인 · 클라우드 · OpenAI 호환) ---
    # 프로바이더 교체는 base_url/model/key만 바꾸면 됨 (기본: NVIDIA NIM)
    filter_provider: str = "nvidia_nim"
    filter_base_url: str = "https://integrate.api.nvidia.com/v1"
    filter_model: str = "qwen/qwen2.5-72b-instruct"
    filter_api_key: str = ""              # .env에만 — 커밋 금지
    filter_temperature: float = 0.0

    # --- 인제스트 경로 ---
    raw_dir: str = "data/raw"
    cache_dir: str = ".cache"             # 필터 판정 캐시(멱등·resumable)


settings = Settings()
