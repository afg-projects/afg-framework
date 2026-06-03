package io.github.afgprojects.framework.data.core.util;

/**
 * JVM 类型描述符解析工具
 * <p>
 * 将 JVM 字段/方法描述符（如 "Ljava/lang/String;"、"I"）解析为对应的 {@link Class} 对象。
 * 用于 Lambda 表达式序列化时的类型推断。
 */
public final class TypeDescriptorUtils {

    private TypeDescriptorUtils() {
        // 工具类，不允许实例化
    }

    /**
     * 从 JVM 类型描述符解析为 Class 对象
     *
     * @param descriptor JVM 类型描述符（如 "Ljava/lang/String;"、"I"）
     * @return 对应的 Class 对象，可能为 null
     */
    public static Class<?> resolveTypeFromDescriptor(String descriptor) {
        return switch (descriptor.charAt(0)) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            case 'V' -> void.class;
            case 'L' -> {
                String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                try {
                    yield Class.forName(className);
                } catch (ClassNotFoundException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}