package com.infosys.subsidy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class BrevoEmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService(
            JavaMailSender mailSender,
            @Value("${brevo.sender.email}") String senderEmail,
            @Value("${brevo.sender.name}") String senderName) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public void sendOtpEmail(String recipientEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(recipientEmail);
            helper.setSubject("Your Digital Subsidy Platform Login OTP");
            helper.setText(buildOtpEmailHtml(otp), true);

            mailSender.send(message);
            logger.info("Successfully sent OTP email to user.");
        } catch (Exception e) {
            logger.error("Exception occurred while communicating with Brevo SMTP: {}", e.getMessage());
            throw new RuntimeException("Could not deliver OTP email due to external service failure.");
        }
    }

    private String buildOtpEmailHtml(String otp) {
        return "<html><body>"
             + "<h2>Digital Subsidy & Grant Administration Platform</h2>"
             + "<p>Hello,</p>"
             + "<p>Your one-time password for secure login is:</p>"
             + "<h3>" + otp + "</h3>"
             + "<p>This OTP is valid for 5 minutes.</p>"
             + "<p>Do not share this OTP with anyone.</p>"
             + "<p>If you did not request this login, please ignore this email.</p>"
             + "<br/>"
             + "<p>Regards,<br/>Digital Subsidy & Grant Administration Platform</p>"
             + "</body></html>";
    }
}
