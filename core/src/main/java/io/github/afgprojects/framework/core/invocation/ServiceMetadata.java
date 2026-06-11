package io.github.afgprojects.framework.core.invocation;

import java.util.List;

/**
 * 服务元数据接口。
 * <p>
 * 由 APT 处理器根据 @AfService 注解自动生成的元数据类实现此接口。
 * 提供服务的名称、描述、分类、标签、图标、示例和操作列表。
 *
 * @param <T> 服务类型
 * @see io.github.afgprojects.framework.apt.api.AfService
 */
public interface ServiceMetadata<T> {

    /**
     * 获取服务名称。
     *
     * @return 服务名称
     */
    String serviceName();

    /**
     * 获取服务描述。
     *
     * @return 服务描述
     */
    String description();

    /**
     * 获取服务分类。
     *
     * @return 服务分类
     */
    String category();

    /**
     * 获取服务标签列表。
     *
     * @return 标签列表
     */
    List<String> tags();

    /**
     * 获取服务图标标识（UI 展示支持）。
     *
     * @return 图标标识，空字符串表示无图标
     */
    default String icon() {
        return "";
    }

    /**
     * 获取服务使用示例列表（UI 展示支持）。
     *
     * @return 示例列表
     */
    default List<String> examples() {
        return List.of();
    }

    /**
     * 获取服务类型。
     *
     * @return 服务类的 Class 对象
     */
    Class<T> serviceType();

    /**
     * 获取服务操作列表。
     *
     * @return 操作元数据列表
     */
    List<OperationMetadata> operations();
}
