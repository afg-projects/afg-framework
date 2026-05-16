package io.github.afgprojects.framework.security.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.afgprojects.framework.security.core.security.IpRestrictionChecker;
import io.github.afgprojects.framework.security.core.security.model.IpRestrictionRule;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 默认 IP 限制检查器实现。
 *
 * <p>支持以下匹配方式：
 * <ul>
 *   <li>精确匹配：192.168.1.100</li>
 *   <li>通配符匹配：192.168.1.*、192.168.*.*、*</li>
 *   <li>CIDR 表示法：192.168.1.0/24、10.0.0.0/8</li>
 * </ul>
 *
 * <p>使用 Caffeine 缓存提高检查性能，缓存默认过期时间为 5 分钟。
 *
 * @since 1.0.0
 */
public class DefaultIpRestrictionChecker implements IpRestrictionChecker {

    /**
     * 白名单规则缓存结果
     */
    private static final String CACHE_KEY_WHITELIST = "whitelist";

    /**
     * 黑名单规则缓存结果
     */
    private static final String CACHE_KEY_BLACKLIST = "blacklist";

    /**
     * 白名单规则列表
     */
    @Nullable
    private volatile List<IpRestrictionRule> whitelistRules;

    /**
     * 黑名单规则列表
     */
    @Nullable
    private volatile List<IpRestrictionRule> blacklistRules;

    /**
     * IP 检查结果缓存
     * Key: ip:type (type 为 whitelist 或 blacklist)
     * Value: 检查结果
     */
    private final Cache<String, Boolean> ipCheckCache;

    /**
     * 默认构造函数，使用默认缓存配置（5 分钟过期，最大 10000 条记录）。
     */
    public DefaultIpRestrictionChecker() {
        this(5, TimeUnit.MINUTES, 10000);
    }

    /**
     * 构造函数，允许自定义缓存配置。
     *
     * @param expireDuration 缓存过期时间
     * @param expireUnit     时间单位
     * @param maxSize        最大缓存条目数
     */
    public DefaultIpRestrictionChecker(long expireDuration, TimeUnit expireUnit, int maxSize) {
        this.ipCheckCache = Caffeine.newBuilder()
                .expireAfterWrite(expireDuration, expireUnit)
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public boolean isAllowed(String ip, @Nullable String userId, @Nullable String tenantId) {
        // 白名单优先：如果在白名单中，直接允许
        if (isWhitelisted(ip)) {
            return true;
        }

        // 如果在黑名单中，拒绝访问
        if (isBlacklisted(ip)) {
            return false;
        }

        // 默认允许访问
        return true;
    }

    @Override
    public boolean isBlacklisted(String ip) {
        return checkIpInRules(ip, CACHE_KEY_BLACKLIST, blacklistRules);
    }

    @Override
    public boolean isWhitelisted(String ip) {
        return checkIpInRules(ip, CACHE_KEY_WHITELIST, whitelistRules);
    }

    /**
     * 检查 IP 是否在指定规则列表中。
     *
     * @param ip       IP 地址
     * @param cacheKey 缓存键前缀
     * @param rules    规则列表
     * @return 如果匹配则返回 true
     */
    private boolean checkIpInRules(String ip, String cacheKey, @Nullable List<IpRestrictionRule> rules) {
        // 参数校验
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        if (rules == null || rules.isEmpty()) {
            return false;
        }

        // 尝试从缓存获取结果
        String cacheKeyWithIp = cacheKey + ":" + ip;
        Boolean cachedResult = ipCheckCache.getIfPresent(cacheKeyWithIp);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 执行检查
        boolean result = matchAnyRule(ip, rules);

        // 缓存结果
        ipCheckCache.put(cacheKeyWithIp, result);

        return result;
    }

    /**
     * 检查 IP 是否匹配任意规则。
     *
     * @param ip    IP 地址
     * @param rules 规则列表
     * @return 如果匹配任意规则则返回 true
     */
    private boolean matchAnyRule(String ip, List<IpRestrictionRule> rules) {
        for (IpRestrictionRule rule : rules) {
            if (rule != null && matchIp(ip, rule.getIpPattern())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 IP 是否匹配指定模式。
     *
     * <p>支持以下模式：
     * <ul>
     *   <li>精确匹配：192.168.1.100</li>
     *   <li>通配符匹配：192.168.1.*、192.168.*.*、*</li>
     *   <li>CIDR 表示法：192.168.1.0/24</li>
     * </ul>
     *
     * @param ip      IP 地址
     * @param pattern 匹配模式
     * @return 如果匹配则返回 true
     */
    private boolean matchIp(String ip, @Nullable String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        // CIDR 表示法
        if (pattern.contains("/")) {
            return matchCidr(ip, pattern);
        }

        // 通配符匹配
        if (pattern.contains("*")) {
            return matchWildcard(ip, pattern);
        }

        // 精确匹配
        return ip.equals(pattern);
    }

    /**
     * 通配符匹配。
     *
     * @param ip      IP 地址
     * @param pattern 包含通配符的模式
     * @return 如果匹配则返回 true
     */
    private boolean matchWildcard(String ip, String pattern) {
        // 特殊情况：单个 * 匹配所有 IP
        if ("*".equals(pattern)) {
            return true;
        }

        // 将通配符模式转换为正则表达式
        // * 匹配 0-255 的数字
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");

        return ip.matches(regex);
    }

    /**
     * CIDR 匹配。
     *
     * @param ip      IP 地址
     * @param cidr    CIDR 表示法（如 192.168.1.0/24）
     * @return 如果匹配则返回 true
     */
    private boolean matchCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            String networkAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            if (prefixLength < 0 || prefixLength > 32) {
                return false;
            }

            // 解析 IP 地址
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkAddress);

            // 计算网络掩码
            long mask = prefixLength == 0 ? 0 : (-1L << (32 - prefixLength));

            // 检查 IP 是否在网络范围内
            return (ipLong & mask) == (networkLong & mask);

        } catch (Exception e) {
            // 解析失败，返回 false
            return false;
        }
    }

    /**
     * 将 IP 地址转换为长整型。
     *
     * @param ip IP 地址
     * @return 长整型表示
     * @throws UnknownHostException 如果 IP 地址格式无效
     */
    private long ipToLong(String ip) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(ip).getAddress();
        long result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    /**
     * 设置白名单规则。
     *
     * <p>设置新规则后会清除相关缓存。
     *
     * @param rules 白名单规则列表
     */
    public void setWhitelistRules(@Nullable List<IpRestrictionRule> rules) {
        this.whitelistRules = rules;
        clearCacheForType(CACHE_KEY_WHITELIST);
    }

    /**
     * 设置黑名单规则。
     *
     * <p>设置新规则后会清除相关缓存。
     *
     * @param rules 黑名单规则列表
     */
    public void setBlacklistRules(@Nullable List<IpRestrictionRule> rules) {
        this.blacklistRules = rules;
        clearCacheForType(CACHE_KEY_BLACKLIST);
    }

    /**
     * 清除指定类型的缓存。
     *
     * @param cacheKey 缓存键前缀
     */
    private void clearCacheForType(String cacheKey) {
        // 由于 Caffeine 不支持按前缀删除，这里清除所有缓存
        // 在实际生产环境中，可以考虑使用更复杂的缓存结构
        ipCheckCache.invalidateAll();
    }

    /**
     * 清除所有缓存。
     */
    public void clearCache() {
        ipCheckCache.invalidateAll();
    }

    /**
     * 获取白名单规则列表。
     *
     * @return 白名单规则列表
     */
    @Nullable
    public List<IpRestrictionRule> getWhitelistRules() {
        return whitelistRules;
    }

    /**
     * 获取黑名单规则列表。
     *
     * @return 黑名单规则列表
     */
    @Nullable
    public List<IpRestrictionRule> getBlacklistRules() {
        return blacklistRules;
    }
}
