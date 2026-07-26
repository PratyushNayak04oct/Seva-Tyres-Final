package com.sevatyres.service;

import com.sevatyres.model.Customer;
import com.sevatyres.model.Transaction;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.io.InputStream;
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
            enabled  = Boolean.parseBoolean(props.getProperty("email.enabled", "false"));
            smtpHost = props.getProperty("email.smtp.host", "smtp.gmail.com");
            smtpPort = Integer.parseInt(props.getProperty("email.smtp.port", "587"));
            username = props.getProperty("email.smtp.username", "");
            password = props.getProperty("email.smtp.password", "");
            fromName = props.getProperty("email.from.name", "Seva Tyres");
        } catch (Exception e) {
            LOG.warning("[EmailService] Could not load email config: " + e.getMessage());
        }
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

    private void sendCreditReminders(CustomerService customerService,
                                     TransactionService txnService) {
        try {
            List<Customer> customers = customerService.getAll();
            for (Customer customer : customers) {
                if (customer.getEmail() == null || customer.getEmail().isBlank()) continue;

                List<Transaction> credits = txnService.getCreditsByCustomer(customer.getId());
                double outstanding = credits.stream()
                        .filter(Transaction::isOngoing)
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

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
