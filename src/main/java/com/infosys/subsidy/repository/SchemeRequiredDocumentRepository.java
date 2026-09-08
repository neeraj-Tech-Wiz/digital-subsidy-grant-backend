package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.SchemeRequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeRequiredDocumentRepository extends JpaRepository<SchemeRequiredDocument, Long> {
    List<SchemeRequiredDocument> findBySchemeId(Long schemeId);
    List<SchemeRequiredDocument> findBySchemeIdAndActiveTrue(Long schemeId);
}
