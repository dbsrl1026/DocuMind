package com.gloud.document.service;

import com.gloud.document.dto.*;
import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ContentType;
import com.gloud.document.enums.ProcessingStatus;
import com.gloud.document.kafka.DocumentKafkaProducer;
import com.gloud.document.minio.MinioProperties;
import com.gloud.document.repository.MetadataRepository;
import com.gloud.document.util.DocumentToPdfConverter;
import com.gloud.document.util.TextExtractionUtil;
import io.minio.*;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final MetadataRepository metadataRepository;
    private final DocumentKafkaProducer documentKafkaProducer;

    private static final int MAX_UPLOAD_COUNT = 20;

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx != -1 && idx < filename.length() - 1) ? filename.substring(idx + 1) : null;
    }

    @Override
    public String uploadFile(MultipartFile file, String email) {
        LocalDate now = LocalDate.now();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String objectName = String.format("documents/%s/%d/%02d/%s", email, now.getYear(), now.getMonthValue(), fileName);
        File previewPdf = null;
        String previewObjectName = null;

        try (InputStream is = file.getInputStream()) {
            // Validate content type and extension
            String extension = getExtension(file.getOriginalFilename());
            ContentType contentType = ContentType.fromExtension(extension)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + extension));
            String contentTypeHeader = contentType.getMimeType();

            if (contentTypeHeader == null || extension == null) {
                throw new IllegalArgumentException("Invalid file: content type or extension is missing.");
            }

            // 1. Upload original to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(contentTypeHeader)
                            .build()
            );

            // 2. Convert to preview PDF
            previewPdf = DocumentToPdfConverter.convertToPdf(contentType, file.getInputStream(), file.getOriginalFilename());
            previewObjectName = objectName.replaceFirst("documents/", "previews/").replaceAll("\\.[^.]+$", ".pdf");

            try (InputStream previewIs = new FileInputStream(previewPdf)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(previewObjectName)
                                .stream(previewIs, previewPdf.length(), -1)
                                .contentType("application/pdf")
                                .build()
                );
            }

            // 3. Save metadata
            Metadata metadata = Metadata.builder()
                    .originalFilename(file.getOriginalFilename())
                    .filePath(objectName)
                    .previewPath(previewObjectName)
                    .contentType(contentType)
                    .uploaderEmail(email)
                    .processingStatus(ProcessingStatus.PROCESSING)
                    .build();

            metadataRepository.save(metadata);

            try (InputStream extractIs = file.getInputStream()) {
                String text = TextExtractionUtil.extractText(contentType, extractIs);
                DocumentKafkaMessage message = DocumentKafkaMessage.builder()
                        .metadataId(metadata.getMetadataId())
                        .email(email)
                        .originalFilename(file.getOriginalFilename())
                        .contentType(contentType.name())
                        .textContent(text)
                        .uploadTime(metadata.getUploadTime())
                        .build();
                documentKafkaProducer.publish(message);
            } catch (Exception e) {
                log.warn("Kafka 메시지 발행 또는 텍스트 추출 실패: {}", e.getMessage());
            }

            return objectName;
        } catch (Exception e) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectName)
                                .build());
                if (previewObjectName != null) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(properties.getBucket())
                                    .object(previewObjectName)
                                    .build());
                }
            } catch (Exception removeEx) {
                e.addSuppressed(removeEx);
            }

            throw new RuntimeException("Upload failed. Rolled back both original and preview uploads.", e);
        } finally {
            if (previewPdf != null && previewPdf.exists()) {
                previewPdf.delete();
            }
        }
    }

    @Override
    public List<UploadResult> uploadMultipleFiles(List<MultipartFile> files, String email) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided.");
        }

        if (files.size() > MAX_UPLOAD_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files. Max allowed: " + MAX_UPLOAD_COUNT);
        }

        List<UploadResult> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String path = uploadFile(file, email);
                uploaded.add(UploadResult.builder()
                        .originalFilename(file.getOriginalFilename())
                        .success(true)
                        .filePath(path)
                        .build());
            } catch (Exception e) {
                uploaded.add(UploadResult.builder()
                        .originalFilename(file.getOriginalFilename())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return uploaded;
    }



    @Override
    public Page<MetadataDto> getDocuments(String email, int page, int size, String sortBy, String direction,
                                          ProcessingStatus status, String contentType,
                                          LocalDate startDate, LocalDate endDate) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                direction.equalsIgnoreCase("desc") ?
                        Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        Page<Metadata> metadataPage = metadataRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add((cb.equal(root.get("uploaderEmail"), email)));
            if (status != null) {
                predicates.add(cb.equal(root.get("processingStatus"), status));
            }

            if (contentType != null) {
                predicates.add(cb.equal(root.get("contentType"), ContentType.fromExtension(contentType)));
            }

            if (startDate != null && endDate != null) {
                predicates.add(cb.between(
                        root.get("uploadTime"),
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return metadataPage.map(MetadataDto::fromEntity);
    }

    @Override
    public void deleteDocument(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata with id: " + metadataId));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }

        String objectName = metadata.getFilePath(); // 또는 newFilename

        try {
            metadataRepository.deleteById(metadataId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete metadata: " + e.getMessage(), e);
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .build()
            );

            String previewPath = metadata.getPreviewPath();
            if (previewPath != null) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(properties.getBucket())
                                    .object(previewPath)
                                    .build());
                } catch (Exception e) {
                    log.warn("Preview file deletion failed for metadataId {}: {}", metadataId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("MinIO deletion failed for metadataId {}: {}", metadataId, e.getMessage());
        }
    }

    @Override
    public DeleteResultDto deleteDocuments(List<Long> metadataIds, String email) {
        List<Long> successIds = new ArrayList<>();
        List<FailedDeleteInfo> failed = new ArrayList<>();


        List<Metadata> metadataList = metadataRepository.findAllById(metadataIds);

        for (Metadata metadata : metadataList) {
            Long id = metadata.getMetadataId();

            if (!metadata.getUploaderEmail().equals(email)) {
                failed.add(new FailedDeleteInfo(id, "Unauthorized access"));
                continue;
            }

            try {
                metadataRepository.delete(metadata);
                successIds.add(id);
            } catch (Exception e) {
                failed.add(new FailedDeleteInfo(id, "RDB deletion failed: " + e.getMessage()));
                continue;
            }

            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(metadata.getFilePath())
                        .build());

                String previewPath = metadata.getPreviewPath();
                if (previewPath != null) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(previewPath)
                                .build());
                    } catch (Exception e) {
                        log.warn("Preview file deletion failed for metadataId {}: {}", id, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("MinIO deletion failed for metadataId {}: {}", id, e.getMessage());
            }
        }

        return DeleteResultDto.builder()
                .successIds(successIds)
                .failed(failed)
                .build();
    }

    @Override
    public DocumentDetailDto getDocumentDetail(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata"));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }

        String previewPath = metadata.getPreviewPath();
        String previewUrl = null;
        if (previewPath != null) {
            try {
                previewUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .bucket(properties.getBucket())
                                .object(previewPath)
                                .method(Method.GET)
                                .expiry(300)
                                .build());
            } catch (Exception e) {
                log.warn("Presigned URL 생성 실패: {}", e.getMessage());
                // 실패해도 전체 흐름은 유지
            }
        }

        return DocumentDetailDto.builder()
                .originalFilename(metadata.getOriginalFilename())
                .contentType(metadata.getContentType().name())
                .uploadTime(metadata.getUploadTime())
                .filePath(metadata.getFilePath())
                .previewPath(previewUrl)
                .build();
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long metadataId, String email) {
        Metadata metadata = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find metadata"));

        if (!metadata.getUploaderEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access");
        }


        String objectName = metadata.getFilePath();

        try {
            InputStream is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());

            byte[] fileBytes = IOUtils.toByteArray(is);  // Apache Commons IO


            String contentType = metadata.getContentType().getMimeType();
            String encodedFilename = URLEncoder.encode(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(new ByteArrayResource(fileBytes));

        } catch (Exception e) {
            throw new RuntimeException("file download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateProcessingStatus(Long metadataId, ProcessingStatus status) {
        Optional<Metadata> optional = metadataRepository.findById(metadataId);
        if (optional.isEmpty()) {
            log.warn("Metadata not found for ID: {}", metadataId);
            return;
        }
        Metadata metadata = optional.get();
        if (metadata.getProcessingStatus() == ProcessingStatus.COMPLETED) {
            log.info("이미 완료된 문서입니다. 재처리 생략.");
            return;
        }
        metadata.setProcessingStatus(status);
        metadataRepository.save(metadata);
        log.info("Metadata ID {} updated to status: {}", metadataId, status);
    }
}