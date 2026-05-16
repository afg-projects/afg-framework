-- OAuth2 客户端表
-- 用于存储 OAuth2 客户端配置信息

CREATE TABLE IF NOT EXISTS auth_client (
    id VARCHAR(64) PRIMARY KEY,
    client_id VARCHAR(128) UNIQUE NOT NULL COMMENT '客户端 ID',
    client_secret VARCHAR(256) COMMENT '客户端密钥（机密客户端）',
    client_name VARCHAR(256) NOT NULL COMMENT '客户端名称',
    redirect_uris TEXT NOT NULL COMMENT '允许的重定向 URI（逗号分隔）',
    scopes TEXT NOT NULL COMMENT '授权范围（逗号分隔）',
    grant_types TEXT NOT NULL COMMENT '支持的授权类型（逗号分隔）',
    auth_methods TEXT NOT NULL COMMENT '客户端认证方法（逗号分隔）',
    require_pkce BOOLEAN DEFAULT FALSE COMMENT '是否需要 PKCE',
    access_token_ttl INTEGER DEFAULT 3600 COMMENT '访问令牌有效期（秒）',
    refresh_token_ttl INTEGER DEFAULT 604800 COMMENT '刷新令牌有效期（秒）',
    status INTEGER DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_client_id ON auth_client (client_id);
CREATE INDEX IF NOT EXISTS idx_client_status ON auth_client (status);

-- 插入默认测试客户端（可选）
INSERT INTO auth_client (id, client_id, client_secret, client_name, redirect_uris, scopes, grant_types, auth_methods, require_pkce)
VALUES (
    'client-001',
    'afg-client',
    'afg-secret',
    'AFG Default Client',
    'http://localhost:8080/login/oauth2/code/afg-client,http://localhost:8080/authorized',
    'openid,read,write',
    'authorization_code,refresh_token,client_credentials',
    'client_secret_basic',
    FALSE
) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 插入公共客户端（需要 PKCE）
INSERT INTO auth_client (id, client_id, client_secret, client_name, redirect_uris, scopes, grant_types, auth_methods, require_pkce)
VALUES (
    'client-002',
    'afg-public-client',
    NULL,
    'AFG Public Client',
    'http://localhost:3000/callback',
    'openid,read',
    'authorization_code,refresh_token',
    'none',
    TRUE
) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;