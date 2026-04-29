package io.github.afgprojects.framework.core.model.version;

import org.jspecify.annotations.NonNull;

/**
 * API 版本
 * 语义化版本号表示
 */
public record ApiVersion(int major, int minor, int patch) implements Comparable<ApiVersion> {

    /**
     * 解析版本字符串
     *
     * @param version 版本字符串，如 "1.2.3"
     * @return ApiVersion 实例
     * @throws IllegalArgumentException 如果版本格式无效
     */
    @NonNull public static ApiVersion parse(@NonNull String version) {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid version format: " + version + ", expected: major.minor.patch");
        }
        try {
            return new ApiVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + version, e);
        }
    }

    /**
     * 创建初始版本 (0.0.0)
     */
    public static ApiVersion initial() {
        return new ApiVersion(0, 0, 0);
    }

    /**
     * 判断是否比另一个版本新
     */
    public boolean isNewerThan(@NonNull ApiVersion other) {
        return this.compareTo(other) > 0;
    }

    /**
     * 判断是否比另一个版本旧
     */
    public boolean isOlderThan(@NonNull ApiVersion other) {
        return this.compareTo(other) < 0;
    }

    /**
     * 判断是否与另一个版本兼容
     * 主版本号相同则认为兼容
     */
    public boolean isCompatibleWith(@NonNull ApiVersion other) {
        return this.major == other.major;
    }

    /**
     * 判断是否是主版本变更
     */
    public boolean isMajorChange(@NonNull ApiVersion other) {
        return this.major != other.major;
    }

    /**
     * 判断是否是次版本变更
     */
    public boolean isMinorChange(@NonNull ApiVersion other) {
        return this.major == other.major && this.minor != other.minor;
    }

    /**
     * 判断是否是补丁变更
     */
    public boolean isPatchChange(@NonNull ApiVersion other) {
        return this.major == other.major && this.minor == other.minor && this.patch != other.patch;
    }

    @Override
    public int compareTo(@NonNull ApiVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
