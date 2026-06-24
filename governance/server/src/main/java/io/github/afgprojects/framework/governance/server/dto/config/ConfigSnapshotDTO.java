package io.github.afgprojects.framework.governance.server.dto.config;

import lombok.Data;
import java.time.Instant;

@Data
public class ConfigSnapshotDTO {
    private String id;
    private String name;
    private String description;
    private String tag;
    private String data;
    private String groupId;
    private String groupName;
    private String creatorId;
    private String creatorName;
    private Instant createdAt;
}
