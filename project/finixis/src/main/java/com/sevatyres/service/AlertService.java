package com.sevatyres.service;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.AlertConfig;
import com.sevatyres.model.Customer;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Manages alert campaigns and sends email/SMS notifications to customers.
 */
public class AlertService {

    private static final Logger LOG = Logger.getLogger(AlertService.class.getName());

    private boolean emailEnabled;
    private String smtpHost, smtpUser, smtpPass, fromName;
    private int smtpPort;

    public AlertService() {
        loadEmailConfig();
    }

    private void loadEmailConfig() {
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            if (is == null) return;
            Properties p = new Properties();
            p.load(is);
            emailEnabled = Boolean.parseBoolean(p.getProperty("mail.enabled", "false"));
            smtpHost     = p.getProperty("mail.host", "smtp.gmail.com");
            smtpPort     = Integer.parseInt(p.getProperty("mail.port", "587"));
            smtpUser     = p.getProperty("mail.username", "");
            smtpPass     = p.getProperty("mail.password", "");
            fromName     = p.getProperty("mail.from", "Seva Tyres");
        } catch (Exception e) {
            LOG.warning("Could not load email config: " + e.getMessage());
        }
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public List<AlertConfig> getAll() {
        List<AlertConfig> list = new ArrayList<>();
        String sql = "SELECT * FROM Alert_Config ORDER BY config_id";
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public AlertConfig save(AlertConfig cfg) {
        if (cfg.getId() == 0) return insert(cfg);
        update(cfg);
        return cfg;
    }

    private AlertConfig insert(AlertConfig cfg) {
        String sql = "INSERT INTO Alert_Config(name,message_template,channel,interval_days,duration_days,is_active) VALUES(?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cfg.getName());
            ps.setString(2, cfg.getMessageTemplate());
            ps.setString(3, cfg.getChannel().name());
            ps.setInt(4, cfg.getIntervalDays());
            ps.setInt(5, cfg.getDurationDays());
            ps.setBoolean(6, cfg.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) cfg.setId(k.getInt(1)); }
        } catch (Exception e) { throw new RuntimeException(e); }
        return cfg;
    }

    private void update(AlertConfig cfg) {
        String sql = "UPDATE Alert_Config SET name=?,message_template=?,channel=?,interval_days=?,duration_days=?,is_active=? WHERE config_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cfg.getName());
            ps.setString(2, cfg.getMessageTemplate());
            ps.setString(3, cfg.getChannel().name());
            ps.setInt(4, cfg.getIntervalDays());
            ps.setInt(5, cfg.getDurationDays());
            ps.setBoolean(6, cfg.isActive());
            ps.setInt(7, cfg.getId());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Alert_Config WHERE config_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Sends the alert to every customer whose email is set (for EMAIL channel).
     * Returns count of successfully sent messages.
     */
    public int sendNow(AlertConfig cfg, List<Customer> customers) {
        int sent = 0;
        for (Customer c : customers) {
            String msg = personalize(cfg.getMessageTemplate(), c);
            boolean ok = false;
            if (cfg.getChannel() == AlertConfig.Channel.EMAIL) {
                ok = sendEmail(c.getEmail(), c.getName(), "Message from Seva Tyres", msg);
            } else {
                // SMS via email-to-SMS gateway (e.g. number@airtelmail.com)
                ok = sendSmsViaEmail(c.getPhone(), c.getName(), msg);
            }
            if (ok) {
                logAlert(cfg.getId(), c.getId(), c.getName(), cfg.getChannel().name(), msg, "SENT");
                sent++;
            } else {
                logAlert(cfg.getId(), c.getId(), c.getName(), cfg.getChannel().name(), msg, "FAILED");
            }
        }
        // Update last_run
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Alert_Config SET last_run=? WHERE config_id=?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, cfg.getId());
            ps.executeUpdate();
        } catch (Exception ignored) {}
        return sent;
    }

    private String personalize(String template, Customer c) {
        return template
                .replace("{name}", c.getName() != null ? c.getName() : "Customer")
                .replace("{phone}", c.getPhone() != null ? c.getPhone() : "")
                .replace("{email}", c.getEmail() != null ? c.getEmail() : "");
    }

    private boolean sendEmail(String toEmail, String toName, String subject, String body) {
        if (!emailEnabled || smtpUser.isBlank()) {
            LOG.info("[Alert] Email disabled/unconfigured — would send to: " + toEmail);
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) return false;
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPass);
                }
            });
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(smtpUser, fromName));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail, toName));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
            return true;
        } catch (Exception e) {
            LOG.warning("[Alert] Email send failed to " + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    private boolean sendSmsViaEmail(String phone, String name, String body) {
        // Uses email-to-SMS gateway approach
        // Without Twilio credentials this is a stub — logs the attempt
        LOG.info("[Alert] SMS stub — would send to " + phone + ": " + body);
        return false;
    }

    private void logAlert(int configId, int customerId, String customerName,
                          String channel, String message, String status) {
        String sql = "INSERT INTO Alert_Log(config_id,customer_id,customer_name,channel,message,status) VALUES(?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, configId);
            ps.setInt(2, customerId);
            ps.setString(3, customerName);
            ps.setString(4, channel);
            ps.setString(5, message);
            ps.setString(6, status);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public List<AlertLogEntry> getLog(int limit) {
        List<AlertLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM Alert_Log ORDER BY sent_at DESC LIMIT " + limit;
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AlertLogEntry e = new AlertLogEntry();
                e.customerName = rs.getString("customer_name");
                e.channel      = rs.getString("channel");
                e.status       = rs.getString("status");
                Timestamp ts = rs.getTimestamp("sent_at");
                e.sentAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                e.message = rs.getString("message");
                list.add(e);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private AlertConfig map(ResultSet rs) throws SQLException {
        AlertConfig cfg = new AlertConfig();
        cfg.setId(rs.getInt("config_id"));
        cfg.setName(rs.getString("name"));
        cfg.setMessageTemplate(rs.getString("message_template"));
        try { cfg.setChannel(AlertConfig.Channel.valueOf(rs.getString("channel"))); }
        catch (Exception ignored) {}
        cfg.setIntervalDays(rs.getInt("interval_days"));
        try { cfg.setDurationDays(rs.getInt("duration_days")); } catch (Exception ignored) {}
        cfg.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("last_run");
        if (ts != null) cfg.setLastRun(ts.toLocalDateTime());
        return cfg;
    }

    public static class AlertLogEntry {
        public String customerName, channel, status, message;
        public LocalDateTime sentAt;
    }

    public boolean isEmailConfigured() { return emailEnabled && !smtpUser.isBlank(); }

    /**
     * Ensures a system "Credit Payment Reminder" campaign exists.
     * Sends every 5 days; campaign duration 365 days.
     * Actual start delay of 15 days after the transaction date is enforced
     * by EmailService.sendCreditReminders().
     */
    public void ensureCreditPaymentCampaign() {
        final String CAMPAIGN_NAME = "Credit Payment Reminder";
        boolean exists = getAll().stream()
                .anyMatch(c -> CAMPAIGN_NAME.equalsIgnoreCase(c.getName()));
        if (exists) return;

        AlertConfig cfg = new AlertConfig();
        cfg.setName(CAMPAIGN_NAME);
        cfg.setMessageTemplate(
                "Dear {name},\n\n"
                + "This is a friendly reminder from Seva Tyres regarding your outstanding credit balance.\n\n"
                + "Please clear your dues at your earliest convenience.\n\n"
                + "Thank you for choosing Seva Tyres.\n"
                + "Best regards,\nSeva Tyres");
        cfg.setChannel(AlertConfig.Channel.EMAIL);
        cfg.setIntervalDays(5);   // every 5 days
        cfg.setDurationDays(365); // keep running for a year
        cfg.setActive(true);
        save(cfg);
        LOG.info("[AlertService] Created default campaign: " + CAMPAIGN_NAME);
    }
}
