from kafka import KafkaConsumer
import json
from model.dto import DocumentKafkaMessage
from vector import index_text_to_qdrant
from config import settings

def consume_kafka():
    indexed_documents = set()
    consumer = KafkaConsumer(
        settings.kafka_consume_topic,
        bootstrap_servers=[settings.kafka_broker],
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        auto_offset_reset='latest',
        enable_auto_commit=False,
        group_id="ai-service-consumer",
    )

    print(f"Listening to Kafka topic: {settings.kafka_consume_topic}")

    for message in consumer:
        try:
            msg = DocumentKafkaMessage(**message.value)
            print(f"[Kafka] Received: {msg.originalFilename} from {msg.email}")

            if msg.metadataId in indexed_documents:
                print(f"[Kafka] Skipping duplicate document ID: {msg.metadataId}")
                continue
            indexed_documents.add(msg.metadataId)

            metadata = {
                "document_id": msg.metadataId,
                "email": msg.email,
                "filename": msg.originalFilename,
                "upload_time": str(msg.uploadTime),
            }

            index_text_to_qdrant(msg.textContent, metadata)

            print(f"[Qdrant] Document {msg.metadataId} indexed.")
            consumer.commit()

        except Exception as e:
            print(f"[Error] Failed to process message: {e}")