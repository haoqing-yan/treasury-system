package com.treasury.service;

import com.treasury.domain.CashPlan;
import com.treasury.dto.CashPlanDtos;
import com.treasury.repository.CashPlanRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashPlanService {

    private final CashPlanRepository repository;
    private final AuditService auditService;

    public CashPlanService(CashPlanRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CashPlanDtos.Response> list(LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now().plusDays(30) : to;
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        return repository.findByPlanDateBetweenOrderByPlanDateAsc(effectiveFrom, effectiveTo).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @Transactional
    public CashPlanDtos.Response create(CashPlanDtos.CreateRequest request, String username) {
        CashPlan plan = new CashPlan(request.planDate(), request.type(), request.category(), request.amount(),
                request.organizationName(), request.description(), username);
        repository.save(plan);
        auditService.record(username, "CREATE", "CASH_PLAN", plan.getId().toString(),
                "新增资金计划：" + plan.getCategory() + " " + plan.getAmount());
        return toResponse(plan);
    }

    public CashPlanDtos.Response toResponse(CashPlan plan) {
        return new CashPlanDtos.Response(
                plan.getId(), plan.getPlanDate(), plan.getType(), plan.getCategory(), plan.getAmount(),
                plan.getOrganizationName(), plan.getDescription(), plan.getCreatedBy(), plan.getCreatedAt()
        );
    }
}
