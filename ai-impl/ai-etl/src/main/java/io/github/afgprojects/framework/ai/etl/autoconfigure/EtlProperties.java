package io.github.afgprojects.framework.ai.etl.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ETL 配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     etl:
 *       enabled: true
 *       splitter:
 *         chunk-size: 500
 *         chunk-overlap: 50
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.etl")
public class EtlProperties {

    /**
     * 是否启用 ETL 模块
     */
    private boolean enabled = true;

    /**
     * 文本切片器配置
     */
    private SplitterConfig splitter = new SplitterConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SplitterConfig getSplitter() {
        return splitter;
    }

    public void setSplitter(SplitterConfig splitter) {
        this.splitter = splitter;
    }

    /**
     * 文本切片器配置
     */
    public static class SplitterConfig {

        /**
         * 目标块大小（字符数）
         */
        private int chunkSize = 500;

        /**
         * 块重叠大小（字符数）
         */
        private int chunkOverlap = 50;

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }
    }
}
