package com.gloud.document.kafka;

import com.gloud.document.dto.DocumentKafkaMessage;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaProducer {

    private final KafkaTemplate<String, DocumentKafkaMessage> kafkaTemplate;
    private final DocumentService documentService;

    private static final String TOPIC = "document.uploaded";

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void publish(DocumentKafkaMessage message) {
        kafkaTemplate.send(TOPIC, message);
        log.info("Kafka message published: metadataId={}, email={}", message.getMetadataId(), message.getEmail());
    }

    @Recover
    public void recover(Exception e, DocumentKafkaMessage message) {
        log.error("Kafka publish failed after retries: metadataId={}, reason={}", message.getMetadataId(), e.getMessage());
        documentService.updateProcessingStatus(message.getMetadataId(), ProcessingStatus.FAILED);
        // optional: persist to local DB for retry queue, or alarm
    }
}
