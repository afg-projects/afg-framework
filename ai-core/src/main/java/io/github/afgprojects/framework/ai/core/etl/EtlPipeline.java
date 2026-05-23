package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * ETL Pipeline 接口。
 *
 * <p>提供轻量级的文档处理管道，组合 Reader、Transformer、Writer。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface EtlPipeline {

    /**
     * 执行 Pipeline（单源）。
     *
     * @param source 数据源
     * @return 执行结果
     */
    @NonNull
    EtlResult execute(@NonNull Source source);

    /**
     * 执行 Pipeline（多源）。
     *
     * @param sources 数据源列表
     * @return 执行结果
     */
    @NonNull
    EtlResult executeAll(@NonNull List<Source> sources);

    /**
     * 创建 Pipeline 构建器。
     *
     * @return 构建器实例
     */
    @NonNull
    static EtlPipelineBuilder builder() {
        return new EtlPipelineBuilder();
    }
}