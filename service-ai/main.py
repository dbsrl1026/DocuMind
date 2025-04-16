from fastapi import FastAPI
from my_kafka import consume_kafka
from router import search_router

import threading

app = FastAPI()
app.include_router(search_router)

@app.on_event("startup")
def start_kafka_consumer():
    threading.Thread(target=consume_kafka, daemon=True).start()

@app.get("/")
def read_root():
    return {"message": "Hello from ai-service!"}

