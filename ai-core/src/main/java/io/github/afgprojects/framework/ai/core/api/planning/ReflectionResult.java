package io.github.afgprojects.framework.ai.core.api.planning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reflection 执行结果
 *
 * <p>封装 Reflection (反思) 模式的执行结果，包含：
 * <ul>
 *   <li>响应 - 初始响应结果</li>
 *   <li>反思 - 对响应的反思和改进建议</li>
 *   <li>成功标志 - 是否成功完成反思</li>
 * </ul>
 *
 * <p>Reflection 模式是一种自我改进的问题解决方法：
 * <pre>
 * 1. 生成初始响应 (Response)
 * 2. 反思响应质量 (Reflection)
 *    - 是否正确？
 *    - 是否完整？
 *    - 是否可以改进？
 * 3. 根据反思改进响应
 * 4. 可选：多轮反思迭代
 * </pre>
 *
 * <p>反思示例：
 * <pre>
 * Task: 计算 15 * 17
 * Response: 255
 * Reflection:
 *   - 我使用了分配律：15 * 17 = 15 * (10 + 7) = 150 + 105 = 255
 *   - 验证：15 * 17 = (20 - 5) * 17 = 340 - 85 = 255 ✓
 *   - 结果正确
 * Success: true
 * </pre>
 *
 * @param response   初始响应
 * @param reflection 反思内容
 * @param success    是否成功
 * @author afg-projects
 * @since 1.0.0
 */
public record ReflectionResult(
    @Nullable Object response,
    @Nullable String reflection,
    boolean success
) {

    /**
     * 创建成功的结果
     *
     * @param response   响应对象
     * @param reflection 反思内容
     * @return 成功结果
     */
    @NonNull
    public static ReflectionResult success(@Nullable Object response, @Nullable String reflection) {
        return new ReflectionResult(response, reflection, true);
    }

    /**
     * 创建失败的结果
     *
     * @param response   响应对象
     * @param reflection 错误反思
     * @return 失败结果
     */
    @NonNull
    public static ReflectionResult failure(@Nullable Object response, @Nullable String reflection) {
        return new ReflectionResult(response, reflection, false);
    }

    /**
     * 创建仅响应的结果（无反思）
     *
     * @param response 响应对象
     * @return 成功结果
     */
    @NonNull
    public static ReflectionResult ofResponse(@Nullable Object response) {
        return new ReflectionResult(response, null, true);
    }

    /**
     * 创建仅反思的结果（无响应）
     *
     * @param reflection 反思内容
     * @return 成功结果
     */
    @NonNull
    public static ReflectionResult ofReflection(@Nullable String reflection) {
        return new ReflectionResult(null, reflection, true);
    }

    /**
     * 判断是否有响应
     *
     * @return 是否有响应
     */
    public boolean hasResponse() {
        return response != null;
    }

    /**
     * 判断是否有反思
     *
     * @return 是否有反思
     */
    public boolean hasReflection() {
        return reflection != null && !reflection.isEmpty();
    }

    /**
     * 获取响应的字符串表示
     *
     * @return 响应字符串，如果无响应则返回 null
     */
    @Nullable
    public String responseAsString() {
        return response != null ? response.toString() : null;
    }
}
