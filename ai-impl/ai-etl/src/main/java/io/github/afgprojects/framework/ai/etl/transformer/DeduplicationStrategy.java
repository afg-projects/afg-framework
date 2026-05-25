package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.rag.Document;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 去重策略接口。
 *
 * <p>定义文档去重的策略。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DeduplicationStrategy {

    /**
     * 计算文档的唯一标识。
     *
     * <p>相同标识的文档被视为重复。
     *
     * @param document 文档
     * @return 唯一标识
     */
    @NonNull
    String computeKey(@NonNull Document document);

    /**
     * 获取策略名称。
     *
     * @return 策略名称
     */
    default @NonNull String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 内容去重策略。
     *
     * <p>基于文档内容的 MD5 哈希进行去重。
     */
    static DeduplicationStrategy contentBased() {
        return new ContentBasedStrategy();
    }

    /**
     * ID 去重策略。
     *
     * <p>基于文档 ID 进行去重。
     */
    static DeduplicationStrategy idBased() {
        return new IdBasedStrategy();
    }
}

/**
 * 内容去重策略实现。
 */
class ContentBasedStrategy implements DeduplicationStrategy {

    @Override
    public @NonNull String computeKey(@NonNull Document document) {
        return String.valueOf(document.content().hashCode());
    }

    @Override
    public @NonNull String getName() {
        return "ContentBasedStrategy";
    }
}

/**
 * ID 去重策略实现。
 */
class IdBasedStrategy implements DeduplicationStrategy {

    @Override
    public @NonNull String computeKey(@NonNull Document document) {
        return document.id();
    }

    @Override
    public @NonNull String getName() {
        return "IdBasedStrategy";
    }
}