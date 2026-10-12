package com.infosys.subsidy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final RestTemplate restTemplate;

    public BrevoEmailService() {
        this(null, null, "Digital Subsidy & Grant Platform");
    }

    @Autowired
    public BrevoEmailService(
            @Value("${brevo.api.key:}") String apiKey,
            @Value("${brevo.sender.email:}") String senderEmail,
            @Value("${brevo.sender.name:Digital Subsidy & Grant Platform}") String senderName) {
        this(apiKey, senderEmail, senderName, createDefaultRestTemplate(5000, 10000));
    }

    public BrevoEmailService(
            String apiKey,
            String senderEmail,
            String senderName,
            RestTemplate restTemplate) {
        this.apiKey = apiKey != null ? apiKey.trim() : null;
        this.senderEmail = senderEmail != null ? senderEmail.trim() : null;
        this.senderName = (senderName != null && !senderName.trim().isEmpty())
                ? senderName.trim()
                : "Digital Subsidy & Grant Platform";
        this.restTemplate = restTemplate != null ? restTemplate : createDefaultRestTemplate(5000, 10000);
    }

    private static RestTemplate createDefaultRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }

    /**
     * Preserves the existing public signature for OTP delivery.
     */
    public void sendOtpEmail(String recipientEmail, String otp) {
        String subject = "Your Digital Subsidy Platform Login OTP";
        String htmlContent = buildOtpEmailHtml(otp);
        String textContent = buildOtpEmailText(otp);
        sendEmail(recipientEmail, subject, htmlContent, textContent);
    }

    /**
     * General transactional email sending method via Brevo HTTP API.
     */
    public void sendEmail(String recipientEmail, String subject, String htmlContent, String textContent) {
        validateConfiguration();

        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            logger.error("Recipient email address must not be blank.");
            throw new IllegalArgumentException("Recipient email must not be empty.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> payload = buildPayload(recipientEmail.trim(), subject, htmlContent, textContent);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully delivered email via Brevo HTTP API to recipient domain: {}", maskEmailForLog(recipientEmail));
            } else {
                logger.error("Brevo HTTP API returned non-success status code: {}", response.getStatusCode().value());
                throw new RuntimeException("Brevo API returned error status: " + response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            logger.error("Brevo API HTTP error: status={}, body={}",
                    e.getStatusCode().value(),
                    sanitizeErrorMessage(e.getResponseBodyAsString()));
            throw new RuntimeException("Could not deliver email due to Brevo API rejection. Status: " + e.getStatusCode().value());

        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error while contacting Brevo API: {}",
                    sanitizeErrorMessage(e.getMessage()));
            throw new RuntimeException("Could not deliver email due to connection timeout or network failure.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;

        } catch (RuntimeException e) {
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during Brevo email delivery: {}",
                    sanitizeErrorMessage(e.getMessage()));
            throw new RuntimeException("Could not deliver OTP email due to external service failure.");
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Brevo API key is not configured.");
            throw new IllegalStateException("Brevo API key is not configured.");
        }
        if (senderEmail == null || senderEmail.isEmpty()) {
            logger.error("Brevo sender email is not configured.");
            throw new IllegalStateException("Brevo sender email is not configured.");
        }
    }

    private Map<String, Object> buildPayload(String recipientEmail, String subject, String htmlContent, String textContent) {
        Map<String, Object> payload = new LinkedHashMap<>();

        Map<String, String> sender = new LinkedHashMap<>();
        sender.put("email", senderEmail);
        if (senderName != null && !senderName.isEmpty()) {
            sender.put("name", senderName);
        }
        payload.put("sender", sender);

        Map<String, String> recipient = new LinkedHashMap<>();
        recipient.put("email", recipientEmail);
        payload.put("to", List.of(recipient));

        if (subject != null && !subject.trim().isEmpty()) {
            payload.put("subject", subject);
        }

        if (htmlContent != null && !htmlContent.trim().isEmpty()) {
            payload.put("htmlContent", htmlContent);
        }

        if (textContent != null && !textContent.trim().isEmpty()) {
            payload.put("textContent", textContent);
        }

        return payload;
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

    private String buildOtpEmailText(String otp) {
        return "Digital Subsidy & Grant Administration Platform\n\n"
             + "Hello,\n\n"
             + "Your one-time password for secure login is: " + otp + "\n\n"
             + "This OTP is valid for 5 minutes.\n"
             + "Do not share this OTP with anyone.\n"
             + "If you did not request this login, please ignore this email.\n\n"
             + "Regards,\nDigital Subsidy & Grant Administration Platform";
    }

    private String maskEmailForLog(String email) {
        if (email == null) {
            return "unknown";
        }
        int atIndex = email.indexOf('@');
        if (atIndex > 1) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return "***";
    }

    private String sanitizeErrorMessage(String raw) {
        if (raw == null) {
            return "No detail provided";
        }
        // Redact any patterns resembling API keys, auth tokens or secrets
        String sanitized = raw.replaceAll("(?i)(api[_-]?key|password|secret|bearer)\\s*[:=]\\s*['\"]?[^'\"\\s]+", "$1=[REDACTED]");
        if (apiKey != null && !apiKey.isEmpty()) {
            sanitized = sanitized.replace(apiKey, "[REDACTED]");
        }
        return sanitized;
    }
}
