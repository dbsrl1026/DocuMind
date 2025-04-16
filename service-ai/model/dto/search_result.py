from pydantic import BaseModel

class SearchResult(BaseModel):
    document_id: int
    filename: str
    preview: str
    similarity: float