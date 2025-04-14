package com.gloud.document.entity;

import com.gloud.document.enums.ContentType;
import com.gloud.document.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "tb_metadata")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "metadataId")
public class Metadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_id", nullable = false)
    private Long metadataId;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    // file_path 있으면 필요없음
//    @Column(name = "new_filename", length = 255, nullable = false)
//    private String newFilename;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "preview_path", length = 255)
    private String previewPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 10)
    private ContentType contentType;

    @Column(name = "uploader_email", nullable = false, length = 64)
    private String uploaderEmail;

    // AI 분석 파이프라인 처리 여부. PENDING : 보류 중, COMPLETED : 처리 완료. FAILED : 실패, PROCESSING 처리 중
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 16)
    private ProcessingStatus processingStatus;

    @PrePersist
    protected void onCreate() {
        if (this.uploadTime == null) {
            this.uploadTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
        if (this.processingStatus == null) {
            this.processingStatus = ProcessingStatus.PENDING;
        }
    }

}
