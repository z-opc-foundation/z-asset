# 项目总览

## 1. 项目简介

z-asset 是 z-opc-foundation 的**资产台账服务**：把散在各业务模块里的「资产」（数据库、模型服务、缓存、消息队列、OSS 桶……）用一套统一的采集 SPI 收进一张 `z_asset` 表，对外只暴露三个查询/触发端点。

它的形状是「**SPI + 采集器 + REST 面**」三段式：各业务模块实现 `AssetProvider` 接口声明自己能提供什么资产；`z-asset-core` 的 `AssetCollector` 定时或被触发时遍历所有已注册的 provider，把资产清单落库；`z-asset-web` 暴露 REST 端点供管理台查询。

**技术栈：** Java 8 · Spring Boot（经 `z-boot-web-starter` / `z-boot-datasource-starter` 引入）· MyBatis-Plus（`BaseMapper` + `LambdaQueryWrapper`）· Druid + MySQL 驱动 · JUnit 5 + Mockito

## 2. 模块架构

```
z-asset/
├── pom.xml                    # 聚合根：parent z-boot-parent:1.1.0，<version>1.0.1</version>，3 个 <module>，central profile
├── LICENSE                    # MIT
├── README.md
├── z-asset-api/               # SPI 定义：AssetProvider 接口 + 内嵌 AssetDTO
│   └── src/main/java/com/zifang/z/asset/api/AssetProvider.java
├── z-asset-core/              # 领域层：采集器 + provider 注册 + 实体 + mapper
│   └── src/main/java/com/zifang/z/asset/core/
│       ├── collector/         # AssetCollector / AssetProviderRegistry / AssetProviderRegistrationBean
│       └── domain/            # entity/AssetEntity · mapper/AssetMapper
├── z-asset-web/               # Web 层：REST 端点 + 自动装配
│   ├── src/main/java/com/zifang/z/asset/web/
│   │   ├── api/AssetController.java
│   │   └── config/AssetAutoConfiguration.java
│   └── src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── _frontend/
    └── z-asset-suit/          # React 19 + antd 6 + Vite 6 管理台壳（"private": true，不发布 npm）
```

## 3. 核心功能

### 3.1 资产提供者 SPI（`z-asset-api`）

`AssetProvider` 接口声明 5 个方法：`getModule()`（所属模块名）、`getAssetType()`（资产类型）、`listAssets()`（资产清单）、`getAssetDetail(String assetCode)`（单个资产详情）、`isHealthy()`（提供方健康状态）。

接口内嵌 `AssetDTO` 数据传输对象，10 个字段：`assetCode` / `assetName` / `assetType` / `module` / `status` / `endpoint` / `spec` / `attributes`（`Map<String, String>`）/ `tenantCode` / `domainCode`。

### 3.2 Provider 自动注册（`z-asset-core`）

`AssetProviderRegistrationBean` 实现 Spring 的 `BeanPostProcessor`，在每个 bean 初始化完成后判断它是不是 `AssetProvider` 实例，是就塞进 `AssetProviderRegistry`。

`AssetProviderRegistry` 内部用 `CopyOnWriteArrayList` 存 provider，提供 `register` / `getAllProviders` / `getProvidersByModule(String)` / `getProvider(String module, String assetType)` 四个方法 —— 即**业务模块只要把 provider 声明成 bean，资产就会自动进台账，无需在本仓登记**。

### 3.3 资产采集与落库（`z-asset-core`）

`AssetCollector.collectAll()` 遍历注册表里的全部 provider 逐个调 `collectFromProvider(provider)`；后者带 `@Transactional`，流程是：

1. `provider.isHealthy()` 为 false 直接返回，不落库；
2. `provider.listAssets()` 返回 null 直接返回，不落库；
3. 逐条 DTO 按 `assetCode` 查库：查不到则 `insert`，查得到则 `updateById`（更新 `assetName` / `status` / `endpoint` / `spec` / `collectedAt` / `updatedTime`；`attributes` 在 update 分支当前不刷新）；
4. `attributes` 走 `JsonUtil.toJson(...)` 序列化成 **JSON** 落 `z_asset.attributes` 列（该列的注释口径写死是「属性信息（JSON格式）」，实现早期写成 Java 的 `{k=v}` 伪格式，已改为真 JSON）。

### 3.4 REST 面（`z-asset-web`）

`AssetController` 类级前缀 `/api/asset`，三个端点：

| 方法 | 路径 | 作用 |
|---|---|---|
| POST | `/api/asset/collect` | 触发一次全量采集（遍历所有 provider） |
| GET | `/api/asset/list` | 查资产列表，可按 `module` / `assetType` 过滤，按 `updatedTime` 倒序 |
| GET | `/api/asset/providers` | 列当前已注册的全部 provider |

统一返回 `com.zifang.util.core.meta.Result` 包装体。

### 3.5 数据源与 MyBatis 装配（`z-asset-web`）

`AssetAutoConfiguration` 继承 `z-boot-datasource-starter` 的 `ModuleDataSourceTemplate`：

- `@ComponentScan("com.zifang.z.asset")` 扫全本仓包；
- `@MapperScan(basePackages = "com.zifang.z.asset.core.domain.mapper", sqlSessionFactoryRef = "sqlSessionFactoryAsset")` 把 mapper 绑到本模块自己的 `SqlSessionFactory`；
- `dataSourceAsset` bean 通过 `buildDataSource(env, "asset")` 构建 Druid 数据源，配置键为 `z.base.db.asset.*`（`host` / `port` / `username` / `password` / `database` / `initial-size` / `min-idle` / `max-active` / `max-wait`），回退到 `z.base.db.default.*`，再回退到内置缺省（localhost / 3306 / root / 空密码）；
- `sqlSessionFactoryAsset` bean 用 `MybatisSqlSessionFactoryBean`，mapper XML 位置 `classpath*:mapper/**/*.xml`。

该类通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，消费方引 `z-asset-web` 即自动装配。

### 3.6 管理台壳（`_frontend/z-asset-suit`）

React 19 + antd 6 + Vite 6 的独立壳，用 `@yuku123/z-frontend-common` 的 `AppLayout` 与登录页；菜单与路由表在 `src/pages-manifest.jsx` 内联（`/z-asset/home` 首页、`/z-asset/status` 服务状态、`/z-asset/login` 登录）；登录态存在 `localStorage.token` / `localStorage.userInfo`，未登录跳 `/z-asset/login`。

## 4. 对外集成 / 上下游

**本仓不自带 HTTP 端口，也不自带 `application*.yml`。** 仓内没有任何 `application*.yml` / `application*.properties`，`server.port` 与 context-path 由宿主应用或部署侧下发。

**上游依赖（取自各模块 `pom.xml`）：**

| 依赖 | 出现在 | 作用 |
|---|---|---|
| `z-util-core` | `z-asset-api` | `Result` 等基础类型（`z-asset-web` 经传递依赖用到 `com.zifang.util.core.meta.Result`） |
| `z-util-parser-json` | `z-asset-core` | `JsonUtil`，把 `attributes` 序列化成 JSON（显式声明，不吃传递依赖） |
| `z-boot-datasource-starter` | `z-asset-core` | `ModuleDataSourceTemplate` + Druid/MySQL 数据源 + MyBatis-Plus 底座 |
| `z-boot-web-starter` | `z-asset-web` | Web 层底座（Spring MVC、返回体等） |
| `org.springframework:spring-context` | `z-asset-core`（`provided` 作用域） | `@Component` / `BeanPostProcessor` 注解 |
| `junit-jupiter` / `junit-vintage-engine` / `mockito-core` / `mockito-junit-jupiter` | 根 POM（test scope） | 全仓统一测试台，`AssetCollectorTest` 用 Mockito 打桩 `AssetMapper` |

**下游消费方：**

- **各业务模块**：实现 `AssetProvider` 并声明为 bean 即被自动采集，无需在本仓改一行代码。
- **宿主应用**：引 `io.github.yuku123:z-asset-web` 即自动装配数据源、mapper 与 REST 端点；三个端点本身无鉴权，认证与租户隔离由宿主/网关负责。
- **管理台**：`_frontend/z-asset-suit` 的 `/api` 代理到 `http://localhost:8888`（本地开发时的后端地址）。

**发布边界：** 根 POM 的 `central` profile 用 `central-publishing-maven-plugin` 0.7.0（`tokenAuth` + `autoPublish` + `waitUntil=uploaded`）发布；本仓 3 个坐标全是库件，无可执行 starter，故 `excludeArtifacts` 为空。

## 5. 技术特点

1. **provider 靠 `BeanPostProcessor` 自动进册**：`AssetProviderRegistrationBean` 在 bean 初始化后扫描类型，业务方加一个 `@Component` 就能把资产接进台账，扩展点不需要改本仓代码。
2. **注册表用 `CopyOnWriteArrayList`**：provider 列表在运行期会随 bean 初始化而增长，读多写少，`CopyOnWriteArrayList` 让遍历方无需加锁。
3. **provider 自带健康开关**：`isHealthy()` 为 false 时 `AssetCollector` 直接跳过该 provider 且不落库 —— 单个子系统挂掉不会污染台账，也不会让整轮采集失败。
4. **`attributes` 落真 JSON**：`JsonUtil.toJson` 序列化，值里含 `,` `=` `"` 时仍是无歧义 JSON（`AssetCollectorTest` 逐条锁死这一列语义）。
5. **模块级独立数据源**：本模块用自己的 `dataSourceAsset` + `sqlSessionFactoryAsset`，配置前缀 `z.base.db.asset.*` 可与其它模块的库共存。
6. **仓内没有 `z_asset` 的建表 SQL**：`AssetEntity` 用 `@TableName("z_asset")` 声明表名与 15 个字段（`id` 自增 + `asset_code` / `asset_name` / `asset_type` / `module` / `status` / `endpoint` / `spec` / `attributes` / `tenant_code` / `domain_code` / `collected_at` / `expire_at` / `created_time` / `updated_time`），但 DDL 不在本仓 —— 建表脚本由部署侧提供。
7. **纯注解式 mapper**：仓内没有 `mapper/**/*.xml`，`AssetMapper` 只继承 `BaseMapper<AssetEntity>`，查库用 `LambdaQueryWrapper`。
8. **零环境变量读取**：本仓不定义任何环境变量名或配置键，数据库连接全部来自 `z.base.db.*` 配置项。

## 6. 目录结构

源码按 Java 包树组织，根包 `com.zifang.z.asset`：

```
z-asset/
├── z-asset-api/src/main/java/com/zifang/z/asset/api/
│   └── AssetProvider.java        # SPI 接口 + 内嵌 AssetDTO（10 字段）
├── z-asset-core/src/
│   ├── main/java/com/zifang/z/asset/core/
│   │   ├── collector/
│   │   │   ├── AssetCollector.java            # 采集编排（@Transactional）
│   │   │   ├── AssetProviderRegistry.java     # provider 注册表（CopyOnWriteArrayList）
│   │   │   └── AssetProviderRegistrationBean.java  # BeanPostProcessor 自动注册
│   │   └── domain/
│   │       ├── entity/AssetEntity.java        # @TableName("z_asset")，15 字段
│   │       └── mapper/AssetMapper.java        # BaseMapper<AssetEntity>
│   └── test/java/com/zifang/z/asset/core/collector/AssetCollectorTest.java   # 7 支 @Test
├── z-asset-web/src/
│   ├── main/java/com/zifang/z/asset/web/
│   │   ├── api/AssetController.java            # /api/asset/{collect,list,providers}
│   │   └── config/AssetAutoConfiguration.java  # 数据源 + SqlSessionFactory + @MapperScan
│   └── main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── _frontend/z-asset-suit/                    # React 管理台壳（不发布 npm）
    ├── index.html  vite.config.js  package.json
    ├── public/icon.png
    └── src/                    # App.jsx / HomePage.jsx / LoginPage.jsx / StatusPage.jsx
                                #   / Status.jsx / main.jsx / pages-manifest.jsx
```

规模实测：main 8 个 `.java`（api 1 / core 5 / web 2，其中 `AssetEntity` 235 行、`AssetProvider` 213 行、`AssetController` 88 行、`AssetAutoConfiguration` 38 行），test 1 个 `.java`（7 支 `@Test`）。
