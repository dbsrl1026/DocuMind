package com.gloud.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadResult {
    private String originalFilename;
    private boolean success;
    private String filePath; // 성공한 경우 경로
    private String errorMessage; // 실패한 경우 메시지
}