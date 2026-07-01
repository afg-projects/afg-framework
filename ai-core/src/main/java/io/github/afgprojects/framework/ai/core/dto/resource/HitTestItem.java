package io.github.afgprojects.framework.ai.core.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命中测试结果项
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HitTestItem {

    /** 命中来源：knowledge（知识库检索）/ tool（工具匹配） */
    private String source;

    /** 引用 ID：知识库 ID 或工具名 */
    private String refId;

    /** 相关性分数（0~1，越高越相关） */
    private double score;

    /** 命中片段（检索文档片段 / 工具描述） */
    private String snippet;
}
