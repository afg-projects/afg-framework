package io.github.afgprojects.framework.governance.client.properties.common;

import lombok.Data;

/**
 * Governance 客户端签名认证配置。
 */
@Data
public class GovernanceSignatureProperties {

    private boolean enabled = false;
    private String keyId = "governance-client";
    private String secret;
}
