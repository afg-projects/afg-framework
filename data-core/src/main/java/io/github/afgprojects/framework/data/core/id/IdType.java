package io.github.afgprojects.framework.data.core.id;

import org.jspecify.annotations.NonNull;

/**
 * ID 类型枚举
 */
public enum IdType {

    /**
     * 数据库自增
     */
    AUTO("数据库自增"),

    /**
     * 无（手动输入）
     */
    NONE("无"),

    /**
     * 用户输入
     */
    INPUT("用户输入"),

    /**
     * ID Worker 序列号
     */
    ID_WORKER("分布式ID"),

    /**
     * ID Worker 字符串格式
     */
    ID_WORKER_STR("分布式ID字符串"),

    /**
     * UUID
     */
    UUID("UUID"),

    /**
     * UUID（无连字符）
     */
    UUID_HEX("UUID无连字符"),

    /**
     * 雪花算法
     */
    SNOWFLAKE("雪花算法"),

    /**
     * 时间戳ID
     */
    TIMESTAMP("时间戳ID");

    private final String description;

    IdType(String description) {
        this.description = description;
    }

    /**
     * 获取描述信息
     *
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 是否为数据库生成
     *
     * @return true表示数据库生成
     */
    public boolean isDbGenerated() {
        return this == AUTO;
    }

    /**
     * 是否需要应用生成
     *
     * @return true表示应用生成
     */
    public boolean isAppGenerated() {
        return this != AUTO && this != NONE && this != INPUT;
    }
}