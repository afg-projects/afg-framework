package io.github.afgprojects.framework.core.properties.security;

import java.util.HashMap;
import java.util.Map;

import io.github.afgprojects.framework.core.web.security.signature.SignatureAlgorithm;
import lombok.Data;

/**
 * 签名验证配置。
 */
@Data
public class AfgCoreSignatureProperties {

    private boolean enabled = true;
    private String defaultKeyId = "default";
    private Map<String, AfgCoreSignatureKeyProperties> keys = new HashMap<>();
    private int timestampTolerance = 300;
    private int nonceCacheSize = 10000;
    private boolean nonceRequired = true;
    private SignatureAlgorithm defaultAlgorithm = SignatureAlgorithm.HMAC_SHA256;
}