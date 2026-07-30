package com.sevatyres.service;

import com.sevatyres.model.Customer;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
import com.sevatyres.model.Transaction;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Email reminder service for Seva Tyres.
 * Sends credit balance reminders to customers every 5 days.
 *
 * SETUP REQUIRED in application.properties:
 *   email.enabled=true
 *   email.smtp.host=smtp.gmail.com
 *   email.smtp.port=587
 *   email.smtp.username=youremail@gmail.com
 *   email.smtp.password=your_app_password
 *   email.from.name=Seva Tyres
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private ScheduledExecutorService scheduler;

    // Email configuration (loaded from application.properties)
    private boolean enabled = false;
    private String smtpHost  = "smtp.gmail.com";
    private int    smtpPort  = 587;
    private String username  = "";
    private String password  = "";
    private String fromName  = "Seva Tyres";

    public EmailService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            if (is == null) return;
            Properties props = new Properties();
            props.load(is);
            // Prefer mail.* keys (application.properties); fall back to legacy email.* keys
            enabled  = Boolean.parseBoolean(first(props, "mail.enabled", "email.enabled", "false"));
            smtpHost = first(props, "mail.host", "email.smtp.host", "smtp.gmail.com");
            smtpPort = Integer.parseInt(first(props, "mail.port", "email.smtp.port", "587"));
            username = first(props, "mail.username", "email.smtp.username", "");
            password = first(props, "mail.password", "email.smtp.password", "");
            fromName = first(props, "mail.from", "email.from.name", "Seva Tyres");
        } catch (Exception e) {
            LOG.warning("[EmailService] Could not load email config: " + e.getMessage());
        }
    }

    private static String first(Properties p, String k1, String k2, String def) {
        String v = p.getProperty(k1);
        if (v != null && !v.isBlank()) return v.trim();
        v = p.getProperty(k2);
        if (v != null && !v.isBlank()) return v.trim();
        return def;
    }

    /**
     * Start a scheduler that sends credit reminders every 5 days.
     */
    public void startReminderScheduler(CustomerService customerService,
                                       TransactionService txnService) {
        if (!enabled) {
            LOG.info("[EmailService] Email reminders disabled. Set email.enabled=true in application.properties.");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "email-reminder");
            t.setDaemon(true);
            return t;
        });
        // Run immediately on startup, then every 5 days
        scheduler.scheduleAtFixedRate(
                () -> sendCreditReminders(customerService, txnService),
                0, 5, TimeUnit.DAYS
        );
        LOG.info("[EmailService] Credit reminder scheduler started (every 5 days).");
    }

    /**
     * Sends reminders for credits that are still open AND at least 15 days old.
     * Runs every 5 days via the scheduler (see startReminderScheduler).
     */
    private void sendCreditReminders(CustomerService customerService,
                                     TransactionService txnService) {
        try {
            java.time.LocalDate cutoff = java.time.LocalDate.now().minusDays(15);
            List<Customer> customers = customerService.getAll();
            for (Customer customer : customers) {
                if (customer.getEmail() == null || customer.getEmail().isBlank()) continue;

                List<Transaction> credits = txnService.getCreditsByCustomer(customer.getId());
                double outstanding = credits.stream()
                        .filter(Transaction::isOngoing)
                        .filter(t -> t.getDate() != null && !t.getDate().isAfter(cutoff))
                        .mapToDouble(Transaction::getBalance)
                        .sum();

                if (outstanding > 0) {
                    sendCreditReminderEmail(customer, outstanding);
                }
            }
        } catch (Exception e) {
            LOG.warning("[EmailService] Error sending credit reminders: " + e.getMessage());
        }
    }

    /**
     * Send a professional credit reminder email to a customer.
     */
    public void sendCreditReminderEmail(Customer customer, double outstandingAmount) {
        if (!enabled || username.isBlank()) {
            LOG.info("[EmailService] Would send reminder to: " + customer.getEmail()
                    + " for amount: ₹" + String.format("%.2f", outstandingAmount));
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, fromName));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(customer.getEmail()));
            message.setSubject("Payment Reminder — Seva Tyres");
            message.setText(buildReminderMessage(customer, outstandingAmount));

            Transport.send(message);
            LOG.info("[EmailService] Reminder sent to " + customer.getEmail());

        } catch (Exception e) {
            LOG.warning("[EmailService] Failed to send email to " + customer.getEmail()
                    + ": " + e.getMessage());
        }
    }

    private String buildReminderMessage(Customer customer, double amount) {
        return "Dear " + customer.getName() + ",\n\n"
                + "This is a friendly reminder from Seva Tyres regarding your outstanding balance.\n\n"
                + "Outstanding Amount: ₹" + String.format("%,.2f", amount) + "\n\n"
                + "We kindly request you to clear the above amount at your earliest convenience.\n\n"
                + "For any queries or to arrange a payment, please contact us:\n"
                + "  Seva Tyres\n"
                + "  Phone: [Your Phone Number]\n"
                + "  Email: " + username + "\n\n"
                + "We appreciate your continued trust in our services.\n\n"
                + "Thank you,\n"
                + "Seva Tyres Team\n\n"
                + "---\n"
                + "This is an automated reminder. Please do not reply to this email.";
    }

    /**
     * Immediate credit-summary email after a sale creates outstanding credit.
     * Uses the customer's delivery email and the Seva Tyres credit template.
     */
    public void sendCreditSaleSummary(Customer customer, SaleTransaction sale,
                                      List<SaleTransactionItem> items) {
        if (customer == null || sale == null) return;
        String toEmail = customer.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            LOG.info("[EmailService] No email for customer " + customer.getName()
                    + " — skipping credit summary.");
            return;
        }
        if (!enabled || username.isBlank()) {
            LOG.info("[EmailService] Would send credit summary to: " + toEmail
                    + " for bill " + sale.getBillNo()
                    + " remaining ₹" + String.format("%,.2f", sale.getCreditAmount()));
            return;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Transaction summary & credit balance — Seva Tyres");
            message.setContent(buildCreditSaleHtml(customer, sale, items), "text/html; charset=UTF-8");
            Transport.send(message);
            LOG.info("[EmailService] Credit summary sent to " + toEmail);
        } catch (Exception e) {
            LOG.warning("[EmailService] Failed credit summary to " + toEmail + ": " + e.getMessage());
        }
    }

    private String buildCreditSaleHtml(Customer customer, SaleTransaction sale,
                                       List<SaleTransactionItem> items) {
        int year = (sale.getSaleDate() != null ? sale.getSaleDate() : LocalDate.now()).getYear();
        String invoiceNum = String.format("INV-%d-%05d", year, sale.getId());
        String custId = customer.getId() > 0
                ? String.format("CUST-%05d", customer.getId()) : "-";
        String dateStr = sale.getSaleDate() != null
                ? sale.getSaleDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
        double paid = Math.max(0, sale.getTotal() - sale.getCreditAmount());
        String contact = customer.getPhone() != null ? customer.getPhone() : "";

        StringBuilder rows = new StringBuilder();
        if (items != null && !items.isEmpty()) {
            for (SaleTransactionItem it : items) {
                rows.append("<tr>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;'>")
                    .append(esc(it.getItemName())).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:center;'>")
                    .append(it.getQuantity()).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;'>")
                    .append(emMoney(it.getUnitPrice())).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;'>")
                    .append(emMoney(it.getLineTotal())).append("</td>")
                    .append("</tr>");
            }
        } else {
            rows.append("<tr>")
                .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;'>")
                .append(esc(sale.getParticulars() != null ? sale.getParticulars() : "Sale"))
                .append("</td>")
                .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:center;'>")
                .append(Math.max(1, sale.getQuantity())).append("</td>")
                .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;'>")
                .append(emMoney(sale.getUnitPrice() > 0 ? sale.getUnitPrice() : sale.getTotal()))
                .append("</td>")
                .append("<td style='padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;'>")
                .append(emMoney(sale.getTotal())).append("</td>")
                .append("</tr>");
        }

        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;color:#111827;line-height:1.5;'>"
                + "<p>Dear</p>"
                + "<p style='font-size:18px;font-weight:700;margin:0 0 16px;'>" + esc(customer.getName()) + "</p>"
                + "<p>Thank you for your recent visit. This is a summary of your transaction and the outstanding "
                + "credit balance on your account. Please review the details below.</p>"
                + "<h3 style='margin:24px 0 8px;'>Transaction details</h3>"
                + "<table style='width:100%;max-width:560px;border-collapse:collapse;'>"
                + row("Invoice number", invoiceNum)
                + row("Date", dateStr)
                + row("Customer ID", custId)
                + row("Contact", contact)
                + "</table>"
                + "<h3 style='margin:24px 0 8px;'>Items purchased</h3>"
                + "<table style='width:100%;max-width:560px;border-collapse:collapse;'>"
                + "<thead><tr style='background:#f3f4f6;'>"
                + "<th style='padding:8px;text-align:left;'>Item</th>"
                + "<th style='padding:8px;text-align:center;'>Qty</th>"
                + "<th style='padding:8px;text-align:right;'>Unit price</th>"
                + "<th style='padding:8px;text-align:right;'>Total</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + "<table style='width:100%;max-width:560px;border-collapse:collapse;margin-top:12px;'>"
                + row("Subtotal", emMoney(sale.getSubtotal() > 0 ? sale.getSubtotal() : sale.getTotal() - sale.getTaxAmount()))
                + row("Tax" + (sale.getTaxLabel() != null ? " (" + sale.getTaxLabel() + ")" : ""), emMoney(sale.getTaxAmount()))
                + row("Total amount", emMoney(sale.getTotal()))
                + row("Paid amount", emMoney(paid))
                + row("Remaining credit", emMoney(sale.getCreditAmount()))
                + "</table>"
                + "<h3 style='margin:24px 0 8px;'>Payment reminder</h3>"
                + "<p>Kindly clear the remaining balance of <strong>" + emMoney(sale.getCreditAmount())
                + "</strong> at your earliest convenience. If you've already made the payment, please ignore this message.</p>"
                + "<p>For any queries regarding this transaction, feel free to contact us. We appreciate your continued business.</p>"
                + "<p>Warm regards,<br><strong>The Seva Tyres Team</strong></p>"
                + "<p style='color:#6b7280;font-size:13px;'>support@Seva Tyres.com · +91 00000 00000</p>"
                + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:24px 0;'>"
                + "<p style='color:#9ca3af;font-size:12px;'>This is an automated message from Seva Tyres. Please do not reply directly to this email.<br>"
                + "© " + year + " Seva Tyres. All rights reserved.</p>"
                + "</body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:6px 8px;color:#6b7280;width:40%;'>" + esc(label)
                + "</td><td style='padding:6px 8px;font-weight:600;'>" + esc(value) + "</td></tr>";
    }

    private static String emMoney(double v) {
        return "₹" + String.format("%,.2f", v);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
