package com.treasury.repository;

import com.treasury.domain.ExceptionCase;
import com.treasury.domain.ExceptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionCaseRepository extends JpaRepository<ExceptionCase, Long> {
    List<ExceptionCase> findAllByOrderByDetectedAtDesc();

    Optional<ExceptionCase> findFirstBySourceTypeAndSourceIdAndStatusNotOrderByDetectedAtDesc(
            String sourceType, String sourceId, ExceptionStatus status);
}
