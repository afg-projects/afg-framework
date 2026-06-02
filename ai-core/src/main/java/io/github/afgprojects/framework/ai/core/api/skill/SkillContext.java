package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill 执行上下文
 *
 * <p>携带执行 Skill 所需的所有信息，包括输入参数、会话信息等。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SkillContext {

    private final String skillName;
    private final Map<String, Object> inputs;
    private SkillDefinition skillDefinition;
    private String sessionId;
    private String userId;
    private Map<String, Object> metadata;

    public SkillContext(@NonNull String skillName, @NonNull Map<String, Object> inputs) {
        this.skillName = skillName;
        this.inputs = new HashMap<>(inputs);
        this.metadata = new HashMap<>();
    }

    public SkillContext(@NonNull String skillName) {
        this(skillName, new HashMap<>());
    }

    @NonNull
    public String getSkillName() {
        return skillName;
    }

    @NonNull
    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void addInput(@NonNull String key, @Nullable Object value) {
        inputs.put(key, value);
    }

    @Nullable
    public Object getInput(@NonNull String key) {
        return inputs.get(key);
    }

    @Nullable
    public <T> T getInput(@NonNull String key, @NonNull Class<T> type) {
        Object value = inputs.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    @Nullable
    public SkillDefinition getSkillDefinition() {
        return skillDefinition;
    }

    public void setSkillDefinition(@Nullable SkillDefinition skillDefinition) {
        this.skillDefinition = skillDefinition;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(@NonNull Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(@NonNull String key, @Nullable Object value) {
        metadata.put(key, value);
    }

    /**
     * 创建 Builder
     */
    public static Builder builder(@NonNull String skillName) {
        return new Builder(skillName);
    }

    public static class Builder {
        private final String skillName;
        private final Map<String, Object> inputs = new HashMap<>();
        private SkillDefinition skillDefinition;
        private String sessionId;
        private String userId;
        private final Map<String, Object> metadata = new HashMap<>();

        private Builder(String skillName) {
            this.skillName = skillName;
        }

        public Builder input(@NonNull String key, @Nullable Object value) {
            inputs.put(key, value);
            return this;
        }

        public Builder inputs(@NonNull Map<String, Object> inputs) {
            this.inputs.putAll(inputs);
            return this;
        }

        public Builder skillDefinition(@Nullable SkillDefinition skillDefinition) {
            this.skillDefinition = skillDefinition;
            return this;
        }

        public Builder sessionId(@Nullable String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        public Builder metadata(@NonNull String key, @Nullable Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public SkillContext build() {
            SkillContext context = new SkillContext(skillName, inputs);
            context.setSkillDefinition(skillDefinition);
            context.setSessionId(sessionId);
            context.setUserId(userId);
            context.setMetadata(metadata);
            return context;
        }
    }
}