from pydantic import BaseModel
from datetime import datetime

class SearchResult(BaseModel):
    document_id: int
    filename: str
    preview: str
    similarity: float