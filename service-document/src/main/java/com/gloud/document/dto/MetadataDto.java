package com.gloud.document.dto;

import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.entity.Metadata;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "업로드된 문서 메타데이터 DTO")
public class MetadataDto {

    @Schema(description = "메타데이터ID", example = "1")
    private Long metadataId;

    @Schema(description = "사용자가 업로드한 원본 파일 이름", example = "report.pdf")
    private String originalFilename;

    @Schema(description = "MinIO에 저장된 파일 경로", example = "1712653827372_report.pdf")
    private String filePath;

    @Schema(description = "파일 확장자", example = "PDF")
    private String contentType;

    @Schema(description = "업로더 이메일", example = "user@example.com")
    private String uploaderEmail;

    @Schema(description = "업로드 시간", example = "2025-04-09T14:45:00")
    private LocalDateTime uploadTime;

    @Schema(description = "AI 분석 처리 상태", example = "PENDING")
    private ProcessingStatus processingStatus;

    public static MetadataDto fromEntity(Metadata metadata) {
        return MetadataDto.builder()
                .metadataId(metadata.getMetadataId())
                .originalFilename(metadata.getOriginalFilename())
                .filePath(metadata.getFilePath())
                .contentType(metadata.getContentType().name())
                .uploaderEmail(metadata.getUploaderEmail())
                .uploadTime(metadata.getUploadTime())
                .processingStatus(metadata.getProcessingStatus())
                .build();
    }
}