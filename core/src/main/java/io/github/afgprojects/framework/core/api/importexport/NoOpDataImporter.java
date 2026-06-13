package io.github.afgprojects.framework.core.api.importexport;

import java.io.InputStream;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 数据导入器降级实现。
 * <p>
 * 所有导入操作均返回空结果。
 * 由 {@code ImportExportAutoConfiguration} 在无其他 {@link DataImporter} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpDataImporter implements DataImporter {

    @Override
    public <T> ImportResult<T> importAs(@NonNull InputStream inputStream, @NonNull Class<T> type) {
        return ImportResult.empty();
    }

    @Override
    public String getFormat() {
        return "noop";
    }
}
