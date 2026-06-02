package io.github.afgprojects.framework.ai.core.etl.reader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * 编码检测器接口。
 *
 * <p>用于自动检测文件或字节数组的字符编码。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface EncodingDetector {

    /**
     * 检测文件编码。
     *
     * @param path 文件路径
     * @return 检测到的字符编码，默认返回 UTF-8
     */
    @NonNull
    Charset detect(@NonNull Path path);

    /**
     * 检测字节数组编码。
     *
     * @param bytes 字节数组
     * @return 检测到的字符编码，默认返回 UTF-8
     */
    @NonNull
    Charset detect(@NonNull byte[] bytes);

    /**
     * 尝试多种编码读取，返回成功的编码。
     *
     * @param bytes     字节数组
     * @param candidates 候选编码列表
     * @return 成功解码的编码，如果都失败返回 null
     */
    @Nullable
    Charset tryDecode(@NonNull byte[] bytes, @NonNull Charset[] candidates);
}