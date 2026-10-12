package com.infosys.subsidy;

import com.infosys.subsidy.service.BrevoEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BrevoEmailServiceTest {

    private RestTemplate restTemplate;
    private BrevoEmailService emailService;

    private static final String API_KEY = "xkeysib-test-dummy-api-key";
    private static final String SENDER_EMAIL = "noreply@digital-subsidy.gov.in";
    private static final String SENDER_NAME = "Digital Subsidy Platform";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        emailService = new BrevoEmailService(API_KEY, SENDER_EMAIL, SENDER_NAME, restTemplate);
    }

    @Test
    void testSendOtpEmail_Success() {
        ResponseEntity<String> response = new ResponseEntity<>("{\"messageId\":\"<123@brevo>\"}", HttpStatus.CREATED);
        when(restTemplate.postForEntity(eq("https://api.brevo.com/v3/smtp/email"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> emailService.sendOtpEmail("beneficiary@example.com", "849201"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).postForEntity(eq("https://api.brevo.com/v3/smtp/email"), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> captured = captor.getValue();
        HttpHeaders headers = captured.getHeaders();
        assertEquals(API_KEY, headers.getFirst("api-key"));
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertTrue(headers.getAccept().contains(MediaType.APPLICATION_JSON));

        Map<String, Object> body = captured.getBody();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        Map<String, String> sender = (Map<String, String>) body.get("sender");
        assertEquals(SENDER_EMAIL, sender.get("email"));
        assertEquals(SENDER_NAME, sender.get("name"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> toList = (List<Map<String, String>>) body.get("to");
        assertEquals(1, toList.size());
        assertEquals("beneficiary@example.com", toList.get(0).get("email"));

        assertEquals("Your Digital Subsidy Platform Login OTP", body.get("subject"));
        assertTrue(((String) body.get("htmlContent")).contains("849201"));
        assertTrue(((String) body.get("textContent")).contains("849201"));
    }

    @Test
    void testSendEmail_Api401Unauthorized_ThrowsException() {
        HttpClientErrorException unauthorized = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                "{\"message\":\"Key not found\"}".getBytes(),
                null
        );

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(unauthorized);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("Brevo API rejection"));
        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    void testSendEmail_Api500InternalError_ThrowsException() {
        HttpServerErrorException serverError = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                "{\"message\":\"Brevo service outage\"}".getBytes(),
                null
        );

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(serverError);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("Brevo API rejection"));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testSendEmail_NetworkTimeout_ThrowsException() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connect timed out to api.brevo.com"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("timeout or network failure"));
    }

    @Test
    void testSendEmail_MissingApiKey_ThrowsIllegalStateException() {
        BrevoEmailService serviceWithoutKey = new BrevoEmailService(null, SENDER_EMAIL, SENDER_NAME, restTemplate);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> serviceWithoutKey.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("Brevo API key is not configured"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSendEmail_MissingSenderEmail_ThrowsIllegalStateException() {
        BrevoEmailService serviceWithoutSender = new BrevoEmailService(API_KEY, "   ", SENDER_NAME, restTemplate);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> serviceWithoutSender.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("Brevo sender email is not configured"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSendEmail_BlankRecipient_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailService.sendOtpEmail("   ", "123456"));

        assertTrue(ex.getMessage().contains("Recipient email must not be empty"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSendEmail_NullTextOrHtml_DoesNotSendNullField() {
        ResponseEntity<String> response = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Sending with null textContent
        emailService.sendEmail("user@example.com", "Test Subject", "<b>Hello</b>", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

        Map<String, Object> body = captor.getValue().getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("textContent"));
        assertTrue(body.containsKey("htmlContent"));
    }

    @Test
    void testSendEmail_Non2xxStatus_ThrowsException() {
        ResponseEntity<String> response = new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendEmail("user@example.com", "Subject", "Body", "Text"));

        assertTrue(ex.getMessage().contains("Brevo API returned error status"));
    }

    @Test
    void testNoArgConstructorInstantiation() {
        assertDoesNotThrow(() -> {
            BrevoEmailService defaultService = new BrevoEmailService();
            assertNotNull(defaultService);
        });
    }

    @Test
    void testThreeArgConstructorInstantiation() {
        assertDoesNotThrow(() -> {
            BrevoEmailService threeArgService = new BrevoEmailService("dummy-key", "test@test.com", "Test");
            assertNotNull(threeArgService);
        });
    }

    @Test
    void testSendEmail_SmtpKey_ThrowsIllegalStateException() {
        BrevoEmailService serviceWithSmtpKey = new BrevoEmailService(
                "xsmtpsib-8c6daf2f848b971d4100e77f0d73f90928cae804fcd5a11d7640aaab72ca2144-dummy",
                SENDER_EMAIL,
                SENDER_NAME,
                restTemplate
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> serviceWithSmtpKey.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().contains("SMTP key"));
        assertTrue(ex.getMessage().contains("xsmtpsib-"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSendEmail_QuotedApiKey_CleansQuotesAndSends() {
        BrevoEmailService serviceWithQuotedKey = new BrevoEmailService(
                "\"xkeysib-cleaned-key\"",
                SENDER_EMAIL,
                SENDER_NAME,
                restTemplate
        );

        ResponseEntity<String> response = new ResponseEntity<>("{\"messageId\":\"<123@brevo>\"}", HttpStatus.CREATED);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> serviceWithQuotedKey.sendOtpEmail("user@example.com", "123456"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

        assertEquals("xkeysib-cleaned-key", captor.getValue().getHeaders().getFirst("api-key"));
    }
}
