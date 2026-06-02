package io.github.afgprojects.framework.ai.core.etl.transformer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符文本分割器。
 *
 * <p>按分隔符层级递归分割文本，尽量保持语义完整性。
 * 支持中英文分隔符，优先按段落分割，再按句子分割，最后按字符分割。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class RecursiveCharacterTextSplitter implements TextSplitter {

    private static final Logger log = LoggerFactory.getLogger(RecursiveCharacterTextSplitter.class);

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;
    private final boolean keepSeparator;
    private final LengthFunction lengthFunction;

    /**
     * 默认分隔符列表，按优先级排序。
     */
    private static final List<String> DEFAULT_SEPARATORS = List.of(
        "\n\n",    // 段落
        "\n",      // 行
        "。",      // 中文句号
        "．",      // 全角句号
        "！",      // 中文感叹号
        "!",       // 英文感叹号
        "？",      // 中文问号
        "?",       // 英文问号
        "；",      // 中文分号
        ";",       // 英文分号
        "，",      // 中文逗号
        ",",       // 英文逗号
        " ",       // 空格
        ""         // 字符
    );

    /**
     * 文本长度计算函数。
     */
    @FunctionalInterface
    public interface LengthFunction {
        int apply(String text);
    }

    /**
     * 创建默认配置的分割器。
     *
     * @param chunkSize    目标块大小
     * @param chunkOverlap 块重叠大小
     */
    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this(chunkSize, chunkOverlap, DEFAULT_SEPARATORS, false, String::length);
    }

    /**
     * 创建自定义配置的分割器。
     *
     * @param chunkSize       目标块大小
     * @param chunkOverlap    块重叠大小
     * @param separators      分隔符列表（按优先级排序）
     * @param keepSeparator   是否保留分隔符
     * @param lengthFunction  长度计算函数
     */
    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap,
                                           List<String> separators, boolean keepSeparator,
                                           LengthFunction lengthFunction) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap cannot be negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
        }

        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators;
        this.keepSeparator = keepSeparator;
        this.lengthFunction = lengthFunction;
    }

    @Override
    public @NonNull List<String> split(@NonNull String text) {
        if (text.isEmpty()) {
            return List.of();
        }

        return splitText(text, separators);
    }

    private List<String> splitText(String text, List<String> separators) {
        if (lengthFunction.apply(text) <= chunkSize) {
            return List.of(text);
        }

        // 尝试每个分隔符
        for (int i = 0; i < separators.size(); i++) {
            String separator = separators.get(i);
            if (separator.isEmpty() || text.contains(separator)) {
                List<String> splits = splitBySeparator(text, separator);
                List<String> result = new ArrayList<>();
                StringBuilder currentChunk = new StringBuilder();

                for (String split : splits) {
                    int splitLength = lengthFunction.apply(split);
                    int currentLength = currentChunk.length() > 0 ?
                        lengthFunction.apply(currentChunk.toString()) : 0;

                    // 如果单个 split 就超过 chunkSize，需要递归
                    if (splitLength > chunkSize) {
                        // 先添加当前累积的块
                        if (currentChunk.length() > 0) {
                            result.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder();
                        }

                        // 递归使用下一级分隔符
                        if (i + 1 < separators.size()) {
                            List<String> subSplits = splitText(split, separators.subList(i + 1, separators.size()));
                            result.addAll(subSplits);
                        } else {
                            // 最后一级，强制按字符分割
                            result.addAll(forceSplit(split));
                        }
                    } else if (currentLength + splitLength + separator.length() > chunkSize) {
                        // 当前块已满，开始新块
                        if (currentChunk.length() > 0) {
                            result.add(currentChunk.toString().trim());
                        }
                        currentChunk = new StringBuilder(split);
                        if (keepSeparator && !separator.isEmpty()) {
                            currentChunk.insert(0, separator);
                        }
                    } else {
                        // 添加到当前块
                        if (currentChunk.length() > 0 && !separator.isEmpty()) {
                            currentChunk.append(separator);
                        }
                        currentChunk.append(split);
                    }
                }

                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString().trim());
                }

                return mergeChunks(result);
            }
        }

        // 最后手段：强制按字符分割
        return forceSplit(text);
    }

    private List<String> splitBySeparator(String text, String separator) {
        if (separator.isEmpty()) {
            return text.chars().mapToObj(c -> String.valueOf((char) c)).toList();
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        int index;
        while ((index = text.indexOf(separator, start)) != -1) {
            result.add(text.substring(start, index));
            start = index + separator.length();
        }
        if (start < text.length()) {
            result.add(text.substring(start));
        }
        return result;
    }

    private List<String> forceSplit(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            start = end - chunkOverlap;
        }

        return result;
    }

    private List<String> mergeChunks(List<String> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder(chunks.get(0));

        for (int i = 1; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (lengthFunction.apply(current.toString()) + lengthFunction.apply(chunk) <= chunkSize) {
                current.append(" ").append(chunk);
            } else {
                result.add(current.toString().trim());
                current = new StringBuilder(chunk);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkOverlap() {
        return chunkOverlap;
    }
}
