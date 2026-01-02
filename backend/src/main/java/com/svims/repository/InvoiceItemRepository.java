package com.svims.repository;

import com.svims.entity.Invoice;
import com.svims.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for InvoiceItem entity
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceOrderByItemOrderAsc(Invoice invoice);
    void deleteByInvoice(Invoice invoice);
}


