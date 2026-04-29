package io.github.afgprojects.framework.core.model.exception;

/**
 * 错误码范围定义
 * <p>
 * 范围划分原则：
 * <ul>
 *     <li>10000-19999: 通用模块（核心错误码）</li>
 *     <li>20000-29999: 认证授权模块</li>
 *     <li>30000-39999: 业务模块</li>
 *     <li>90000-99999: 系统模块</li>
 * </ul>
 * 通用模块内部细分：
 * <ul>
 *     <li>10000-10099: 成功与通用错误</li>
 *     <li>10100-10199: 资源错误</li>
 *     <li>10200-10299: 请求错误</li>
 *     <li>10300-10399: 限流错误</li>
 *     <li>10400-10499: 认证授权错误（轻量级，完整认证在 20000+）</li>
 *     <li>11000-11999: 数据层错误</li>
 *     <li>12000-12999: 存储错误</li>
 *     <li>13000-13999: 任务错误</li>
 *     <li>14000-14999: HTTP客户端错误</li>
 *     <li>15000-15999: 模块错误</li>
 *     <li>16000-16999: 配置错误</li>
 *     <li>17000-17999: 功能开关错误</li>
 *     <li>19000-19999: 系统错误</li>
 * </ul>
 */
public enum ErrorCodeRange {

    /**
     * 通用错误码范围 (10000-19999)
     * <p>
     * 包含核心模块的所有错误码，内部按功能细分
     */
    COMMON(10000, 19999, "通用模块"),

    /**
     * 认证授权错误码范围 (20000-29999)
     */
    AUTH(20000, 29999, "认证授权模块"),

    /**
     * 业务错误码范围 (30000-39999)
     */
    BUSINESS(30000, 39999, "业务模块"),

    /**
     * 系统错误码范围 (90000-99999)
     */
    SYSTEM(90000, 99999, "系统模块");

    private final int start;
    private final int end;
    private final String description;

    ErrorCodeRange(int start, int end, String description) {
        this.start = start;
        this.end = end;
        this.description = description;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查错误码是否在范围内
     */
    public boolean contains(int code) {
        return code >= start && code <= end;
    }

    /**
     * 根据错误码获取对应的范围
     */
    public static ErrorCodeRange fromCode(int code) {
        for (ErrorCodeRange range : values()) {
            if (range.contains(code)) {
                return range;
            }
        }
        return SYSTEM;
    }
}
