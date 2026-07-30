package com.sevatyres.service;

import com.sevatyres.model.CompanyInfo;
import com.sevatyres.model.CompanyMember;
import com.sevatyres.repository.impl.JdbcCompanyRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CompanyService {

    /** Uploaded / active HTML invoice template stored in App_Setting. */
    public static final String SETTING_INVOICE_HTML = "invoice_template_html";

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

    public boolean hasCustomInvoiceHtmlTemplate() {
        return repo.getSetting(SETTING_INVOICE_HTML)
                .filter(s -> s != null && !s.isBlank() && s.toLowerCase().contains("<html"))
                .isPresent();
    }

    /** Active HTML template: custom upload if present, otherwise built-in resource. */
    public String getInvoiceHtmlTemplate() {
        return repo.getSetting(SETTING_INVOICE_HTML)
                .filter(s -> s != null && !s.isBlank() && s.toLowerCase().contains("<html"))
                .orElseGet(CompanyService::loadBuiltinInvoiceHtmlTemplate);
    }

    public void saveInvoiceHtmlTemplate(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("Template content is empty.");
        }
        repo.putSetting(SETTING_INVOICE_HTML, html);
    }

    public void clearInvoiceHtmlTemplate() {
        repo.putSetting(SETTING_INVOICE_HTML, "");
    }

    public static String loadBuiltinInvoiceHtmlTemplate() {
        try (InputStream is = CompanyService.class.getResourceAsStream("/templates/invoice-template.html")) {
            if (is == null) {
                throw new IllegalStateException("Built-in invoice-template.html not found on classpath.");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Could not load built-in invoice template: " + e.getMessage(), e);
        }
    }
}
