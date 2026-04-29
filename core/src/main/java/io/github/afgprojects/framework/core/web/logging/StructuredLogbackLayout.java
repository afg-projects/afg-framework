package io.github.afgprojects.framework.core.web.logging;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import lombok.Setter;

import io.github.afgprojects.framework.core.web.security.util.SensitiveDataMasker;

/**
 * 结构化日志编码器
 * <p>
 * 输出 JSON 格式日志，支持：
 * <ul>
 *   <li>ISO 8601 时间戳</li>
 *   <li>MDC 上下文自动注入</li>
 *   <li>敏感信息脱敏</li>
 *   <li>异常堆栈格式化</li>
 * </ul>
 */
public class StructuredLogbackLayout extends EncoderBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * 手机号正则：匹配中国大陆11位手机号（独立数字，避免误匹配）
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<![\\d])(1[3-9]\\d)\\d{4}(\\d{4})(?![\\d])");

    /**
     * 身份证号正则：匹配18位身份证号（独立数字，避免误匹配）
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![\\d])(\\d{6})\\d{8}(\\d{4})(?![\\d])");

    /**
     * 银行卡号正则：匹配16-19位银行卡号（独立数字，避免误匹配）
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(?<![\\d])(\\d{4})\\d{8,11}(\\d{4})(?![\\d])");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1,3})\\w+(@\\w+\\.\\w+)");

    /**
     * 密码/Token 字段正则
     */
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(password|pwd|token|secret|apikey|credential|accesstoken|refreshtoken)\"\\s*:\\s*\"[^\"]+\"",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    @Setter
    private boolean prettyPrint;

    @Setter
    private boolean maskSensitive = true;

    @Setter
    private Charset charset = StandardCharsets.UTF_8;

    public StructuredLogbackLayout() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        return doLayout(event).getBytes(charset);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    /**
     * 将日志事件转换为 JSON 字符串
     *
     * @param event 日志事件
     * @return JSON 格式的日志字符串
     */
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> logMap = new HashMap<>();

        // 时间戳（ISO 8601 格式）
        logMap.put("timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));

        // 日志级别
        logMap.put("level", event.getLevel().toString());

        // 日志记录器名称
        logMap.put("logger", event.getLoggerName());

        // 线程名称
        logMap.put("thread", event.getThreadName());

        // 日志消息（应用敏感信息脱敏）
        String message = event.getFormattedMessage();
        if (maskSensitive) {
            message = maskMessage(message);
        }
        logMap.put("message", message);

        // 异常信息
        if (event.getThrowableProxy() != null) {
            logMap.put("exception", formatThrowable(event));
        }

        // MDC 中的所有字段（应用敏感信息脱敏）
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (maskSensitive && SensitiveDataMasker.isSensitive(key)) {
                    value = SensitiveDataMasker.mask(key, value);
                }
                logMap.put(key, value);
            }
        }

        try {
            if (prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logMap) + '\n';
            }
            return objectMapper.writeValueAsString(logMap) + '\n';
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize log: " + e.getMessage() + "\"}\n";
        }
    }

    /**
     * 对日志消息中的敏感信息进行脱敏
     *
     * @param message 原始消息
     * @return 脱敏后的消息
     */
    private String maskMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = message;

        // 脱敏 JSON 中的敏感字段值
        masked = SENSITIVE_FIELD_PATTERN.matcher(masked).replaceAll("$1\\\":\\\"***\\\"");

        // 脱敏手机号：保留前3位和后4位
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");

        // 脱敏身份证号：保留前6位和后4位
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("$1********$2");

        // 脱敏银行卡号：保留前4位和后4位
        masked = BANK_CARD_PATTERN.matcher(masked).replaceAll("$1********$2");

        // 脱敏邮箱：保留前1-3位和域名
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("$1***$2");

        return masked;
    }

    private Map<String, Object> formatThrowable(ILoggingEvent event) {
        Map<String, Object> exceptionMap = new HashMap<>();
        IThrowableProxy throwableProxy = event.getThrowableProxy();

        if (throwableProxy != null) {
            exceptionMap.put("className", throwableProxy.getClassName());
            exceptionMap.put("message", throwableProxy.getMessage());

            StackTraceElementProxy[] stackTraceElementProxyArray = throwableProxy.getStackTraceElementProxyArray();
            if (stackTraceElementProxyArray != null && stackTraceElementProxyArray.length > 0) {
                StringBuilder stackTrace = new StringBuilder();
                int maxFrames = Math.min(stackTraceElementProxyArray.length, 20);
                for (int i = 0; i < maxFrames; i++) {
                    stackTrace.append(stackTraceElementProxyArray[i].toString()).append('\n');
                }
                if (stackTraceElementProxyArray.length > maxFrames) {
                    stackTrace
                            .append("... ")
                            .append(stackTraceElementProxyArray.length - maxFrames)
                            .append(" more");
                }
                exceptionMap.put("stackTrace", stackTrace.toString());
            }

            IThrowableProxy cause = throwableProxy.getCause();
            if (cause != null) {
                Map<String, Object> causeMap = new HashMap<>();
                causeMap.put("className", cause.getClassName());
                causeMap.put("message", cause.getMessage());
                exceptionMap.put("cause", causeMap);
            }
        }

        return exceptionMap;
    }
}
