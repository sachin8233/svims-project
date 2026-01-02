package com.svims.repository;

import com.svims.entity.ApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ApprovalRule entity
 */
@Repository
public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {
    List<ApprovalRule> findByIsActiveTrueOrderByPriorityAsc();
    
    @Query("SELECT r FROM ApprovalRule r WHERE r.isActive = true " +
           "AND :amount >= r.minAmount AND :amount <= r.maxAmount " +
           "ORDER BY r.priority ASC")
    Optional<ApprovalRule> findApplicableRule(@Param("amount") BigDecimal amount);
}

