package io.github.afgprojects.framework.ai.core.security;

import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认 PII 检测器实现
 *
 * <p>基于正则表达式的 PII 检测器，适用于：
 * <ul>
 *   <li>常见 PII 类型检测</li>
 *   <li>开发测试环境</li>
 *   <li>轻量级场景</li>
 * </ul>
 *
 * <p>生产环境建议使用专业 PII 检测服务或机器学习模型。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultPiiDetector implements PiiDetector {

    private static final Logger log = LoggerFactory.getLogger(DefaultPiiDetector.class);

    private final Map<PiiType, Pattern> patterns = new EnumMap<>(PiiType.class);

    /**
     * 创建默认 PII 检测器
     */
    public DefaultPiiDetector() {
        initPatterns();
    }

    private void initPatterns() {
        // 电子邮件
        patterns.put(PiiType.EMAIL, Pattern.compile(
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                Pattern.CASE_INSENSITIVE
        ));

        // 中国手机号
        patterns.put(PiiType.PHONE, Pattern.compile(
                "1[3-9]\\d{9}"
        ));

        // 中国身份证号
        patterns.put(PiiType.ID_NUMBER, Pattern.compile(
                "\\d{17}[\\dXx]"
        ));

        // 信用卡号
        patterns.put(PiiType.CREDIT_CARD, Pattern.compile(
                "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b"
        ));

        // 银行账号（16-19位数字）
        patterns.put(PiiType.BANK_ACCOUNT, Pattern.compile(
                "\\b\\d{16,19}\\b"
        ));

        // IPv4 地址
        patterns.put(PiiType.IP_ADDRESS, Pattern.compile(
                "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
        ));

        // MAC 地址
        patterns.put(PiiType.MAC_ADDRESS, Pattern.compile(
                "\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b"
        ));

        // 护照号（中国）
        patterns.put(PiiType.PASSPORT_NUMBER, Pattern.compile(
                "\\b[EG]\\d{8}\\b"
        ));

        // 驾照号（中国）
        patterns.put(PiiType.DRIVER_LICENSE, Pattern.compile(
                "\\b\\d{12}\\b"
        ));
    }

    @Override
    @NonNull
    public PiiDetectionResult detect(@NonNull String text, @NonNull PiiContext context) {
        List<PiiMatch> matches = new ArrayList<>();
        List<PiiType> typesToDetect = context.getDetectTypes().isEmpty()
                ? Arrays.asList(PiiType.values())
                : context.getDetectTypes();

        for (PiiType type : typesToDetect) {
            Pattern pattern = patterns.get(type);
            if (pattern == null) continue;

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String matchedText = matcher.group();
                double confidence = calculateConfidence(type, matchedText);

                if (confidence >= context.getMinConfidence()) {
                    matches.add(new DefaultPiiMatch(
                            type,
                            matchedText,
                            matcher.start(),
                            matcher.end(),
                            confidence,
                            maskValue(matchedText, type, context.getMaskingStrategy())
                    ));
                }
            }
        }

        ContentSafetyChecker.RiskLevel riskLevel = calculateRiskLevel(matches);

        return new DefaultPiiDetectionResult(!matches.isEmpty(), matches, getPiiTypes(matches), riskLevel);
    }

    @Override
    @NonNull
    public PiiMaskingResult mask(@NonNull String text, @NonNull PiiContext context) {
        return mask(text, Arrays.asList(PiiType.values()), context);
    }

    @Override
    @NonNull
    public PiiMaskingResult mask(@NonNull String text, @NonNull List<PiiType> piiTypes, @NonNull PiiContext context) {
        PiiDetectionResult detectionResult = detect(text, context);

        if (!detectionResult.hasPii()) {
            return new DefaultPiiMaskingResult(text, new DefaultMaskingToken(), 0, Collections.emptyList());
        }

        Map<String, String> mappings = new HashMap<>();
        Map<String, String> reverseMappings = new HashMap<>();
        String maskedText = text;
        int maskedCount = 0;
        List<PiiType> maskedTypes = new ArrayList<>();

        // 按位置倒序处理，避免位置偏移
        List<PiiMatch> matches = new ArrayList<>(detectionResult.getPiiMatches());
        matches.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        for (PiiMatch match : matches) {
            if (!piiTypes.contains(match.getType())) continue;

            String original = match.getText();
            String masked = match.getMaskedValue();

            // 生成唯一占位符
            String placeholder = generatePlaceholder(match.getType(), maskedCount);

            mappings.put(original, placeholder);
            reverseMappings.put(placeholder, original);

            maskedText = maskedText.substring(0, match.getStart()) + placeholder + maskedText.substring(match.getEnd());
            maskedCount++;

            if (!maskedTypes.contains(match.getType())) {
                maskedTypes.add(match.getType());
            }
        }

        MaskingToken token = new DefaultMaskingToken(mappings, reverseMappings);

        return new DefaultPiiMaskingResult(maskedText, token, maskedCount, maskedTypes);
    }

    @Override
    @NonNull
    public String unmask(@NonNull String maskedText, @NonNull MaskingToken maskingToken) {
        String result = maskedText;

        for (Map.Entry<String, String> entry : maskingToken.getReverseMappings().entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private double calculateConfidence(PiiType type, String matchedText) {
        // 简单的置信度计算
        switch (type) {
            case EMAIL:
                return matchedText.contains("@") ? 0.95 : 0.7;
            case PHONE:
                return isValidPhoneNumber(matchedText) ? 0.9 : 0.6;
            case ID_NUMBER:
                return isValidIdNumber(matchedText) ? 0.95 : 0.5;
            case CREDIT_CARD:
                return isValidCreditCard(matchedText) ? 0.9 : 0.5;
            case IP_ADDRESS:
                return isValidIpAddress(matchedText) ? 0.95 : 0.6;
            default:
                return 0.8;
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        // 简单的手机号验证
        return phone.length() == 11 && phone.startsWith("1");
    }

    private boolean isValidIdNumber(String id) {
        // 简单的身份证号验证
        if (id.length() != 18) return false;

        // 校验码验证
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * weights[i];
        }

        char expectedCheckCode = checkCodes[sum % 11];
        char actualCheckCode = Character.toUpperCase(id.charAt(17));

        return expectedCheckCode == actualCheckCode;
    }

    private boolean isValidCreditCard(String card) {
        // Luhn 算法验证
        String digits = card.replaceAll("[^0-9]", "");
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';

            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    private boolean isValidIpAddress(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        for (String part : parts) {
            int num = Integer.parseInt(part);
            if (num < 0 || num > 255) return false;
        }

        return true;
    }

    private String maskValue(String value, PiiType type, MaskingStrategy strategy) {
        switch (strategy) {
            case FULL_MASK:
                return "***";

            case PARTIAL_MASK:
                return partialMask(value, type);

            case HASH_MASK:
                return "[" + type.name() + ":" + Integer.toHexString(value.hashCode()) + "]";

            case RANDOM_MASK:
                return generateRandomMask(type);

            case TYPE_LABEL:
                return "[" + type.name() + "]";

            default:
                return "***";
        }
    }

    private String partialMask(String value, PiiType type) {
        switch (type) {
            case EMAIL:
                int atIndex = value.indexOf('@');
                if (atIndex > 2) {
                    return value.substring(0, 2) + "***" + value.substring(atIndex);
                }
                return "***";

            case PHONE:
                if (value.length() == 11) {
                    return value.substring(0, 3) + "****" + value.substring(7);
                }
                return "***";

            case ID_NUMBER:
                if (value.length() == 18) {
                    return value.substring(0, 6) + "********" + value.substring(14);
                }
                return "***";

            case CREDIT_CARD:
                return "**** **** **** " + value.substring(value.length() - 4);

            default:
                return "***";
        }
    }

    private String generateRandomMask(PiiType type) {
        return "[" + type.name() + ":" + UUID.randomUUID().toString().substring(0, 8) + "]";
    }

    private String generatePlaceholder(PiiType type, int index) {
        return "<<" + type.name() + "_" + index + ">>";
    }

    private ContentSafetyChecker.RiskLevel calculateRiskLevel(List<PiiMatch> matches) {
        if (matches.isEmpty()) {
            return ContentSafetyChecker.RiskLevel.NONE;
        }

        // 根据检测到的 PII 类型和数量计算风险等级
        Set<PiiType> types = EnumSet.noneOf(PiiType.class);
        for (PiiMatch match : matches) {
            types.add(match.getType());
        }

        if (types.contains(PiiType.ID_NUMBER) || types.contains(PiiType.CREDIT_CARD) ||
                types.contains(PiiType.BANK_ACCOUNT)) {
            return ContentSafetyChecker.RiskLevel.HIGH;
        }

        if (types.size() > 2) {
            return ContentSafetyChecker.RiskLevel.MEDIUM;
        }

        return ContentSafetyChecker.RiskLevel.LOW;
    }

    private List<PiiType> getPiiTypes(List<PiiMatch> matches) {
        Set<PiiType> types = EnumSet.noneOf(PiiType.class);
        for (PiiMatch match : matches) {
            types.add(match.getType());
        }
        return new ArrayList<>(types);
    }

    /**
     * 默认 PII 检测结果
     */
    private static class DefaultPiiDetectionResult implements PiiDetectionResult {
        private final boolean hasPii;
        private final List<PiiMatch> piiMatches;
        private final List<PiiType> piiTypes;
        private final ContentSafetyChecker.RiskLevel riskLevel;

        DefaultPiiDetectionResult(boolean hasPii, List<PiiMatch> piiMatches, List<PiiType> piiTypes,
                                   ContentSafetyChecker.RiskLevel riskLevel) {
            this.hasPii = hasPii;
            this.piiMatches = piiMatches;
            this.piiTypes = piiTypes;
            this.riskLevel = riskLevel;
        }

        @Override
        public boolean hasPii() { return hasPii; }

        @Override
        @NonNull
        public List<PiiMatch> getPiiMatches() { return piiMatches; }

        @Override
        @NonNull
        public List<PiiType> getPiiTypes() { return piiTypes; }

        @Override
        public ContentSafetyChecker.@NonNull RiskLevel getRiskLevel() { return riskLevel; }
    }

    /**
     * 默认 PII 匹配
     */
    private static class DefaultPiiMatch implements PiiMatch {
        private final PiiType type;
        private final String text;
        private final int start;
        private final int end;
        private final double confidence;
        private final String maskedValue;

        DefaultPiiMatch(PiiType type, String text, int start, int end, double confidence, String maskedValue) {
            this.type = type;
            this.text = text;
            this.start = start;
            this.end = end;
            this.confidence = confidence;
            this.maskedValue = maskedValue;
        }

        @Override
        @NonNull
        public PiiType getType() { return type; }

        @Override
        @NonNull
        public String getText() { return text; }

        @Override
        public int getStart() { return start; }

        @Override
        public int getEnd() { return end; }

        @Override
        public double getConfidence() { return confidence; }

        @Override
        @NonNull
        public String getMaskedValue() { return maskedValue; }
    }

    /**
     * 默认脱敏结果
     */
    private static class DefaultPiiMaskingResult implements PiiMaskingResult {
        private final String maskedText;
        private final MaskingToken maskingToken;
        private final int maskedCount;
        private final List<PiiType> maskedTypes;

        DefaultPiiMaskingResult(String maskedText, MaskingToken maskingToken, int maskedCount, List<PiiType> maskedTypes) {
            this.maskedText = maskedText;
            this.maskingToken = maskingToken;
            this.maskedCount = maskedCount;
            this.maskedTypes = maskedTypes;
        }

        @Override
        @NonNull
        public String getMaskedText() { return maskedText; }

        @Override
        @NonNull
        public MaskingToken getMaskingToken() { return maskingToken; }

        @Override
        public int getMaskedCount() { return maskedCount; }

        @Override
        @NonNull
        public List<PiiType> getMaskedTypes() { return maskedTypes; }
    }

    /**
     * 默认脱敏令牌
     */
    private static class DefaultMaskingToken implements MaskingToken {
        private final String tokenId;
        private final Map<String, String> mappings;
        private final Map<String, String> reverseMappings;
        private final long createdAt;

        DefaultMaskingToken() {
            this.tokenId = UUID.randomUUID().toString();
            this.mappings = Collections.emptyMap();
            this.reverseMappings = Collections.emptyMap();
            this.createdAt = System.currentTimeMillis();
        }

        DefaultMaskingToken(Map<String, String> mappings, Map<String, String> reverseMappings) {
            this.tokenId = UUID.randomUUID().toString();
            this.mappings = new HashMap<>(mappings);
            this.reverseMappings = new HashMap<>(reverseMappings);
            this.createdAt = System.currentTimeMillis();
        }

        @Override
        @NonNull
        public String getTokenId() { return tokenId; }

        @Override
        @NonNull
        public Map<String, String> getMappings() { return mappings; }

        @Override
        @NonNull
        public Map<String, String> getReverseMappings() { return reverseMappings; }

        @Override
        public long getCreatedAt() { return createdAt; }

        @Override
        public boolean isExpired() {
            // 默认 1 小时过期
            return System.currentTimeMillis() - createdAt > 3600000;
        }
    }
}