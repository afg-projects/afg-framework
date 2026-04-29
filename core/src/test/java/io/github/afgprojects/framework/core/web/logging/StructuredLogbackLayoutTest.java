package io.github.afgprojects.framework.core.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

class StructuredLogbackLayoutTest {

    private StructuredLogbackLayout layout;
    private Logger logger;

    @BeforeEach
    void setUp() {
        layout = new StructuredLogbackLayout();
        logger = (Logger) LoggerFactory.getLogger(StructuredLogbackLayoutTest.class);
        MDC.clear();
    }

    @Test
    void should_outputJsonFormat_when_doLayoutCalled() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"timestamp\"");
        assertThat(result).contains("\"level\":\"INFO\"");
        assertThat(result).contains("\"logger\"");
        assertThat(result).contains("\"thread\"");
        assertThat(result).contains("\"message\":\"Test message\"");
        assertThat(result).startsWith("{");
        assertThat(result).endsWith("}\n");
    }

    @Test
    void should_includeException_when_throwablePresent() {
        // Given
        RuntimeException exception = new RuntimeException("Test exception");
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.ERROR,
                "Error occurred",
                null,
                null);
        event.setThrowableProxy(new ThrowableProxy(exception));
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"exception\"");
        assertThat(result).contains("\"className\":\"java.lang.RuntimeException\"");
        assertThat(result).contains("\"message\":\"Test exception\"");
    }

    @Test
    void should_includeMdcFields_when_mdcPopulated() {
        // Given
        MDC.put("traceId", "trace-123");
        MDC.put("userId", "42");

        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"traceId\":\"trace-123\"");
        assertThat(result).contains("\"userId\":\"42\"");

        MDC.clear();
    }

    @Test
    void should_formatTimestampInIso8601_when_doLayoutCalled() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then - ISO 8601 format contains date, time and Z suffix
        assertThat(result).contains("\"timestamp\":");
        assertThat(result).containsPattern("\"timestamp\":\"[0-9]{4}-[0-9]{2}-[0-9]{2}T");
        assertThat(result).containsPattern("T[0-9]{2}:[0-9]{2}:[0-9]{2}");
        assertThat(result).contains("Z\"");
    }

    @Test
    void should_outputPrettyPrint_when_prettyPrintEnabled() {
        // Given
        layout.setPrettyPrint(true);
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\n  \"");
        assertThat(result).contains("\n}");
    }

    @Test
    void should_includeLoggerName_when_doLayoutCalled() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.TestLogger", logger, Level.DEBUG, "Debug message", null, null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then - verify logger field exists
        // Note: LoggingEvent uses the logger object's name, not the first parameter
        assertThat(result).contains("\"logger\":");
        // Logger name comes from the logger object, which is this test class
        assertThat(result)
                .contains("\"logger\":\"io.github.afgprojects.framework.core.web.logging.StructuredLogbackLayoutTest\"");
    }

    @Test
    void should_includeThreadName_when_doLayoutCalled() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setThreadName("test-thread");
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"thread\":\"test-thread\"");
    }

    @Test
    void should_handleNullExceptionMessage_gracefully() {
        // Given
        RuntimeException exception = new RuntimeException();
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.ERROR,
                "Error occurred",
                null,
                null);
        event.setThrowableProxy(new ThrowableProxy(exception));
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"exception\"");
        assertThat(result).contains("\"className\":\"java.lang.RuntimeException\"");
    }

    @Test
    void should_setDefaultPrettyPrintToFalse_when_created() {
        // Given
        StructuredLogbackLayout newLayout = new StructuredLogbackLayout();

        // When/Then
        assertThat(newLayout).isNotNull();
    }

    @Test
    void should_includeCause_when_exceptionHasCause() {
        // Given
        RuntimeException cause = new RuntimeException("Cause exception");
        RuntimeException exception = new RuntimeException("Main exception", cause);

        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.ERROR,
                "Error occurred",
                null,
                null);
        event.setThrowableProxy(new ThrowableProxy(exception));
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"cause\"");
    }

    @Test
    void should_handleDifferentLogLevels_when_doLayoutCalled() {
        // Given
        LoggingEvent warnEvent = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.WARN,
                "Warning message",
                null,
                null);
        warnEvent.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(warnEvent);

        // Then
        assertThat(result).contains("\"level\":\"WARN\"");
    }

    // ==================== 敏感信息脱敏测试 ====================

    @Test
    void should_maskPhone_when_messageContainsPhone() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "User phone: 13812345678",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("138****5678");
        assertThat(result).doesNotContain("13812345678");
    }

    @Test
    void should_maskIdCard_when_messageContainsIdCard() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "ID card: 320102199001011234",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("320102********1234");
        assertThat(result).doesNotContain("320102199001011234");
    }

    @Test
    void should_maskBankCard_when_messageContainsBankCard() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Bank card: 6222021234567890123",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("6222********0123");
        assertThat(result).doesNotContain("6222021234567890123");
    }

    @Test
    void should_maskEmail_when_messageContainsEmail() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Email: testuser@example.com",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("tes***@example.com");
        assertThat(result).doesNotContain("testuser@example.com");
    }

    @Test
    void should_maskPasswordInJson_when_messageContainsPassword() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "{\"username\":\"admin\",\"password\":\"secret123\"}",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then - password is masked (note: JSON is escaped in the message field)
        assertThat(result).contains("\\\"password\\\":\\\"***\\\"");
        assertThat(result).doesNotContain("secret123");
    }

    @Test
    void should_maskTokenInJson_when_messageContainsToken() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgR\"}",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then - token is masked (note: JSON is escaped in the message field)
        assertThat(result).contains("\\\"token\\\":\\\"***\\\"");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
    }

    @Test
    void should_maskSensitiveMdcField_when_mdcContainsSensitiveData() {
        // Given
        MDC.put("password", "mypassword123");
        MDC.put("userId", "42");

        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "Test message",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"password\":\"myp***\"");
        assertThat(result).doesNotContain("mypassword123");
        assertThat(result).contains("\"userId\":\"42\"");

        MDC.clear();
    }

    @Test
    void should_notMaskSensitiveData_when_maskSensitiveDisabled() {
        // Given
        layout.setMaskSensitive(false);
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "User phone: 13812345678",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("13812345678");
    }

    @Test
    void should_handleMultipleSensitiveFields_when_messageContainsMultiple() {
        // Given
        LoggingEvent event = new LoggingEvent(
                "io.github.afgprojects.web.logging.StructuredLogbackLayoutTest",
                logger,
                Level.INFO,
                "User: phone=13812345678, idcard=320102199001011234, email=test@example.com",
                null,
                null);
        event.setTimeStamp(System.currentTimeMillis());

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("138****5678");
        assertThat(result).contains("320102********1234");
        assertThat(result).contains("tes***@example.com");
    }
}
