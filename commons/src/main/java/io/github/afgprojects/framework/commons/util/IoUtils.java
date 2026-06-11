package io.github.afgprojects.framework.commons.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * IO 工具类。
 * <p>提供常用 IO 读取、复制、关闭操作。
 *
 * <p>使用示例：
 * <pre>{@code
 * String content = IoUtils.readAsString(inputStream)
 * IoUtils.copy(inputStream, outputStream)
 * IoUtils.closeQuietly(inputStream)
 * }</pre>
 */
public final class IoUtils {

    private IoUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将 InputStream 读取为字符串（UTF-8）
     *
     * @param inputStream 输入流
     * @return 字符串内容
     * @throws IOException 如果读取失败
     */
    public static String readAsString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            // 移除末尾多余换行
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\n') {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }

    /**
     * 将 InputStream 复制到 OutputStream
     *
     * @param input  输入流
     * @param output 输出流
     * @return 复制的字节数
     * @throws IOException 如果复制失败
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        if (input == null || output == null) {
            return 0;
        }
        byte[] buffer = new byte[8192];
        long count = 0;
        int n;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * 安静关闭 AutoCloseable，不抛出异常
     *
     * @param closeable 可关闭对象
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 安静关闭，忽略异常
            }
        }
    }
}