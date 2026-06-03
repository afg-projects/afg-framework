package io.github.afgprojects.framework.core.properties.ratelimit;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 限流白名单配置。
 */
@Data
public class AfgCoreRateLimitWhitelistProperties {

    private boolean enabled = true;
    private List<String> ips = new ArrayList<>();
    private List<Long> userIds = new ArrayList<>();
    private List<String> usernames = new ArrayList<>();
    private List<Long> tenantIds = new ArrayList<>();
    private @Nullable String customCheckerBean;
}
