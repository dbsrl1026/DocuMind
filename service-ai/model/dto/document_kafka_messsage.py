from pydantic import BaseModel
from datetime import datetime

class DocumentKafkaMessage(BaseModel):
    metadataId: int
    email: str
    originalFilename: str
    contentType: str
    textContent: str
    uploadTime: datetime