from whoosh.index import create_in, open_dir
from whoosh.fields import Schema, TEXT, ID
from whoosh.qparser import QueryParser
from config import settings
import os

# Step 1: Create and build BM25 index using Whoosh
def build_bm25_index(documents, index_dir=settings.bm25_index_path):
    schema = Schema(doc_id=ID(stored=True), content=TEXT)
    if not os.path.exists(index_dir):
        os.mkdir(index_dir)
        ix = create_in(index_dir, schema)
    else:
        ix = open_dir(index_dir)

    writer = ix.writer()
    for doc in documents:
        writer.add_document(doc_id=str(doc["document_id"]), content=doc["page_content"])
    writer.commit()


# Step 2: BM25 검색 결과를 반환하는 함수


def get_bm25_doc_ids(query, index_dir=settings.bm25_index_path, top_k=5):
    ix = open_dir(index_dir)
    with ix.searcher() as searcher:
        parser = QueryParser("content", ix.schema)
        parsed_query = parser.parse(query)
        results = searcher.search(parsed_query, limit=top_k)
        return [int(hit["doc_id"]) for hit in results]


# Step 3: 기존 FastAPI search_documents 라우터에 통합 (중요 부분만)

def rerank_with_bm25_and_vector(search_result, bm25_doc_ids, query, top_k):
    seen_doc_ids = set()
    reranked = []

    for point in search_result:
        payload = point.payload or {}
        metadata = payload.get("metadata", {})
        doc_id = metadata.get("document_id", -1)

        if doc_id in seen_doc_ids:
            continue
        seen_doc_ids.add(doc_id)

        filename = metadata.get("filename", "unknown")
        content = payload.get("page_content", "")
        preview = content[:100].replace("\n", " ")

        score = point.score
        if query.lower() in content.lower():
            score += 0.05  # soft rerank
        if doc_id in bm25_doc_ids:
            score += 0.05  # hybrid rerank

        reranked.append({
            "document_id": doc_id,
            "filename": filename,
            "preview": preview,
            "score": score
        })

    reranked.sort(key=lambda x: x["score"], reverse=True)
    return reranked[:top_k]
