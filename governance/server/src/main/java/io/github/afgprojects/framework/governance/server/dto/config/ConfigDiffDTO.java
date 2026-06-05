package io.github.afgprojects.framework.governance.server.dto.config;

import lombok.Data;
import java.util.List;

@Data
public class ConfigDiffDTO {
    private Long snapshot1Id;
    private Long snapshot2Id;
    private String snapshot1Name;
    private String snapshot2Name;
    private List<ConfigDiffItem> added;
    private List<ConfigDiffItem> modified;
    private List<ConfigDiffItem> deleted;

    @Data
    public static class ConfigDiffItem {
        private String key;
        private String oldValue;
        private String newValue;
        private String itemName;
    }
}
