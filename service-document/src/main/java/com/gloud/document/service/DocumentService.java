package com.gloud.document.service;


import com.gloud.document.dto.DeleteResultDto;
import com.gloud.document.dto.DocumentDetailDto;
import com.gloud.document.dto.MetadataDto;
import com.gloud.document.dto.UploadResult;
import com.gloud.document.enums.ProcessingStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;

public interface DocumentService {


    String uploadFile(MultipartFile file, String email);

    List<UploadResult> uploadMultipleFiles(List<MultipartFile> files, String email);

    Page<MetadataDto> getDocuments(String email,int page, int size, String sortBy, String direction,
                                   ProcessingStatus status, String contentType,
                                   LocalDate startDate, LocalDate endDate);

    void deleteDocument(Long metadataId, String email);

    DeleteResultDto deleteDocuments(List<Long> metadataIds, String email);

    DocumentDetailDto getDocumentDetail(Long metadataId, String email);

    ResponseEntity<Resource> downloadFile(Long metadataId, String email);

    void updateProcessingStatus(Long metadataId, ProcessingStatus status);
}
