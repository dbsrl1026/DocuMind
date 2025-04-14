package com.gloud.document.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FailedDeleteInfo {
    private Long metadataId;
    private String reason;  // "권한 없음", "MinIO 삭제 실패", "DB 삭제 실패" 등
}