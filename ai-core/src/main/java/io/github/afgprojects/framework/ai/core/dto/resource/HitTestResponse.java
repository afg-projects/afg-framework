package io.github.afgprojects.framework.ai.core.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 命中测试响应
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HitTestResponse {

    private List<HitTestItem> results;
}
