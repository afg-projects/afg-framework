package io.github.afgprojects.framework.ai.core.provider;

import java.util.List;

/**
 * 供应商模板定义，预定义常见 AI 供应商的连接配置
 */
public record ProviderTemplate(
    String type,
    String displayName,
    String baseUrl,
    String icon,
    List<CredentialField> credentialFields,
    String providerCategory,
    String description
) {

    public enum ProviderCategory {
        OPENAI_COMPATIBLE, OLLAMA, CUSTOM
    }

    public record CredentialField(
        String name,
        String label,
        String type,       // text, password
        boolean required,
        String placeholder
    ) {}
}
