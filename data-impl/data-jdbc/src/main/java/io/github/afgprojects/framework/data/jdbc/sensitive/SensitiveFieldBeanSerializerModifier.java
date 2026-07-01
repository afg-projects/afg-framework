package io.github.afgprojects.framework.data.jdbc.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.sensitive.MaskingContext;
import io.github.afgprojects.framework.data.core.sensitive.MaskingStrategy;
import io.github.afgprojects.framework.data.core.sensitive.SensitiveFieldMetadata;
import io.github.afgprojects.framework.data.core.sensitive.DefaultMaskingStrategy;
import io.github.afgprojects.framework.data.core.sensitive.NoOpMaskingContext;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jackson 序列化器修饰器 — 在序列化时动态应用数据脱敏。
 * <p>
 * 当实体类具有 {@code @SensitiveField} 注解的字段时，在 Jackson 序列化输出时
 * 自动检查 {@link MaskingContext} 并应用 {@link MaskingStrategy} 对字段值进行脱敏。
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>检测实体类类型（通过 BeanDescription.getClassInfo()）</li>
 *   <li>通过 EntityMetadataCache 获取实体元数据，获取敏感字段列表</li>
 *   <li>对每个敏感字段的 BeanPropertyWriter，包装为脱敏写入器</li>
 *   <li>脱敏写入器在序列化时调用 MaskingContext.shouldMask() 判断是否脱敏</li>
 *   <li>如需脱敏，调用 MaskingStrategy.mask() 获取脱敏后的值</li>
 * </ol>
 *
 * <h3>缓存</h3>
 * <p>
 * 敏感字段元数据按实体类缓存，避免每次序列化时重复查询 EntityMetadataCache。
 *
 * @see SensitiveFieldMetadata
 * @see MaskingStrategy
 * @see MaskingContext
 */
@SuppressWarnings("PMD.NonSerializableClass")
public class SensitiveFieldBeanSerializerModifier extends BeanSerializerModifier {

    private final EntityMetadataCache metadataCache;
    private final MaskingStrategy maskingStrategy;
    private final MaskingContext maskingContext;

    /**
     * 敏感字段元数据缓存：实体类 -> 敏感字段名集合
     */
    private final ConcurrentHashMap<Class<?>, Set<String>> sensitiveFieldsCache = new ConcurrentHashMap<>();

    /**
     * 创建脱敏序列化器修饰器。
     *
     * @param metadataCache   实体元数据缓存（用于获取敏感字段列表）
     * @param maskingStrategy 脱敏策略（默认 DefaultMaskingStrategy）
     * @param maskingContext  脱敏上下文（默认 NoOpMaskingContext，始终脱敏）
     */
    public SensitiveFieldBeanSerializerModifier(EntityMetadataCache metadataCache,
                                                 @Nullable MaskingStrategy maskingStrategy,
                                                 @Nullable MaskingContext maskingContext) {
        this.metadataCache = metadataCache;
        this.maskingStrategy = maskingStrategy != null ? maskingStrategy : new DefaultMaskingStrategy();
        this.maskingContext = maskingContext != null ? maskingContext : new NoOpMaskingContext();
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                      BeanDescription beanDesc,
                                                      List<BeanPropertyWriter> beanProperties) {
        Class<?> beanClass = beanDesc.getBeanClass();
        Set<String> sensitiveFields = getSensitiveFields(beanClass);

        if (sensitiveFields.isEmpty()) {
            return beanProperties;
        }

        return beanProperties.stream()
            .map(writer -> wrapIfSensitive(writer, sensitiveFields, beanClass))
            .toList();
    }

    /**
     * 如果字段是敏感字段，包装为脱敏写入器。
     */
    private BeanPropertyWriter wrapIfSensitive(BeanPropertyWriter writer,
                                                Set<String> sensitiveFields,
                                                Class<?> beanClass) {
        if (sensitiveFields.contains(writer.getName())) {
            return new MaskingPropertyWriter(writer, beanClass);
        }
        return writer;
    }

    /**
     * 获取实体类的敏感字段名集合（带缓存）。
     */
    private Set<String> getSensitiveFields(Class<?> beanClass) {
        return sensitiveFieldsCache.computeIfAbsent(beanClass, clazz -> {
            try {
                @SuppressWarnings("unchecked")
                EntityMetadata<?> metadata = metadataCache.get(clazz);
                if (metadata == null) {
                    return Set.of();
                }
                List<SensitiveFieldMetadata> sensitiveFieldList = metadata.getSensitiveFields();
                if (sensitiveFieldList.isEmpty()) {
                    return Set.of();
                }
                Set<String> fields = new HashSet<>();
                for (SensitiveFieldMetadata sfm : sensitiveFieldList) {
                    fields.add(sfm.fieldName());
                }
                return fields;
            } catch (Exception e) {
                return Set.of();
            }
        });
    }

    /**
     * 脱敏属性写入器 — 在序列化时拦截字段值，检查上下文并应用脱敏。
     */
    private class MaskingPropertyWriter extends BeanPropertyWriter {

        private final BeanPropertyWriter delegate;
        private final Class<?> beanClass;

        MaskingPropertyWriter(BeanPropertyWriter delegate, Class<?> beanClass) {
            super(delegate);
            this.delegate = delegate;
            this.beanClass = beanClass;
        }

        @Override
        @SuppressWarnings("PMD.PreserveStackTrace")
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws IOException {
            try {
                Object value = get(bean);
                if (value == null) {
                    delegate.serializeAsField(bean, gen, prov);
                    return;
                }

                String fieldName = delegate.getName();
                String sensitiveType = resolveSensitiveType(beanClass, fieldName);

                if (maskingContext.shouldMask(fieldName, sensitiveType)) {
                    String maskedValue = maskingStrategy.mask(value.toString(), sensitiveType, fieldName);
                    gen.writeFieldName(delegate.getName());
                    gen.writeString(maskedValue);
                } else {
                    delegate.serializeAsField(bean, gen, prov);
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                // Fallback: let the original writer handle it
                try {
                    delegate.serializeAsField(bean, gen, prov);
                } catch (IOException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new IOException("Failed to serialize field: " + delegate.getName(), ex);
                }
            }
        }

        @Override
        public Object get(Object bean) throws Exception {
            return delegate.get(bean);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        private String resolveSensitiveType(Class<?> beanClass, String fieldName) {
            try {
                @SuppressWarnings("unchecked")
                EntityMetadata<?> metadata = metadataCache.get(beanClass);
                if (metadata != null) {
                    SensitiveFieldMetadata sfm = metadata.getSensitiveField(fieldName);
                    if (sfm != null) {
                        return sfm.sensitiveType().name();
                    }
                }
            } catch (Exception ignored) {
                // fall through
            }
            return "CUSTOM";
        }
    }
}
