package io.github.afgprojects.framework.security.core.security.model;

import org.jspecify.annotations.Nullable;

/**
 * IP 限制规则。
 *
 * <p>用于定义 IP 白名单或黑名单规则，支持通配符匹配。
 *
 * @since 1.0.0
 */
public class IpRestrictionRule {

    /**
     * 规则类型。
     */
    public enum Type {
        /**
         * 白名单规则。
         */
        WHITELIST,

        /**
         * 黑名单规则。
         */
        BLACKLIST
    }

    private Type type;

    private String ipPattern;

    @Nullable
    private String description;

    /**
     * 默认构造函数。
     */
    public IpRestrictionRule() {
    }

    /**
     * 构造函数。
     *
     * @param type       规则类型
     * @param ipPattern  IP 匹配模式（支持通配符 *）
     * @param description 规则描述
     */
    public IpRestrictionRule(Type type, String ipPattern, @Nullable String description) {
        this.type = type;
        this.ipPattern = ipPattern;
        this.description = description;
    }

    /**
     * 判断给定 IP 是否匹配此规则。
     *
     * <p>支持通配符 * 匹配，例如：
     * <ul>
     *   <li>192.168.1.1 - 精确匹配</li>
     *   <li>192.168.1.* - 匹配 192.168.1.0-255</li>
     *   <li>192.168.*.* - 匹配 192.168.0.0-255.0-255</li>
     *   <li>* - 匹配所有 IP</li>
     * </ul>
     *
     * @param ip 要检查的 IP 地址
     * @return 如果匹配则返回 true
     */
    public boolean matches(String ip) {
        if (ipPattern == null || ip == null) {
            return false;
        }

        // 将通配符模式转换为正则表达式
        String regex = ipPattern
                .replace(".", "\\.")
                .replace("*", "\\d+");

        return ip.matches(regex);
    }

    /**
     * 获取规则类型。
     *
     * @return 规则类型
     */
    public Type getType() {
        return type;
    }

    /**
     * 设置规则类型。
     *
     * @param type 规则类型
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * 获取 IP 匹配模式。
     *
     * @return IP 匹配模式
     */
    public String getIpPattern() {
        return ipPattern;
    }

    /**
     * 设置 IP 匹配模式。
     *
     * @param ipPattern IP 匹配模式
     */
    public void setIpPattern(String ipPattern) {
        this.ipPattern = ipPattern;
    }

    /**
     * 获取规则描述。
     *
     * @return 规则描述，可能为 null
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * 设置规则描述。
     *
     * @param description 规则描述
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }
}
