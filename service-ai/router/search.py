from fastapi import APIRouter, Query, Header, HTTPException
from model.dto import SearchResult
from langchain_openai import OpenAIEmbeddings
from langchain_qdrant import Qdrant
from qdrant_client import QdrantClient
from config import settings
from utils import verify_email_from_token
from vector import get_bm25_doc_ids, rerank_with_bm25_and_vector

router = APIRouter()

client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
embeddings = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)

qdrant = Qdrant(
    client=client,
    collection_name=settings.qdrant_collection_name,
    embeddings=embeddings
)


@router.get("/search", response_model=list[SearchResult])
def search_documents(
        query: str = Query(..., description="검색할 사용자 질의"),
        email: str = Query(..., description="검색할 대상 email"),
        authorization: str = Header(...),
        top_k: int = 5
):
    verify_email_from_token(authorization, email)

    bm25_doc_ids = get_bm25_doc_ids(query, top_k=top_k)

    # 임베딩 생성
    query_embedding = embeddings.embed_query(query)

    # Qdrant에서 직접 검색 (중첩 필드 필터 사용)
    search_result = client.search(
        collection_name=settings.qdrant_collection_name,
        query_vector=query_embedding,
        limit=top_k * 3,  # 중복 제거를 고려해 여유 있게 요청
        with_payload=True,
        score_threshold=None,
        query_filter={
            "must": [
                {
                    "key": "metadata.email",
                    "match": {
                        "value": email
                    }
                }
            ]
        }
    )

    reranked = rerank_with_bm25_and_vector(search_result, bm25_doc_ids, query, top_k)
    return [SearchResult(
        document_id=doc["document_id"],
        filename=doc["filename"],
        preview=doc["preview"],
        similarity=doc["score"]
    ) for doc in reranked]
