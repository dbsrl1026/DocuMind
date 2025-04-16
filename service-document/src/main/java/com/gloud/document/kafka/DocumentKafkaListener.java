package com.gloud.document.kafka;

import com.gloud.document.dto.DocumentStatusUpdateMessage;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;

import com.gloud.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaListener {

    private final DocumentService documentService;

    @KafkaListener(topics = "document.completed", groupId = "document-status-handler")
    public void handleDocumentCompleted(DocumentStatusUpdateMessage message, Acknowledgment ack) {
        try {
            log.info("Received document.completed event: metadataId={} -> status={}", message.getMetadataId(), message.getStatus());

            Metadata metadata = documentService.getMetadata(message.getMetadataId());
            if (metadata == null) {
                log.warn("Metadata not found for ID {}", message.getMetadataId());
                ack.acknowledge(); // acknowledge even if skipped
                return;
            }

            if (metadata.getProcessingStatus() == message.getStatus()) {
                log.info("No status change needed for metadataId={}", message.getMetadataId());
                ack.acknowledge(); // acknowledge even if skipped
                return;
            }

            documentService.updateProcessingStatus(message.getMetadataId(), message.getStatus());
            ack.acknowledge(); // 성공 시 커밋
        } catch (Exception e) {
            log.error("Error processing document.completed message: {}", e.getMessage(), e);
            // 수동 커밋이므로 실패 시 재시도 가능
        }
    }
}