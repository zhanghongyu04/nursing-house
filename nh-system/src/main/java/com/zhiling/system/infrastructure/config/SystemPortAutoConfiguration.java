package com.zhiling.system.infrastructure.config;

import com.zhiling.system.config.RustFsConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * System Port 自动配置。
 *
 * 作为 system 模块对外能力的装配入口，显式导入各子域配置，
 * 而非通过大范围包扫描整体导出。
 *
 * <h3>子域清单</h3>
 * <ul>
 *   <li>{@link AdminConfiguration} - 管理员域服务与用户管理</li>
 *   <li>{@link AuthConfiguration} - 认证鉴权域服务</li>
 *   <li>{@link DictConfiguration} - 字典域服务</li>
 *   <li>{@link PanelConfiguration} - 大屏统计域服务</li>
 *   <li>{@link SanaImageConfiguration} - 机构图片域服务</li>
 *   <li>{@link ApplicationServiceConfiguration} - 应用服务层（业务编排入口）</li>
 *   <li>{@link PortAdapterConfiguration} - Port 适配器与范围解析器</li>
 * </ul>
 *
 * persistence 层由 {@link InfrastructurePersistenceConfiguration} 独立管理。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@Import({
    RustFsConfig.class,
    InfrastructurePersistenceConfiguration.class,
    SystemPortAutoConfiguration.AdminConfiguration.class,
    SystemPortAutoConfiguration.AuthConfiguration.class,
    SystemPortAutoConfiguration.DictConfiguration.class,
    SystemPortAutoConfiguration.PanelConfiguration.class,
    SystemPortAutoConfiguration.SanaImageConfiguration.class,
    SystemPortAutoConfiguration.ApplicationServiceConfiguration.class,
    SystemPortAutoConfiguration.PortAdapterConfiguration.class,
    SystemPortAutoConfiguration.HttpControllerConfiguration.class,
    SystemPortAutoConfiguration.FrameworkAdapterConfiguration.class,
    SystemPortAutoConfiguration.StorageConfiguration.class,
    SystemPortAutoConfiguration.EzvizConfiguration.class
})
public class SystemPortAutoConfiguration {

    /** 管理员域：用户 CRUD、角色授权、机构范围管理 */
    @Configuration
    @ComponentScan("com.zhiling.system.admin")
    static class AdminConfiguration {
    }

    /** 认证鉴权域：登录态、权限资源查询 */
    @Configuration
    @ComponentScan("com.zhiling.system.auth")
    static class AuthConfiguration {
    }

    /** 字典域：数据字典查询 */
    @Configuration
    @ComponentScan("com.zhiling.system.dict")
    static class DictConfiguration {
    }

    /** 大屏统计域：导航统计、区域分布 */
    @Configuration
    @ComponentScan("com.zhiling.system.panel")
    static class PanelConfiguration {
    }

    /** 机构图片域：图片 CRUD 与文件代理 */
    @Configuration
    @ComponentScan("com.zhiling.system.sanaimage")
    static class SanaImageConfiguration {
    }

    /** 应用服务层：业务编排入口，被控制器与 port adapter 共同依赖 */
    @Configuration
    @ComponentScan("com.zhiling.system.application.service")
    static class ApplicationServiceConfiguration {
    }

    /** Port 适配器层：对外契约实现与访问范围解析策略 */
    @Configuration
    @ComponentScan("com.zhiling.system.interfaces.system")
    static class PortAdapterConfiguration {
    }

    /** HTTP 接口层：REST 控制器 */
    @Configuration
    @ComponentScan("com.zhiling.system.interfaces.http")
    static class HttpControllerConfiguration {
    }

    /** Framework 适配器层：实现 framework 定义的 Port 接口 */
    @Configuration
    @ComponentScan("com.zhiling.system.interfaces.framework")
    static class FrameworkAdapterConfiguration {
    }

    /** 基础设施存储层：对象存储适配器 */
    @Configuration
    @ComponentScan("com.zhiling.system.infrastructure.storage")
    static class StorageConfiguration {
    }

    /** 萤石接入层：配置、客户端、Token 服务 */
    @Configuration
    @ComponentScan("com.zhiling.system.infrastructure.ezviz")
    static class EzvizConfiguration {
    }

    /** 定时任务层：摄像头状态同步等 */
    @Configuration
    @ComponentScan("com.zhiling.system.infrastructure.schedule")
    static class ScheduleConfiguration {
    }
}
