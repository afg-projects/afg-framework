package io.github.afgprojects.framework.data.jdbc.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.afgprojects.framework.data.core.DataManager;

/**
 * 数据层集成测试基类
 *
 * <p>继承 BasePostgresTest，使用真实的 PostgreSQL 容器。
 * 测试后自动回滚（@Transactional），保持数据库干净。
 * 适用于 DataManager CRUD、条件查询、分页等数据操作测试。
 */
@Transactional
@ActiveProfiles("test")
public abstract class BaseDataTest extends BasePostgresTest {

    protected DataManager dataManager;

    @org.springframework.beans.factory.annotation.Autowired
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }
}