package com.treasury.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "exception_cases", uniqueConstraints =
        @UniqueConstraint(name = "uk_exception_case_no", columnNames = "case_no"))
public class ExceptionCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no", nullable = false, length = 32)
    private String caseNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionCaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionStatus status;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Column(name = "source_reference", nullable = false, length = 80)
    private String sourceReference;

    @Column(length = 64)
    private String assignee;

    @Column(length = 500)
    private String resolution;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime claimedAt;
    private LocalDateTime resolvedAt;

    @Version
    private Long version;

    protected ExceptionCase() {
    }

    public ExceptionCase(String caseNo, ExceptionCategory category, ExceptionCaseType type,
                         ExceptionSeverity severity, String title,
                         String description, String sourceType, String sourceId, String sourceReference) {
        this.caseNo = caseNo;
        this.category = category;
        this.type = type;
        this.severity = severity;
        this.status = ExceptionStatus.OPEN;
        this.title = title;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceReference = sourceReference;
        this.detectedAt = LocalDateTime.now();
    }

    public void claim(String username) {
        if (status == ExceptionStatus.RESOLVED) {
            throw new IllegalStateException("已解决工单不能认领");
        }
        if (status == ExceptionStatus.PROCESSING && !username.equals(assignee)) {
            throw new IllegalStateException("该异常已由 " + assignee + " 认领");
        }
        this.status = ExceptionStatus.PROCESSING;
        this.assignee = username;
        if (claimedAt == null) {
            this.claimedAt = LocalDateTime.now();
        }
    }

    public void resolve(String username, String note, boolean admin) {
        if (status != ExceptionStatus.PROCESSING) {
            throw new IllegalStateException("请先认领异常工单再提交处理结论");
        }
        if (!admin && !username.equals(assignee)) {
            throw new IllegalStateException("只能处理本人认领的异常工单");
        }
        this.status = ExceptionStatus.RESOLVED;
        this.assignee = username;
        this.resolution = note;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reopen() {
        if (status != ExceptionStatus.RESOLVED) {
            throw new IllegalStateException("只有已解决工单可以重新打开");
        }
        this.status = ExceptionStatus.OPEN;
        this.assignee = null;
        this.resolution = null;
        this.claimedAt = null;
        this.resolvedAt = null;
    }

    public Long getId() { return id; }
    public String getCaseNo() { return caseNo; }
    public ExceptionCategory getCategory() { return category; }
    public ExceptionCaseType getType() { return type; }
    public ExceptionSeverity getSeverity() { return severity; }
    public ExceptionStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getSourceReference() { return sourceReference; }
    public String getAssignee() { return assignee; }
    public String getResolution() { return resolution; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
