package io.github.afgprojects.framework.data.core.util;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import org.jspecify.annotations.Nullable;

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
    public static @Nullable Class<?> resolveTypeFromDescriptor(String descriptor) {
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

    /**
     * 从 Lambda 方法引用中解析字段的返回类型
     * <p>
     * 通过 SerializedLambda 获取方法签名，解析返回类型的 JVM 描述符。
     * 如果无法解析则返回 null（类型检查将被跳过）。
     *
     * @param getter Lambda 方法引用
     * @return 字段返回类型，可能为 null
     */
    public static @Nullable Class<?> resolveFieldTypeFromLambda(SFunction<?, ?> getter) {
        try {
            java.lang.reflect.Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            java.lang.invoke.SerializedLambda lambda = (java.lang.invoke.SerializedLambda) writeReplace.invoke(getter);
            String methodSignature = lambda.getImplMethodSignature();
            // 方法签名格式：(参数类型)返回类型
            String returnTypeDesc = methodSignature.substring(methodSignature.indexOf(')') + 1);
            return resolveTypeFromDescriptor(returnTypeDesc);
        } catch (Exception e) {
            // 降级：无法获取类型信息时不检查
            return null;
        }
    }
}