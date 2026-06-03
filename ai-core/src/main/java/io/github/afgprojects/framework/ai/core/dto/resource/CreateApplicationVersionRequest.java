package io.github.afgprojects.framework.ai.core.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建应用版本请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateApplicationVersionRequest {

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String config;

    private String description;

    private LocalDateTime publishedAt;

    private String userId;
}
