package io.github.afgprojects.framework.ai.core.provider;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商模板注册表，预定义常见 AI 供应商模板
 */
@Component
public class ProviderTemplateRegistry {

    private static final List<ProviderTemplate> TEMPLATES = List.of(
        new ProviderTemplate(
            "openai", "OpenAI", "https://api.openai.com/v1",
            "openai",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "sk-...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "OpenAI 官方 API，支持 GPT-4o、GPT-4o-mini 等模型"
        ),
        new ProviderTemplate(
            "deepseek", "DeepSeek", "https://api.deepseek.com",
            "deepseek",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "sk-...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "DeepSeek 深度求索，支持 DeepSeek-V3、DeepSeek-R1 等模型"
        ),
        new ProviderTemplate(
            "zhipu", "智谱AI", "https://open.bigmodel.cn/api/paas/v4",
            "zhipu",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "智谱AI，支持 GLM-4、GLM-4-Flash 等模型"
        ),
        new ProviderTemplate(
            "qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "qwen",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "sk-...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "阿里云通义千问，支持 Qwen-Max、Qwen-Plus 等模型"
        ),
        new ProviderTemplate(
            "kimi", "Kimi (月之暗面)", "https://api.moonshot.cn/v1",
            "kimi",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "sk-...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "Moonshot AI，支持 moonshot-v1-8k、moonshot-v1-32k 等模型"
        ),
        new ProviderTemplate(
            "yi", "零一万物", "https://api.lingyiwanwu.com/v1",
            "yi",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "零一万物，支持 Yi-Large、Yi-Medium 等模型"
        ),
        new ProviderTemplate(
            "gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "Google Gemini，支持 gemini-2.0-flash、gemini-1.5-pro 等模型"
        ),
        new ProviderTemplate(
            "azure_openai", "Azure OpenAI", "",
            "azure",
            List.of(
                new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "..."),
                new ProviderTemplate.CredentialField("endpoint", "Endpoint", "text", true, "https://xxx.openai.azure.com/"),
                new ProviderTemplate.CredentialField("deployment_name", "Deployment Name", "text", true, "gpt-4o")
            ),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "Microsoft Azure OpenAI Service"
        ),
        new ProviderTemplate(
            "volcanic", "火山引擎 (豆包)", "https://ark.cn-beijing.volces.com/api/v3",
            "volcanic",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "字节跳动火山引擎，支持 Doubao-pro 等模型"
        ),
        new ProviderTemplate(
            "minimax", "MiniMax", "https://api.minimax.chat/v1",
            "minimax",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "MiniMax，支持 abab6.5s-chat 等模型"
        ),
        new ProviderTemplate(
            "baichuan", "百川智能", "https://api.baichuan-ai.com/v1",
            "baichuan",
            List.of(new ProviderTemplate.CredentialField("api_key", "API Key", "password", true, "...")),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "百川智能，支持 Baichuan4 等模型"
        ),
        new ProviderTemplate(
            "ollama", "Ollama", "http://localhost:11434",
            "ollama",
            List.of(),
            ProviderTemplate.ProviderCategory.OLLAMA.name(),
            "本地大模型运行工具，支持 Llama、Qwen、DeepSeek 等开源模型"
        ),
        new ProviderTemplate(
            "xinference", "Xinference", "http://localhost:9997/v1",
            "xinference",
            List.of(),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "分布式模型推理框架，支持多种开源模型统一部署"
        ),
        new ProviderTemplate(
            "local_ai", "LocalAI", "http://localhost:8080/v1",
            "localai",
            List.of(),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "本地 AI 推理服务，替代 OpenAI API 的开源方案"
        ),
        new ProviderTemplate(
            "custom", "自定义 (OpenAI 兼容)", "",
            "custom",
            List.of(
                new ProviderTemplate.CredentialField("api_key", "API Key", "password", false, "..."),
                new ProviderTemplate.CredentialField("base_url", "Base URL", "text", true, "https://your-api.com/v1")
            ),
            ProviderTemplate.ProviderCategory.OPENAI_COMPATIBLE.name(),
            "兼容 OpenAI API 格式的自定义服务"
        )
    );

    private final Map<String, ProviderTemplate> templateMap = new LinkedHashMap<>();

    public ProviderTemplateRegistry() {
        for (var template : TEMPLATES) {
            templateMap.put(template.type(), template);
        }
    }

    public List<ProviderTemplate> getTemplates() {
        return new ArrayList<>(templateMap.values());
    }

    public ProviderTemplate getByType(String type) {
        return templateMap.get(type);
    }
}
