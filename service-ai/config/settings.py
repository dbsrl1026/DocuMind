from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    jwt_secret_key: str
    jwt_algorithm: str

    kafka_broker: str
    kafka_consume_topic: str
    kafka_produce_topic: str

    qdrant_host: str
    qdrant_port: int
    qdrant_collection_name: str

    openai_api_key: str
    openai_text_embedding_model: str = "text-embedding-ada-002"

    bm25_index_path: str

    class Config:
        env_file = ".env"

settings = Settings()