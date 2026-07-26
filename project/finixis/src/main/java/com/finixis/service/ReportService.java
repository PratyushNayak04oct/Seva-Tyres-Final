package com.finixis.service;

import com.finixis.model.GeneratedFile;
import com.finixis.model.Invoice;
import com.finixis.model.Transaction;
import com.finixis.repository.ReportRepository;
import com.finixis.repository.impl.JdbcReportRepository;
import com.finixis.viewmodel.FileGenerationService;

import java.io.IOException;
import java.util.List;

public class ReportService {

    private final ReportRepository repo;

    public ReportService() { this.repo = new JdbcReportRepository(); }

    public List<GeneratedFile> getAll() { return repo.findAll(); }

    public GeneratedFile saveFile(GeneratedFile gf) { return repo.save(gf); }

    /** Generate a Report PDF+Excel from all transactions, persist to DB. */
    public List<GeneratedFile> generateReport(TransactionService txnService) {
        try {
            List<Transaction> txns = txnService.getAll();
            List<GeneratedFile> files = FileGenerationService.generateReport(txns);
            files.forEach(repo::save);
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    /** Generate an Invoice PDF from credit transactions, persist to DB. */
    public List<GeneratedFile> createInvoice(TransactionService txnService) {
        try {
            List<Transaction> credits = txnService.getAllCredits();
            List<Invoice> invoices = credits.stream().map(t -> {
                Invoice inv = new Invoice();
                inv.setNumber("INV-" + t.getId());
                inv.setCustomerId(t.getCustomerId());
                inv.setCustomerName(t.getCustomerName());
                inv.setIssueDate(t.getDate());
                inv.setDueDate(t.getDate().plusDays(30));
                inv.setSubtotal(t.getAmount());
                inv.setTax(0);
                inv.setTotal(t.getAmount());
                return inv;
            }).toList();
            List<GeneratedFile> files = FileGenerationService.createInvoice(invoices);
            files.forEach(repo::save);
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create invoice", e);
        }
    }

    /** Export all transactions to PDF+Excel, persist to DB. */
    public List<GeneratedFile> exportTransactions(TransactionService txnService) {
        try {
            List<Transaction> txns = txnService.getAll();
            List<GeneratedFile> files = FileGenerationService.exportTransactions(txns);
            files.forEach(repo::save);
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Failed to export transactions", e);
        }
    }
}
