package com.sevatyres.service;

import com.sevatyres.model.CompanyInfo;
import com.sevatyres.model.CompanyMember;
import com.sevatyres.repository.impl.JdbcCompanyRepository;

import java.util.List;

public class CompanyService {

    public static final String SETTING_INVOICE_TEMPLATE = "invoice_template";

    private final JdbcCompanyRepository repo = new JdbcCompanyRepository();

    public CompanyInfo getCompany() { return repo.getCompany(); }

    public void saveCompany(CompanyInfo info) {
        if (info.getCompanyName() == null || info.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        repo.saveCompany(info);
    }

    public List<CompanyMember> getMembers() { return repo.findMembers(); }

    public CompanyMember addMember(CompanyMember m) {
        if (m.getName() == null || m.getName().isBlank()) {
            throw new IllegalArgumentException("Member name is required.");
        }
        return repo.saveMember(m);
    }

    public void updateMember(CompanyMember m) { repo.updateMember(m); }

    public void deleteMember(int id) { repo.deleteMember(id); }

    public String getInvoiceTemplate() {
        return repo.getSetting(SETTING_INVOICE_TEMPLATE).orElse(defaultInvoiceTemplate());
    }

    public void saveInvoiceTemplate(String template) {
        repo.putSetting(SETTING_INVOICE_TEMPLATE,
                template != null ? template : defaultInvoiceTemplate());
    }

    public static String defaultInvoiceTemplate() {
        return String.join("\n",
                "{company_name}",
                "{company_address}",
                "{company_contact}",
                "",
                "INVOICE",
                "# {invoice_number}",
                "{status}",
                "",
                "Billed to",
                "{customer_name}",
                "{customer_id}",
                "{customer_address}",
                "{customer_phone}",
                "{customer_email}",
                "",
                "Invoice details",
                "Invoice date\t{invoice_date}",
                "Due date\t{due_date}",
                "Payment terms\tNet 14",
                "",
                "Line items",
                "{items}",
                "",
                "Subtotal\t{subtotal}",
                "Tax ({tax_label})\t{tax_amount}",
                "Total amount\t{total}",
                "Paid amount\t{paid}",
                "Remaining balance\t{remaining}",
                "",
                "Payment instructions",
                "Bank name\t{bank_name}",
                "Account number\t{bank_account}",
                "IFSC code\t{bank_ifsc}",
                "UPI ID\t{upi_id}",
                "",
                "Please use invoice number {invoice_number} as the payment reference.",
                "",
                "Thank you for your business. Contact us at {company_contact}.",
                "",
                "© {year} {company_name}. All rights reserved.",
                "Generated on {generated_on}"
        );
    }
}
