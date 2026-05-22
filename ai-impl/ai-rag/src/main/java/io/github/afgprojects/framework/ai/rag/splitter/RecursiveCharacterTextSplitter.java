package io.github.afgprojects.framework.ai.rag.splitter;

import io.github.afgprojects.framework.ai.core.rag.TextSplitter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符文本分割器
 *
 * <p>按分隔符层级递归分割文本，尽量保持语义完整性。
 *
 * <p>分割策略：
 * <ol>
 *   <li>按段落分割（\n\n）</li>
 *   <li>按行分割（\n）</li>
 *   <li>按中文句号分割（。）</li>
 *   <li>按中文感叹号分割（！）</li>
 *   <li>按中文问号分割（？）</li>
 *   <li>按英文句号分割（.）</li>
 *   <li>按空格分割（ ）</li>
 *   <li>按字符分割</li>
 * </ol>
 *
 * @author afg-projects
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
     * 默认分隔符层级（优先中文分隔符）
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
     * 创建递归字符分割器
     *
     * @param chunkSize    目标块大小
     * @param chunkOverlap 块重叠大小
     */
    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this(chunkSize, chunkOverlap, DEFAULT_SEPARATORS, false, String::length);
    }

    /**
     * 创建递归字符分割器（完整参数）
     *
     * @param chunkSize      目标块大小
     * @param chunkOverlap   块重叠大小
     * @param separators     分隔符列表
     * @param keepSeparator  是否保留分隔符
     * @param lengthFunction 长度计算函数
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
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        log.debug("Splitting text of length {} with chunkSize {} and overlap {}",
            lengthFunction.apply(text), chunkSize, chunkOverlap);

        return splitText(text, separators);
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkOverlap() {
        return chunkOverlap;
    }

    /**
     * 递归分割文本
     */
    private List<String> splitText(String text, List<String> currentSeparators) {
        // 基础情况：文本足够小
        int length = lengthFunction.apply(text);
        if (length <= chunkSize) {
            return List.of(text);
        }

        // 没有更多分隔符，强制分割
        if (currentSeparators.isEmpty()) {
            return forceSplit(text);
        }

        // 获取当前分隔符
        String separator = currentSeparators.get(0);
        List<String> remainingSeparators = currentSeparators.subList(1, currentSeparators.size());

        // 按分隔符分割
        List<String> splits = splitBySeparator(text, separator);

        // 处理分割结果
        List<String> result = new ArrayList<>();
        List<String> goodSplits = new ArrayList<>();
        int currentLength = 0;

        for (String split : splits) {
            int splitLength = lengthFunction.apply(split);

            if (splitLength > chunkSize) {
                // 分片太大，先合并已有的小分片
                if (!goodSplits.isEmpty()) {
                    result.addAll(mergeSplits(goodSplits, separator));
                    goodSplits.clear();
                    currentLength = 0;
                }
                // 递归分割大分片
                result.addAll(splitText(split, remainingSeparators));
            } else if (currentLength + splitLength + (goodSplits.isEmpty() ? 0 : lengthFunction.apply(separator)) > chunkSize) {
                // 当前块已满，合并并开始新块
                if (!goodSplits.isEmpty()) {
                    result.addAll(mergeSplits(goodSplits, separator));
                    goodSplits.clear();
                }
                goodSplits.add(split);
                currentLength = splitLength;
            } else {
                // 添加到当前块
                goodSplits.add(split);
                currentLength += splitLength + (goodSplits.size() > 1 ? lengthFunction.apply(separator) : 0);
            }
        }

        // 处理剩余的分片
        if (!goodSplits.isEmpty()) {
            result.addAll(mergeSplits(goodSplits, separator));
        }

        return result;
    }

    /**
     * 按分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator) {
        if (separator.isEmpty()) {
            // 按字符分割
            List<String> result = new ArrayList<>();
            for (char c : text.toCharArray()) {
                result.add(String.valueOf(c));
            }
            return result;
        }

        List<String> splits = new ArrayList<>();
        int start = 0;
        int separatorLength = separator.length();

        while (true) {
            int index = text.indexOf(separator, start);
            if (index == -1) {
                // 添加剩余部分
                if (start < text.length()) {
                    splits.add(text.substring(start));
                }
                break;
            }

            // 添加分隔符前的部分
            String part = text.substring(start, keepSeparator ? index + separatorLength : index);
            if (!part.isEmpty()) {
                splits.add(part);
            }

            start = index + separatorLength;
        }

        return splits;
    }

    /**
     * 合并小分片到目标大小
     */
    private List<String> mergeSplits(List<String> splits, String separator) {
        if (splits.isEmpty()) {
            return List.of();
        }

        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder(splits.get(0));

        for (int i = 1; i < splits.size(); i++) {
            String next = splits.get(i);
            String potential = current.toString() + separator + next;

            if (lengthFunction.apply(potential) <= chunkSize) {
                current.append(separator).append(next);
            } else {
                merged.add(current.toString());
                current = new StringBuilder(next);
            }
        }

        if (current.length() > 0) {
            merged.add(current.toString());
        }

        // 添加重叠
        return addOverlap(merged);
    }

    /**
     * 添加块重叠
     */
    private List<String> addOverlap(List<String> chunks) {
        if (chunkOverlap == 0 || chunks.size() <= 1) {
            return chunks;
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            if (i > 0) {
                // 从前一个块末尾取重叠部分
                String prevChunk = chunks.get(i - 1);
                String overlap = getOverlap(prevChunk);
                if (!overlap.isEmpty()) {
                    chunk = overlap + chunk;
                }
            }

            result.add(chunk);
        }

        return result;
    }

    /**
     * 从文本末尾获取重叠部分
     */
    private String getOverlap(String text) {
        int overlapChars = chunkOverlap;
        if (text.length() <= overlapChars) {
            return text;
        }
        return text.substring(text.length() - overlapChars);
    }

    /**
     * 强制分割（当没有分隔符时）
     */
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

    /**
     * 长度计算函数
     */
    @FunctionalInterface
    public interface LengthFunction {
        int apply(String text);
    }

    /**
     * 创建基于 Token 计数的分割器
     */
    public static RecursiveCharacterTextSplitter forTokenCount(int chunkTokens, int overlapTokens,
                                                                LengthFunction tokenCounter) {
        return new RecursiveCharacterTextSplitter(chunkTokens, overlapTokens,
            DEFAULT_SEPARATORS, false, tokenCounter);
    }
}