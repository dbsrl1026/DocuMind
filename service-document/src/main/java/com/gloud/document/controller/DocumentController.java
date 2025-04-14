package com.gloud.document.controller;

import com.gloud.document.annotation.ValidateEmailClaim;
import com.gloud.document.dto.DeleteResultDto;
import com.gloud.document.dto.DocumentDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.gloud.document.dto.MetadataDto;
import com.gloud.document.dto.UploadResult;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Tag(name = "문서 관리 API", description = "문서 업로드 및 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @ValidateEmailClaim
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email,
            @RequestHeader("Authorization") String token) {
        String objectName = documentService.uploadFile(file, email);
        return ResponseEntity.ok("Uploaded: " + objectName);
    }

    @ValidateEmailClaim
    @PostMapping("/upload/multiple")
    public ResponseEntity<List<UploadResult>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("email") String email) {

        List<UploadResult> uploadResults = documentService.uploadMultipleFiles(files, email);
        return ResponseEntity.ok(uploadResults);
    }

    @ValidateEmailClaim
    @Operation(summary = "문서 목록 조회", description = "업로드된 문서의 목록을 조회합니다. 페이징, 정렬, 상태/타입/날짜 필터링을 지원합니다.")
    @GetMapping
    public ResponseEntity<Page<MetadataDto>> getDocumentsByEmail(
            @Parameter(description = "조회하고자 하는 email", example = "0")
            @RequestParam String email,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 당 항목 수", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "정렬 기준 필드(uploadTime, originalFilename, contentType)", example = "uploadTime")
            @RequestParam(defaultValue = "uploadTime") String sortBy,

            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction,

            @Parameter(description = "처리 상태 필터 (PENDING, PROCESSING, COMPLETED, FAILED)", example = "PENDING")
            @RequestParam(required = false) ProcessingStatus status,

            @Parameter(description = "문서 타입 필터 (pdf, docx 등)", example = "pdf")
            @RequestParam(required = false) String contentType,

            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2025-04-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2025-04-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Page<MetadataDto> result = documentService.getDocuments(
                email, page, size, sortBy, direction, status, contentType, startDate, endDate);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/{metadataId}")
    @Operation(summary = "문서 상세 조회", description = "문서 메타데이터와 내부 내용을 조회합니다.")
    @ValidateEmailClaim
    public ResponseEntity<DocumentDetailDto> getDocumentDetail(@PathVariable Long metadataId,
                                                               @RequestParam String email) {
        DocumentDetailDto dto = documentService.getDocumentDetail(metadataId, email);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{metadataId}/download")
    @Operation(summary = "문서 다운로드", description = "문서 다운로드 기능을 제공합니다.")
    @ValidateEmailClaim
    public ResponseEntity<Resource> downloadFile(@PathVariable Long metadataId,
                                                 @RequestParam String email) {
        return documentService.downloadFile(metadataId, email);
    }

    @ValidateEmailClaim
    @Operation(summary = "문서 삭제", description = "파일과 메타데이터를 함께 삭제합니다.")
    @DeleteMapping("/{metadataId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long metadataId, @RequestParam String email) {
        documentService.deleteDocument(metadataId, email);
        return ResponseEntity.noContent().build();
    }

    @ValidateEmailClaim
    @Operation(summary = "복수 문서 삭제", description = "파일과 메타데이터를 함께 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<DeleteResultDto> deleteMultipleDocuments(
            @RequestBody List<Long> metadataIds,
            @RequestParam String email) {
        DeleteResultDto result = documentService.deleteDocuments(metadataIds, email);
        return ResponseEntity.ok(result);
    }

}