package io.github.afgprojects.framework.core.api.enummanagement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

/**
 * 枚举管理 REST 端点。
 * <p>
 * 暴露枚举元数据给前端，供下拉框、选项卡等 UI 组件使用。
 * 仅在 SERVLET Web 环境下注册。
 *
 * <h2>端点列表</h2>
 * <ul>
 *   <li>{@code GET /afg/enums} — 获取所有枚举元数据</li>
 *   <li>{@code GET /afg/enums/{name}} — 获取指定枚举的元数据</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
@RequestMapping("${afg.core.enum-management.endpoint-path:/afg/enums}")
public class EnumManagementEndpoint {

    private final EnumRegistry enumRegistry;

    /**
     * 获取所有枚举元数据。
     *
     * @return 枚举元数据列表
     */
    @GetMapping
    public List<EnumMetadata> list() {
        return enumRegistry.getAllMetadata();
    }

    /**
     * 获取指定枚举的元数据。
     *
     * @param name 枚举名称（简单类名）
     * @return 枚举元数据，不存在返回 404
     */
    @GetMapping("/{name}")
    public ResponseEntity<EnumMetadata> get(@PathVariable String name) {
        EnumMetadata metadata = enumRegistry.getMetadata(name);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }
}
