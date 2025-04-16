package com.gloud.document.kafka;

import com.gloud.document.dto.DocumentKafkaMessage;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.repository.MetadataRepository;
import com.gloud.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaProducer {

    private final KafkaTemplate<String, DocumentKafkaMessage> kafkaTemplate;
    private final MetadataRepository metadataRepository;

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
        Optional<Metadata> optional = metadataRepository.findById(message.getMetadataId());
        if (optional.isEmpty()) {
            log.warn("Metadata not found for ID: {}", message.getMetadataId());
            return;
        }
        Metadata metadata = optional.get();
        metadata.setProcessingStatus(ProcessingStatus.FAILED);
        metadataRepository.save(metadata);
        log.info("Metadata ID {} updated to status: {}", metadata.getMetadataId(), ProcessingStatus.FAILED);
    }
}
