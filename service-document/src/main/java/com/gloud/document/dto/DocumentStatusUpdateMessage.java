package com.gloud.document.dto;

import com.gloud.document.enums.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatusUpdateMessage {
    private Long metadataId;
    private ProcessingStatus status;  // COMPLETED or FAILED
}