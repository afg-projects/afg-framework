package io.github.afgprojects.framework.core.api.importexport;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 数据导出器降级实现。
 * <p>
 * 所有导出操作均为空操作：写入空数据，返回空字节数组。
 * 由 {@code ImportExportAutoConfiguration} 在无其他 {@link DataExporter} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpDataExporter implements DataExporter {

    @Override
    public <T> void export(@NonNull List<T> data, @NonNull Class<T> type,
                           @NonNull OutputStream outputStream) {
        // no-op: 写入空数据
    }

    @Override
    public <T> byte[] export(@NonNull List<T> data, @NonNull Class<T> type) {
        return new byte[0];
    }

    @Override
    public String getFormat() {
        return "noop";
    }
}
