package com.svims.scheduler;

import com.svims.entity.Invoice;
import com.svims.repository.InvoiceRepository;
import com.svims.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler for invoice-related automated tasks
 * - Marks overdue invoices
 * - Escalates overdue invoices
 * - Sends due-date reminders (email integration can be added)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceScheduler {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    /**
     * Run daily at 9 AM to mark overdue invoices
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void markOverdueInvoices() {
        log.info("Starting scheduled task: Mark overdue invoices");
        try {
            invoiceService.markOverdueInvoices();
            log.info("Completed: Mark overdue invoices");
        } catch (Exception e) {
            log.error("Error in markOverdueInvoices scheduler", e);
        }
    }

    /**
     * Run daily at 10 AM to escalate overdue invoices
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void escalateOverdueInvoices() {
        log.info("Starting scheduled task: Escalate overdue invoices");
        try {
            invoiceService.escalateOverdueInvoices();
            log.info("Completed: Escalate overdue invoices");
        } catch (Exception e) {
            log.error("Error in escalateOverdueInvoices scheduler", e);
        }
    }

    /**
     * Run daily at 8 AM to send due-date reminders
     * Sends reminders for invoices due in next 3 days
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDueDateReminders() {
        log.info("Starting scheduled task: Send due-date reminders");
        try {
            LocalDate today = LocalDate.now();
            LocalDate reminderDate = today.plusDays(3);
            
            List<Invoice> upcomingInvoices = invoiceRepository.findInvoicesByDateRange(today, reminderDate);
            
            for (Invoice invoice : upcomingInvoices) {
                if (invoice.getStatus() != Invoice.InvoiceStatus.PAID && 
                    invoice.getStatus() != Invoice.InvoiceStatus.REJECTED) {
                    // TODO: Send email notification
                    log.info("Reminder: Invoice {} is due on {}", 
                            invoice.getInvoiceNumber(), invoice.getDueDate());
                }
            }
            
            log.info("Completed: Send due-date reminders. Found {} invoices", upcomingInvoices.size());
        } catch (Exception e) {
            log.error("Error in sendDueDateReminders scheduler", e);
        }
    }
}

