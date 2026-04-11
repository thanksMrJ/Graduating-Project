package com.ajing.medicalmultimodal.repo;

import com.ajing.medicalmultimodal.domain.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, String> {

    List<AnalysisRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AnalysisRecord> findAllByOrderByCreatedAtDesc();

    void deleteByUserId(Long userId);

    Optional<AnalysisRecord> findByIdAndUserId(String id, Long userId);
}
