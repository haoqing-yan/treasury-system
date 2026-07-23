package com.treasury.service;

import com.treasury.domain.ExceptionCase;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.domain.ExceptionStatus;
import com.treasury.dto.ExceptionCaseDtos;
import com.treasury.repository.ExceptionCaseRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionCaseService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExceptionCaseRepository repository;
    private final AuditService auditService;

    public ExceptionCaseService(ExceptionCaseRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ExceptionCaseDtos.Response> list(String keyword, String category, String status, String type) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return repository.findAllByOrderByDetectedAtDesc().stream()
                .filter(item -> normalized.isBlank()
                        || item.getCaseNo().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.getDescription().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.getSourceReference().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equals(status))
                .filter(item -> category == null || category.isBlank() || item.getCategory().name().equals(category))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equals(type))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExceptionCaseDtos.Summary summary() {
        List<ExceptionCase> cases = repository.findAll();
        return new ExceptionCaseDtos.Summary(
                cases.size(),
                cases.stream().filter(item -> item.getCategory() == ExceptionCategory.BUSINESS).count(),
                cases.stream().filter(item -> item.getCategory() == ExceptionCategory.SYSTEM).count(),
                cases.stream().filter(item -> item.getStatus() == ExceptionStatus.OPEN).count(),
                cases.stream().filter(item -> item.getStatus() == ExceptionStatus.PROCESSING).count(),
                cases.stream().filter(item -> item.getStatus() == ExceptionStatus.RESOLVED).count(),
                cases.stream().filter(item -> item.getStatus() != ExceptionStatus.RESOLVED)
                        .filter(item -> item.getSeverity() == ExceptionSeverity.HIGH
                                || item.getSeverity() == ExceptionSeverity.CRITICAL).count()
        );
    }

    @PreAuthorize("hasAuthority('exception:handle')")
    @Transactional
    public ExceptionCaseDtos.Response claim(Long id, String username) {
        ExceptionCase item = get(id);
        item.claim(username);
        auditService.record(username, "CLAIM_EXCEPTION", "EXCEPTION_CASE", id.toString(),
                "认领异常工单 " + item.getCaseNo());
        return toResponse(item);
    }

    @PreAuthorize("hasAuthority('exception:handle')")
    @Transactional
    public ExceptionCaseDtos.Response resolve(Long id, String note, String username) {
        ExceptionCase item = get(id);
        item.resolve(username, note, isAdmin());
        auditService.record(username, "RESOLVE_EXCEPTION", "EXCEPTION_CASE", id.toString(),
                "解决异常工单 " + item.getCaseNo() + "：" + note);
        return toResponse(item);
    }

    @PreAuthorize("hasAuthority('exception:handle')")
    @Transactional
    public ExceptionCaseDtos.Response reopen(Long id, String reason, String username) {
        ExceptionCase item = get(id);
        item.reopen();
        auditService.record(username, "REOPEN_EXCEPTION", "EXCEPTION_CASE", id.toString(),
                "重新打开异常工单 " + item.getCaseNo() + "：" + reason);
        return toResponse(item);
    }

    @Transactional
    public ExceptionCase register(ExceptionCategory category, ExceptionCaseType type,
                                  ExceptionSeverity severity, String title,
                                  String description, String sourceType, String sourceId, String sourceReference) {
        return repository.findFirstBySourceTypeAndSourceIdAndStatusNotOrderByDetectedAtDesc(
                        sourceType, sourceId, ExceptionStatus.RESOLVED)
                .orElseGet(() -> repository.save(new ExceptionCase(
                        nextCaseNo(), category, type, severity, title, description,
                        sourceType, sourceId, sourceReference
                )));
    }

    @Transactional
    public void resolveBySource(String sourceType, String sourceId, String username, String note) {
        repository.findFirstBySourceTypeAndSourceIdAndStatusNotOrderByDetectedAtDesc(
                        sourceType, sourceId, ExceptionStatus.RESOLVED)
                .ifPresent(item -> {
                    if (item.getStatus() == ExceptionStatus.OPEN) {
                        item.claim(username);
                    }
                    item.resolve(username, note, true);
                    auditService.record(username, "RESOLVE_EXCEPTION", "EXCEPTION_CASE", item.getId().toString(),
                            "业务处理自动关闭异常工单 " + item.getCaseNo() + "：" + note);
                });
    }

    private ExceptionCase get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("异常工单不存在"));
    }

    private String nextCaseNo() {
        return "YC" + LocalDateTime.now().format(NUMBER_TIME) + String.format("%04d", RANDOM.nextInt(10_000));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public ExceptionCaseDtos.Response toResponse(ExceptionCase item) {
        return new ExceptionCaseDtos.Response(
                item.getId(), item.getCaseNo(), item.getCategory(), item.getType(),
                item.getSeverity(), item.getStatus(),
                item.getTitle(), item.getDescription(), item.getSourceType(), item.getSourceId(),
                item.getSourceReference(), item.getAssignee(), item.getResolution(), item.getDetectedAt(),
                item.getClaimedAt(), item.getResolvedAt()
        );
    }
}
