package com.treasury.repository;

import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @Override
    @EntityGraph(attributePaths = "payerAccount")
    List<PaymentOrder> findAll();

    @EntityGraph(attributePaths = "payerAccount")
    List<PaymentOrder> findTop8ByOrderByCreatedAtDesc();

    long countByStatus(PaymentStatus status);

    @Query("""
            select count(p) > 0 from PaymentOrder p
            where p.payerAccount.id = :payerAccountId
              and p.payeeAccountNo = :accountNo
              and p.amount = :amount
              and p.createdAt >= :since
              and p.status <> com.treasury.domain.PaymentStatus.REJECTED
            """)
    boolean existsPotentialDuplicate(@Param("payerAccountId") Long payerAccountId,
                                     @Param("accountNo") String accountNo,
                                     @Param("amount") BigDecimal amount,
                                     @Param("since") LocalDateTime since);
}
