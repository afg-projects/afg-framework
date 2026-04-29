package io.github.afgprojects.framework.core.web.version;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * API 版本信息
 * 封装版本元数据，用于版本路由和兼容性检查
 *
 * @param value    版本号（格式: major.minor）
 * @param major    主版本号
 * @param minor    次版本号
 * @param deprecated 是否已废弃
 * @param since    引入版本
 * @param until    废弃版本
 * @param replacement 替代方案
 * @param reason   废弃原因
 */
public record ApiVersionInfo(
        @NonNull String value,
        int major,
        int minor,
        boolean deprecated,
        @Nullable String since,
        @Nullable String until,
        @Nullable String replacement,
        @Nullable String reason) {

    /**
     * 从 @ApiVersion 注解创建 ApiVersionInfo
     *
     * @param annotation @ApiVersion 注解
     * @return ApiVersionInfo 实例
     */
    @NonNull public static ApiVersionInfo from(@NonNull ApiVersion annotation) {
        String version = annotation.value();
        int[] parts = parseVersion(version);

        String since = annotation.since().isBlank() ? null : annotation.since();
        String until = annotation.until().isBlank() ? null : annotation.until();
        String replacement = annotation.replacement().isBlank() ? null : annotation.replacement();
        String reason = annotation.reason().isBlank() ? null : annotation.reason();

        return new ApiVersionInfo(
                version, parts[0], parts[1], annotation.deprecated(), since, until, replacement, reason);
    }

    /**
     * 解析版本字符串
     *
     * @param version 版本字符串（如 "1.0" 或 "1.0.0"）
     * @return [major, minor] 数组
     * @throws IllegalArgumentException 如果版本格式无效
     */
    public static int @NonNull [] parseVersion(@NonNull String version) {
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid version format: " + version + ", expected: major.minor");
        }
        try {
            return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + version, e);
        }
    }

    /**
     * 创建默认版本信息
     *
     * @param version 版本字符串
     * @return ApiVersionInfo 实例
     */
    @NonNull public static ApiVersionInfo of(@NonNull String version) {
        int[] parts = parseVersion(version);
        return new ApiVersionInfo(version, parts[0], parts[1], false, null, null, null, null);
    }

    /**
     * 判断是否与指定版本兼容
     * 主版本号相同则认为兼容
     *
     * @param otherMajor 其他版本的主版本号
     * @return 是否兼容
     */
    public boolean isCompatibleWith(int otherMajor) {
        return this.major == otherMajor;
    }

    /**
     * 判断版本是否在支持范围内
     *
     * @param requestMajor 请求版本的主版本号
     * @return 是否在支持范围内
     */
    public boolean isInRange(int requestMajor) {
        // 如果有 since 限制，检查主版本号是否 >= since 的主版本号
        if (since != null) {
            int[] sinceParts = parseVersion(since);
            if (requestMajor < sinceParts[0]) {
                return false;
            }
        }
        // 如果有 until 限制，检查主版本号是否 < until 的主版本号
        if (until != null) {
            int[] untilParts = parseVersion(until);
            if (requestMajor >= untilParts[0]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较版本号
     *
     * @param other 其他版本信息
     * @return 比较结果
     */
    public int compareTo(@NonNull ApiVersionInfo other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        return Integer.compare(this.minor, other.minor);
    }

    /**
     * 判断是否比指定版本新
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @return 是否更新
     */
    public boolean isNewerThan(int major, int minor) {
        if (this.major != major) {
            return this.major > major;
        }
        return this.minor > minor;
    }

    /**
     * 构建废弃警告信息
     *
     * @return 废弃警告信息，如果不废弃则返回 null
     */
    public @Nullable String buildDeprecationWarning() {
        if (!deprecated) {
            return null;
        }

        StringBuilder warning = new StringBuilder("API version ");
        warning.append(value);
        warning.append(" is deprecated");

        if (until != null) {
            warning.append(" and will be removed in version ").append(until);
        }

        if (replacement != null) {
            warning.append(". Use ").append(replacement).append(" instead");
        }

        if (reason != null) {
            warning.append(". Reason: ").append(reason);
        }

        return warning.toString();
    }
}