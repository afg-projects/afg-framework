package io.github.afgprojects.framework.data.core.metadata;

/**
 * APT 元数据加载器
 * <p>
 * 加载编译时 APT 生成的实体元数据类。
 * 生成的类命名规则：{@code 实体包名.metadata.实体名Metadata}
 *
 * <p>优先级：最高（0），优先尝试加载 APT 生成的元数据。
 *
 * <pre>
 * 示例：
 * {@code
 * // 实体类
 * package com.example.entity;
 *
 * @AfEntity
 * public class User { ... }
 *
 * // APT 生成的元数据类
 * package com.example.entity.metadata;
 * public class UserMetadata implements DatabaseEntityMetadata<User> { ... }
 *
 * // 加载
 * AptMetadataLoader loader = new AptMetadataLoader();
 * EntityMetadata<User> metadata = loader.load(User.class);
 * }
 * </pre>
 *
 * @see io.github.afgprojects.framework.apt.entity.AfEntity
 * @see io.github.afgprojects.framework.apt.entity.EntityMetadataProcessor
 */
public class AptMetadataLoader implements EntityMetadataLoader {

    @Override
    @SuppressWarnings("unchecked")
    public <T> EntityMetadata<T> load(Class<T> entityClass) {
        try {
            String packageName = entityClass.getPackageName();
            String simpleName = entityClass.getSimpleName();
            String metadataClassName = packageName + ".metadata." + simpleName + "Metadata";

            Class<?> metadataClass = Class.forName(metadataClassName);
            return (EntityMetadata<T>) metadataClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            // APT 生成的元数据类不存在
            return null;
        } catch (Exception e) {
            // 其他异常（实例化失败等）
            throw new RuntimeException("Failed to load APT generated metadata for: " + entityClass.getName(), e);
        }
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        try {
            String packageName = entityClass.getPackageName();
            String simpleName = entityClass.getSimpleName();
            String metadataClassName = packageName + ".metadata." + simpleName + "Metadata";

            Class.forName(metadataClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return 0; // 最高优先级
    }

    @Override
    public String getName() {
        return "APT";
    }
}
