package com.gloud.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKafkaMessage {
    private Long metadataId;
    private String email;
    private String originalFilename;
    private String contentType;
    private String textContent;
    private String uploadTime;
}
