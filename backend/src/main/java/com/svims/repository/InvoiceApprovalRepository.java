package com.svims.repository;

import com.svims.entity.Invoice;
import com.svims.entity.InvoiceApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InvoiceApproval entity
 */
@Repository
public interface InvoiceApprovalRepository extends JpaRepository<InvoiceApproval, Long> {
    List<InvoiceApproval> findByInvoice(Invoice invoice);
    List<InvoiceApproval> findByInvoiceAndApprovalLevel(Invoice invoice, Integer level);
    
    // Check if user has already approved this invoice at any level
    boolean existsByInvoiceAndApprovedBy(Invoice invoice, String approvedBy);
    
    // Check if a specific level has already been approved
    boolean existsByInvoiceAndApprovalLevel(Invoice invoice, Integer level);
}

