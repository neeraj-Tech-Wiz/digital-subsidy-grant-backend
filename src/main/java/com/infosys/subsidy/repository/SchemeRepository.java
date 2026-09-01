package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    Optional<Scheme> findBySchemeCode(String schemeCode);

    Optional<Scheme> findBySchemeName(String schemeName);

    boolean existsBySchemeCode(String schemeCode);

    boolean existsBySchemeName(String schemeName);

    List<Scheme> findByStatus(SchemeStatus status);

    List<Scheme> findByBeneficiaryCategory(BeneficiaryCategory category);

    List<Scheme> findByApplicableRegionContainingIgnoreCase(String region);

    @Query("SELECT s FROM Scheme s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(s.schemeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.schemeCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR s.beneficiaryCategory = :category) AND " +
           "(:region IS NULL OR :region = '' OR LOWER(s.applicableRegion) LIKE LOWER(CONCAT('%', :region, '%'))) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<Scheme> filterSchemes(@Param("keyword") String keyword,
                               @Param("category") BeneficiaryCategory category,
                               @Param("region") String region,
                               @Param("status") SchemeStatus status);
}
