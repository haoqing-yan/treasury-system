package com.treasury.repository;

import com.treasury.domain.CashPlan;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashPlanRepository extends JpaRepository<CashPlan, Long> {
    List<CashPlan> findByPlanDateBetweenOrderByPlanDateAsc(LocalDate from, LocalDate to);
}
