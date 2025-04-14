package com.gloud.document.repository;

import com.gloud.document.entity.Metadata;
import com.gloud.document.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface MetadataRepository extends JpaRepository<Metadata, Long>, JpaSpecificationExecutor<Metadata> {

}