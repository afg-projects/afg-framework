package io.github.afgprojects.framework.ai.security;

import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultPiiDetector 单元测试
 */
class DefaultPiiDetectorTest {

    private DefaultPiiDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DefaultPiiDetector();
    }

    @Test
    @DisplayName("检测电子邮件")
    void detect_email() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiDetectionResult result = detector.detect("Contact me at test@example.com", context);

        assertThat(result.hasPii()).isTrue();
        assertThat(result.getPiiTypes()).contains(PiiDetector.PiiType.EMAIL);
        assertThat(result.getPiiMatches()).hasSize(1);

        PiiDetector.PiiMatch match = result.getPiiMatches().get(0);
        assertThat(match.getText()).isEqualTo("test@example.com");
        assertThat(match.getType()).isEqualTo(PiiDetector.PiiType.EMAIL);
    }

    @Test
    @DisplayName("检测手机号")
    void detect_phone() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiDetectionResult result = detector.detect("My phone is 13812345678", context);

        assertThat(result.hasPii()).isTrue();
        assertThat(result.getPiiTypes()).contains(PiiDetector.PiiType.PHONE);
    }

    @Test
    @DisplayName("检测身份证号")
    void detect_idNumber() {
        PiiDetector.PiiContext context = createContext();
        // 使用一个有效的身份证号格式（校验码正确）
        String validId = "11010519491231002X";

        PiiDetector.PiiDetectionResult result = detector.detect("ID: " + validId, context);

        assertThat(result.hasPii()).isTrue();
        assertThat(result.getPiiTypes()).contains(PiiDetector.PiiType.ID_NUMBER);
    }

    @Test
    @DisplayName("检测 IP 地址")
    void detect_ipAddress() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiDetectionResult result = detector.detect("Server IP: 192.168.1.100", context);

        assertThat(result.hasPii()).isTrue();
        assertThat(result.getPiiTypes()).contains(PiiDetector.PiiType.IP_ADDRESS);
    }

    @Test
    @DisplayName("检测多个 PII")
    void detect_multiplePii() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiDetectionResult result = detector.detect(
                "Email: test@example.com, Phone: 13812345678, IP: 192.168.1.1",
                context
        );

        assertThat(result.hasPii()).isTrue();
        assertThat(result.getPiiMatches()).hasSize(3);
        assertThat(result.getRiskLevel()).isNotEqualTo(ContentSafetyChecker.RiskLevel.NONE);
    }

    @Test
    @DisplayName("脱敏文本")
    void mask() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiMaskingResult result = detector.mask(
                "Contact me at test@example.com",
                context
        );

        assertThat(result.getMaskedCount()).isEqualTo(1);
        assertThat(result.getMaskedText()).doesNotContain("test@example.com");
        assertThat(result.getMaskingToken().getMappings()).containsKey("test@example.com");
    }

    @Test
    @DisplayName("部分脱敏")
    void mask_partialMask() {
        PiiDetector.PiiContext context = new PiiDetector.PiiContext() {
            @Override
            public String getUserId() { return "user-001"; }
            @Override
            public String getTenantId() { return "tenant-001"; }
            @Override
            public List<PiiDetector.PiiType> getDetectTypes() { return List.of(); }
            @Override
            public PiiDetector.MaskingStrategy getMaskingStrategy() { return PiiDetector.MaskingStrategy.PARTIAL_MASK; }
            @Override
            public double getMinConfidence() { return 0.5; }
        };

        PiiDetector.PiiMaskingResult result = detector.mask(
                "Phone: 13812345678",
                context
        );

        // 部分脱敏应该隐藏中间部分
        assertThat(result.getMaskedText()).doesNotContain("13812345678");
        assertThat(result.getMaskedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("还原脱敏内容")
    void unmask() {
        PiiDetector.PiiContext context = createContext();

        String original = "Email: test@example.com";
        PiiDetector.PiiMaskingResult maskedResult = detector.mask(original, context);

        String restored = detector.unmask(maskedResult.getMaskedText(), maskedResult.getMaskingToken());

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("指定类型脱敏")
    void mask_specificTypes() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiMaskingResult result = detector.mask(
                "Email: test@example.com, IP: 192.168.1.1",
                List.of(PiiDetector.PiiType.EMAIL),
                context
        );

        assertThat(result.getMaskedCount()).isEqualTo(1);
        assertThat(result.getMaskedTypes()).containsExactly(PiiDetector.PiiType.EMAIL);
        assertThat(result.getMaskedText()).contains("192.168.1.1");
    }

    @Test
    @DisplayName("无 PII 内容")
    void detect_noPii() {
        PiiDetector.PiiContext context = createContext();

        PiiDetector.PiiDetectionResult result = detector.detect("Hello, this is a normal message.", context);

        assertThat(result.hasPii()).isFalse();
        assertThat(result.getPiiMatches()).isEmpty();
        assertThat(result.getRiskLevel()).isEqualTo(ContentSafetyChecker.RiskLevel.NONE);
    }

    private PiiDetector.PiiContext createContext() {
        return new PiiDetector.PiiContext() {
            @Override
            public String getUserId() { return "user-001"; }
            @Override
            public String getTenantId() { return "tenant-001"; }
            @Override
            public List<PiiDetector.PiiType> getDetectTypes() { return List.of(); }
            @Override
            public PiiDetector.MaskingStrategy getMaskingStrategy() { return PiiDetector.MaskingStrategy.TYPE_LABEL; }
            @Override
            public double getMinConfidence() { return 0.5; }
        };
    }
}