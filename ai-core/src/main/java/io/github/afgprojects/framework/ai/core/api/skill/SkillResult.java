package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Skill 执行结果
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SkillResult {

    private final boolean success;
    private final String skillName;
    private final String output;
    private final String errorMessage;
    private final Map<String, Object> data;
    private final long executionTimeMs;

    private SkillResult(boolean success, String skillName, String output, String errorMessage,
                        Map<String, Object> data, long executionTimeMs) {
        this.success = success;
        this.skillName = skillName;
        this.output = output;
        this.errorMessage = errorMessage;
        this.data = data;
        this.executionTimeMs = executionTimeMs;
    }

    @NonNull
    public static SkillResult success(@NonNull String skillName, @NonNull String output) {
        return new SkillResult(true, skillName, output, null, Map.of(), 0);
    }

    @NonNull
    public static SkillResult success(@NonNull String skillName, @NonNull String output,
                                      @NonNull Map<String, Object> data) {
        return new SkillResult(true, skillName, output, null, data, 0);
    }

    @NonNull
    public static SkillResult success(@NonNull String skillName, @NonNull String output,
                                      @NonNull Map<String, Object> data, long executionTimeMs) {
        return new SkillResult(true, skillName, output, null, data, executionTimeMs);
    }

    @NonNull
    public static SkillResult failure(@NonNull String skillName, @NonNull String errorMessage) {
        return new SkillResult(false, skillName, null, errorMessage, Map.of(), 0);
    }

    @NonNull
    public static SkillResult failure(@NonNull String skillName, @NonNull String errorMessage,
                                      @NonNull Map<String, Object> data) {
        return new SkillResult(false, skillName, null, errorMessage, data, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    @NonNull
    public String getSkillName() {
        return skillName;
    }

    @Nullable
    public String getOutput() {
        return output;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @NonNull
    public Map<String, Object> getData() {
        return data;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String toString() {
        if (success) {
            return "SkillResult[success=" + success + ", skill=" + skillName +
                    ", output=" + (output != null && output.length() > 100 ? output.substring(0, 100) + "..." : output) +
                    ", time=" + executionTimeMs + "ms]";
        }
        return "SkillResult[success=" + success + ", skill=" + skillName +
                ", error=" + errorMessage + "]";
    }
}