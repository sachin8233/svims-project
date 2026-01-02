package com.svims.repository;

import com.svims.entity.Invoice;
import com.svims.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Invoice entity
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByVendor(Vendor vendor);
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status NOT IN ('PAID', 'REJECTED')")
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate BETWEEN :startDate AND :endDate")
    List<Invoice> findInvoicesByDateRange(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.isOverdue = true")
    List<Invoice> findAllOverdueInvoices();
    
    // Find invoices created by a specific user
    List<Invoice> findByCreatedBy(String createdBy);
}

