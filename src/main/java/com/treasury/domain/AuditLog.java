package com.treasury.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String resourceType;

    @Column(length = 64)
    private String resourceId;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(nullable = false, length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime operatedAt;

    protected AuditLog() {
    }

    public AuditLog(String username, String action, String resourceType, String resourceId,
                    String detail, String ipAddress) {
        this.username = username;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.operatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getDetail() { return detail; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getOperatedAt() { return operatedAt; }
}
