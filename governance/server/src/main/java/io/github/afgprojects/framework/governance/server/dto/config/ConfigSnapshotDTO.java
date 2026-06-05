package io.github.afgprojects.framework.governance.server.dto.config;

import lombok.Data;
import java.time.Instant;

@Data
public class ConfigSnapshotDTO {
    private Long id;
    private String name;
    private String description;
    private String tag;
    private String data;
    private Long groupId;
    private String groupName;
    private Long creatorId;
    private String creatorName;
    private Instant createdAt;
}
