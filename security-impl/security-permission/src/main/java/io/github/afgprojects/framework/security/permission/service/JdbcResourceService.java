package io.github.afgprojects.framework.security.permission.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.permission.entity.SecPermission;
import io.github.afgprojects.framework.security.permission.entity.SecResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资源管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcResourceService {

    private final DataManager dataManager;

    public SecResource create(@NonNull SecResource resource) {
        SecResource saved = dataManager.save(SecResource.class, resource);
        log.info("Created resource: {}", resource.getResourceCode());
        return saved;
    }

    public SecResource update(@NonNull SecResource resource) {
        return dataManager.save(SecResource.class, resource);
    }

    public Optional<SecResource> findById(@NonNull Long id) {
        return dataManager.findById(SecResource.class, id);
    }

    public Optional<SecResource> findByCode(@NonNull String resourceCode, @Nullable String tenantId) {
        var condition = Conditions.builder(SecResource.class)
            .eq(SecResource::getResourceCode, resourceCode);
        if (tenantId != null) {
            condition.eq(SecResource::getTenantId, tenantId);
        }
        return dataManager.findOne(SecResource.class, condition.build());
    }

    public List<SecResource> findAll(@Nullable String tenantId) {
        if (tenantId != null) {
            return dataManager.findList(SecResource.class,
                Conditions.builder(SecResource.class)
                    .eq(SecResource::getTenantId, tenantId)
                    .build());
        }
        return dataManager.findAll(SecResource.class);
    }

    public List<SecResource> getResourceTree(@Nullable String tenantId) {
        return findAll(tenantId);
    }

    public List<SecResource> findByType(@NonNull String resourceType, @Nullable String tenantId) {
        var condition = Conditions.builder(SecResource.class)
            .eq(SecResource::getResourceType, resourceType);
        if (tenantId != null) {
            condition.eq(SecResource::getTenantId, tenantId);
        }
        return dataManager.findList(SecResource.class, condition.build());
    }

    public SecPermission createPermission(@NonNull SecPermission permission) {
        SecPermission saved = dataManager.save(SecPermission.class, permission);
        log.info("Created permission: {}", permission.getPermissionCode());
        return saved;
    }

    public Optional<SecPermission> findPermissionByCode(@NonNull String permissionCode, @Nullable String tenantId) {
        var condition = Conditions.builder(SecPermission.class)
            .eq(SecPermission::getPermissionCode, permissionCode);
        if (tenantId != null) {
            condition.eq(SecPermission::getTenantId, tenantId);
        }
        return dataManager.findOne(SecPermission.class, condition.build());
    }

    public List<SecPermission> findAllPermissions(@Nullable String tenantId) {
        if (tenantId != null) {
            return dataManager.findList(SecPermission.class,
                Conditions.builder(SecPermission.class)
                    .eq(SecPermission::getTenantId, tenantId)
                    .build());
        }
        return dataManager.findAll(SecPermission.class);
    }

    public List<SecPermission> findPermissionsByResource(@NonNull Long resourceId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecPermission.class)
            .eq(SecPermission::getResourceId, resourceId);
        if (tenantId != null) {
            condition.eq(SecPermission::getTenantId, tenantId);
        }
        return dataManager.findList(SecPermission.class, condition.build());
    }

    @Transactional
    public void delete(@NonNull Long id) {
        dataManager.findList(SecPermission.class,
            Conditions.builder(SecPermission.class)
                .eq(SecPermission::getResourceId, id)
                .build())
            .forEach(p -> dataManager.deleteById(SecPermission.class, p.getId()));

        dataManager.deleteById(SecResource.class, id);
        log.info("Deleted resource: {}", id);
    }
}
