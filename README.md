# z-asset

> 资产台账服务 —— 统一采集各业务模块的资产（数据库 / 模型服务 / 缓存 / 队列 / 存储桶……）进一张 `z_asset` 表，对外只给三个 REST 端点

各业务模块各自有一堆「资产」（自己用的库、模型服务、缓存、消息队列、OSS 桶），但没有一处能看全。本仓提供一套统一形状：业务模块实现 `AssetProvider` 接口声明自己能提供什么，本仓负责自动登记、周期采集、落库、对外查询。

它由三段组成：**SPI（`z-asset-api`）+ 采集器与注册中心（`z-asset-core`）+ REST 面与自动装配（`z-asset-web`）**。

**技术栈：** Java 8 · Spring Boot（经 `z-boot-web-starter` / `z-boot-datasource-starter` 引入）· MyBatis-Plus（`BaseMapper` + `LambdaQueryWrapper`）· Druid + MySQL 驱动 · JUnit 5 + Mockito

---

## 📦 模块构成

| 模块 | 坐标 | 职责 |
|------|------|------|
| `z-asset-api` | `io.github.yuku123:z-asset-api` | **SPI 定义**：`AssetProvider` 接口（5 个方法）+ 内嵌 `AssetDTO`（10 字段）。业务模块只依赖这一层就能接入。 |
| `z-asset-core` | `io.github.yuku123:z-asset-core` | **领域层**：`AssetProviderRegistry`（注册表）、`AssetProviderRegistrationBean`（`BeanPostProcessor` 自动登记）、`AssetCollector`（采集与落库）、`AssetEntity`（`@TableName("z_asset")`）+ `AssetMapper`。 |
| `z-asset-web` | `io.github.yuku123:z-asset-web` | **Web 层**：`AssetController`（`/api/asset/**`）+ `AssetAutoConfiguration`（模块级数据源、`SqlSessionFactory`、`@MapperScan`），通过 `AutoConfiguration.imports` 自动装配。 |

根 POM：`packaging=pom`，`<version>1.0.1</version>`，parent `io.github.yuku123:z-boot-parent:1.2.0`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘），3 个 `<module>` 全是库件。

仓内另有 `_frontend/z-asset-suit/`：React 19 + antd 6 + Vite 6 的独立管理台壳（`"private": true`，不发布 npm），与三个 Maven 模块互不耦合。

---

## 🧩 核心能力

### 接入一个资产来源

实现 `AssetProvider` 并声明为 bean 即可，**不需要在本仓改任何代码**：

```java
@Component
public class MyModuleAssetProvider implements AssetProvider {
    @Override public String getModule() { return "my-module"; }
    @Override public String getAssetType() { return "DATABASE"; }
    @Override public List<AssetDTO> listAssets() { /* ... */ }
    @Override public AssetDTO getAssetDetail(String assetCode) { /* ... */ }
    @Override public boolean isHealthy() { return true; }
}
```

`AssetProviderRegistrationBean` 会在这个 bean 初始化完成后把它登记进 `AssetProviderRegistry`。

### REST 端点

`AssetController` 的类级前缀是 `/api/asset`：

| 方法 | 路径 | 作用 |
|---|---|---|
| POST | `/api/asset/collect` | 触发一次全量采集（遍历所有已注册 provider） |
| GET | `/api/asset/list` | 查资产列表，可按 `module` / `assetType` 过滤，按 `updatedTime` 倒序 |
| GET | `/api/asset/providers` | 列当前已注册的全部 provider |

返回体统一是 `com.zifang.util.core.meta.Result` 包装。**这三个端点自身没有鉴权**，认证与租户隔离由宿主应用或网关负责。

### 采集规则

`AssetCollector.collectFromProvider(provider)` 带 `@Transactional`：provider 不健康 → 整轮跳过不落库；`listAssets()` 返回 null → 跳过不落库；按 `assetCode` 逐条比对，查不到则 insert、查得到则 update。`attributes` 字段序列化成**真 JSON**（`JsonUtil.toJson`）落 `z_asset.attributes` 列。

---

## 🚀 怎么跑起来

### 编译与测试

```bash
mvn clean install -DskipTests   # 全 reactor：聚合根 + 3 个模块
mvn test                        # 1 个测试类、7 支 @Test（core 模块，Mockito 打桩 mapper）
```

构建需要能从 repo1 解析到 `io.github.yuku123:z-boot-parent:1.2.0`（parent 不在磁盘上）。

### 作为库被引用

本仓**不产出可执行应用**：没有 `@SpringBootApplication`、没有 `main()`、也没有任何 `application*.yml`，因此 `server.port` 与 context-path 一律由宿主应用决定。宿主只需引 `z-asset-web`：

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-asset-web</artifactId>
    <version>1.0.1</version>
</dependency>
```

`AssetAutoConfiguration` 会通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 被 Spring Boot 自动装配：扫 `com.zifang.z.asset` 包、注册 `dataSourceAsset` 与 `sqlSessionFactoryAsset` 两个 bean、把 `com.zifang.z.asset.core.domain.mapper` 下的 mapper 绑到本模块自己的 `SqlSessionFactory`。

### 数据源配置

`dataSourceAsset` 走 `z-boot-datasource-starter` 的 `ModuleDataSourceTemplate.buildDataSource(env, "asset")`，配置键为 `z.base.db.asset.*`，缺省回退到 `z.base.db.default.*`，再回退到内置缺省：

| 键 | 内置缺省 | 说明 |
|---|---|---|
| `z.base.db.asset.host` | `localhost` | 数据库主机 |
| `z.base.db.asset.port` | `3306` | 数据库端口 |
| `z.base.db.asset.username` | `root` | 账号 |
| `z.base.db.asset.password` | 空 | 口令 |
| `z.base.db.asset.database` | 空 | 库名 |
| `z.base.db.asset.initial-size` / `min-idle` | `5` | 连接池初始/最小空闲连接数 |
| `z.base.db.asset.max-active` | `20` | 连接池最大活跃连接数 |
| `z.base.db.asset.max-wait` | `60000`（毫秒） | 取连接最长等待 |

连接串固定为 `jdbc:mysql://<host>:<port>/<database>?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8`，驱动 `com.mysql.cj.jdbc.Driver`。

### 建表

**仓内没有 `z_asset` 的建表 SQL** —— 表结构由 `@TableName("z_asset")` 与 `AssetEntity` 的 15 个字段声明（`id` 自增 + `asset_code` / `asset_name` / `asset_type` / `module` / `status` / `endpoint` / `spec` / `attributes` / `tenant_code` / `domain_code` / `collected_at` / `expire_at` / `created_time` / `updated_time`），DDL 由部署侧提供。

### 管理台壳（本地开发）

```bash
cd _frontend/z-asset-suit
npm install
npm run dev     # vite，端口由 vite.config.js 固定为 3025
```

`vite.config.js` 把 `/api` 代理到 `http://localhost:8888`（本地后端地址）。登录页当前是 mock 模式，账号 `admin` / 密码 `123456`（写在 `src/LoginPage.jsx` 的 `MOCK_AUTH` 常量里，CTC/SSO 接入前一直如此）；登录成功后把 `mock-admin` 写进 `localStorage.token`。

---

## 🧪 测试

```bash
mvn test
```

`z-asset-core` 的 `AssetCollectorTest` 共 7 支 `@Test`，全部用 Mockito 打桩 `AssetMapper`（不打桩则不落库），盯的是 `z_asset.attributes` 这一列的语义：null 时落 null、序列化结果是能被 `JsonUtil.parseObject` 原样解析回的 JSON、值里含 `,` `=` `"` 时仍无歧义；另有两支盯短路条件（provider 不健康、`listAssets()` 返回 null 时确实一库不写），以及一支记录「update 分支当前不刷新 attributes」的现状。

`z-asset-api` 与 `z-asset-web` 无测试代码。

---

## 📤 发布

三个坐标（api / core / web）全是库件，无可执行 starter，因此中央发布的 `excludeArtifacts` 为空。发布走根 POM 的 `central` profile：`maven-source-plugin` 3.3.0 + `maven-javadoc-plugin` 3.11.2 + `maven-gpg-plugin` 3.2.7 + `central-publishing-maven-plugin` 0.7.0（`tokenAuth` + `autoPublish` + `waitUntil=uploaded`）。

```bash
mvn deploy -P central
```

签名口令与 `CENTRAL_TOKEN` 等凭据一律由发布机的环境变量 / 密钥环提供，**不得写进仓内任何文件**。

---

## 文档目录

本项目文档统一收口在 `_doc/` 下（八类固定编号表，桶按需存在）:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构、接口、DB schema、协议、路线图

---

## 📄 License

MIT，见仓库根 [`LICENSE`](LICENSE)（`Copyright (c) 2026 z-opc-foundation`）。

_Maintained by the z-opc-foundation organization._
