package com.treasury.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_plans")
public class CashPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CashPlanType type;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 64)
    private String organizationName;

    @Column(length = 240)
    private String description;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected CashPlan() {
    }

    public CashPlan(LocalDate planDate, CashPlanType type, String category, BigDecimal amount,
                    String organizationName, String description, String createdBy) {
        this.planDate = planDate;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.organizationName = organizationName;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public CashPlanType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
