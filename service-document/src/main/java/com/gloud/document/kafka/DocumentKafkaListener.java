package com.gloud.document.kafka;

import com.gloud.document.dto.DocumentStatusUpdateMessage;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;

import com.gloud.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaListener {

    private final DocumentService documentService;

    @KafkaListener(topics = "document.completed", groupId = "document-status-handler")
    public void handleDocumentCompleted(DocumentStatusUpdateMessage message) {
        log.info("Received document.completed event: metadataId={} -> status={}", message.getMetadataId(), message.getStatus());
        documentService.updateProcessingStatus(message.getMetadataId(), message.getStatus());
    }
}