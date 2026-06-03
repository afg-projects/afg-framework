package io.github.afgprojects.framework.core.properties.security;

import java.util.Set;

import lombok.Data;

/**
 * XSS 防护配置。
 */
@Data
public class AfgCoreXssProperties {

    private boolean enabled = true;
    private boolean richTextMode;
    private Set<String> allowedTags = Set.of(
            "p", "br", "b", "i", "u", "strong", "em", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "a", "img", "span", "div");
    private Set<String> allowedAttributes = Set.of("href", "src", "alt", "title", "class", "id", "target");
}
