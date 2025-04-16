# /chat API for document-aware chatbot with multi-turn context support
from fastapi import APIRouter, Header
from pydantic import BaseModel
from typing import List, Tuple
from config import settings
from langchain.chains import ConversationalRetrievalChain
from langchain_openai import ChatOpenAI
from langchain_qdrant import Qdrant
from qdrant_client import QdrantClient
from model.dto import SearchResult
from utils import verify_email_from_token

router = APIRouter()

# LangChain LLM and Retriever setup
llm = ChatOpenAI(
    openai_api_key=settings.openai_api_key,
    model_name="gpt-3.5-turbo"
)
client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
retriever = Qdrant(
    client=client,
    collection_name=settings.qdrant_collection_name,
    embeddings=None
).as_retriever()

chat_chain = ConversationalRetrievalChain.from_llm(
    llm=llm,
    retriever=retriever,
    return_source_documents=True
)

# Request/Response Schema
class ChatRequest(BaseModel):
    query: str
    email: str
    history: List[Tuple[str, str]] = []

@router.post("/chat", response_model=List[SearchResult])
def chat_with_documents(payload: ChatRequest, authorization: str = Header(...)):
    verify_email_from_token(authorization, payload.email)

    # Run LLM + vector retrieval
    result = chat_chain.invoke({
        "question": payload.query,
        "chat_history": payload.history
    })

    seen = set()
    top_k = 5
    docs = []

    for doc in result["source_documents"]:
        metadata = doc.metadata or {}
        doc_id = metadata.get("document_id", -1)
        if doc_id in seen:
            continue
        seen.add(doc_id)
        docs.append(SearchResult(
            document_id=doc_id,
            filename=metadata.get("filename", "unknown"),
            preview=(doc.page_content or "")[:100].replace("\n", " "),
            similarity=1.0
        ))
        if len(docs) >= top_k:
            break

    return docs
