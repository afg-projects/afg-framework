package io.github.afgprojects.framework.ai.etl.cleaner;

import org.jspecify.annotations.NonNull;

/**
 * 内容清洗器接口。
 *
 * <p>定义文本内容清洗的核心方法。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface ContentCleaner {

    /**
     * 清洗文本内容。
     *
     * @param content 原始内容
     * @return 清洗后的内容
     */
    @NonNull
    String clean(@NonNull String content);

    /**
     * 获取清洗器名称。
     *
     * @return 清洗器名称
     */
    default @NonNull String getName() {
        return this.getClass().getSimpleName();
    }
}
