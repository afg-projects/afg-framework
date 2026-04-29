package io.github.afgprojects.framework.core.web.security.signature;

/**
 * 签名算法枚举
 */
public enum SignatureAlgorithm {

    /**
     * HMAC-SHA256 签名算法
     */
    HMAC_SHA256("HmacSHA256", "HS256"),

    /**
     * HMAC-SHA384 签名算法
     */
    HMAC_SHA384("HmacSHA384", "HS384"),

    /**
     * HMAC-SHA512 签名算法
     */
    HMAC_SHA512("HmacSHA512", "HS512");

    private final String algorithm;
    private final String shortName;

    SignatureAlgorithm(String algorithm, String shortName) {
        this.algorithm = algorithm;
        this.shortName = shortName;
    }

    /**
     * 获取算法名称
     *
     * @return 算法名称
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 获取简称
     *
     * @return 简称
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * 根据简称获取算法
     *
     * @param shortName 简称
     * @return 签名算法，如果未找到返回 null
     */
    public static SignatureAlgorithm fromShortName(String shortName) {
        for (SignatureAlgorithm algorithm : values()) {
            if (algorithm.shortName.equalsIgnoreCase(shortName)) {
                return algorithm;
            }
        }
        return null;
    }
}
