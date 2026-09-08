package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    List<ApplicationDocument> findByApplicationId(Long applicationId);
    Optional<ApplicationDocument> findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDesc(Long applicationId, DocumentType documentType);
}
