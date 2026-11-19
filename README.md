# z-asset

> 企业资产台账 —— 管「公司买了什么、归谁、什么时候到期」

实物设备（服务器、网络设备、办公电脑）走采购→入库→领用→在用→归还→报废；权益项（域名、SSL 证书、
软件许可、云订阅）有到期日，到期前必须提醒。账目落到一张 `z_asset` 表。

**技术资源**（数据库 / 缓存 / 队列 / 模型服务 / 存储桶）是运行态、由各模块主动上报的东西，
2026-11-19 起归 **z-meta** 的 `z_resource` 台账（`ResourceProvider` SPI + `ResourceCollector` +
`ResourceController`）。本仓不再提供技术资源上报。

它由三段组成：**枚举与契约（`z-asset-api`）+ 领域层与状态机（`z-asset-core`）+ REST 面与自动装配（`z-asset-web`）**。

**技术栈：** Java 8 · Spring Boot（经 `z-boot-web-starter` / `z-boot-datasource-starter` 引入）· MyBatis-Plus（`BaseMapper` + `LambdaQueryWrapper`）· Druid + MySQL 驱动 · JUnit 5 + Mockito

---

## 📦 模块构成

| 模块 | 坐标 | 职责 |
|------|------|------|
| `z-asset-api` | `io.github.yuku123:z-asset-api` | **契约定义**：`AssetCategory`（2 个枚举值：实物 / 权益）、`AssetLifecycle`（7 个枚举值）、`AssetTransitionRequest`（流转请求）。 |
| `z-asset-core` | `io.github.yuku123:z-asset-core` | **领域层**：`ZAssetDO`（`@TableName("z_asset")`，18 个字段）+ `ZAssetMapper`、`AssetLifecycleService`（状态机与合法流转表）、`AssetService`（流转校验、到期扫描、分类统计）。实体与服务方法带 `@BizModel` / `@BizField` / `@BizMethod` / `@BizCause`（注解来自 `z-meta-api`，纯契约 jar）。 |
| `z-asset-web` | `io.github.yuku123:z-asset-web` | **Web 层**：`AssetController`（`/api/asset/**`）+ `AssetAutoConfiguration`（模块级数据源、`SqlSessionFactory`、`@MapperScan`），通过 `AutoConfiguration.imports` 自动装配。 |

根 POM：`packaging=pom`，`<version>1.0.1</version>`，parent `io.github.yuku123:z-boot-parent:1.2.6`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘），3 个 `<module>` 全是库件。

仓内另有 `_frontend/z-asset-suit/`：React 19 + antd 6 + Vite 6 的独立管理台壳（`"private": true`，不发布 npm），与三个 Maven 模块互不耦合。

---

## 🧩 核心能力

### 资产分类

`AssetCategory` 两个值：`PHYSICAL`（实物：采购→领用→归还→报废）、`ENTITLEMENT`（权益：域名 / 证书 /
许可 / 订阅，必有到期日）。分类决定一件东西**该不该有到期日**，也决定盘点时怎么问。

### 生命周期状态机

`AssetLifecycle` 七个状态：`PURCHASED` `IN_STOCK` `ALLOCATED` `IN_USE` `RETURNED` `EXPIRED` `RETIRED`。

合法流转由 `AssetLifecycleService` 单独守一张流转表，非法跳转直接拒绝、不落库 ——
「已报废的机器又被人领走了」这种事一旦进了账，盘点时才发现就晚了。`RETIRED` 是终态。

流转到 `ALLOCATED` / `IN_USE` 必须带责任人，否则拒绝。

### 模型口径写在代码里

资产表的业务口径不是写在文档里，而是用 z-meta 的注解写在实体上：

```java
@TableName("z_asset")
@BizModel(name = "企业资产", module = "asset", description = "公司持有的实物设备与权益项")
public class ZAssetDO {
    @BizField(name = "资产大类", type = BizField.FieldType.ENUM, dictCode = "asset_category")
    private String assetCategory;
    ...
}
```

宿主里只要同时有 z-meta 与本仓，z-meta 的采集器就会把这份口径扫走，
汇进 `GET /api/meta/openapi.json` 的 `components.schemas` 与 `x-methods`，
再被认知板块（`z-ontology-meta`）吃成本体 —— 资产台账的模型、字段、流转语义、因果边
因此不在任何一份需要人工同步的文档里。

`@BizCause` 还能把因果边一并声明出来：本仓声明了「执行流转 → 资产责任发生转移」
（前提：流转合法且已指定责任人）与「资产台账发生变更」（前提：有人操作了资产流转）。

### REST 端点

`AssetController` 的类级前缀是 `/api/asset`：

| 方法 | 路径 | 作用 |
|---|---|---|
| POST | `/api/asset/transition` | 流转资产生命周期状态（非法流转直接拒绝，不落库） |
| GET | `/api/asset/list` | 查资产清单，可按 `category` / `status` / `owner` 过滤，按 `gmtModified` 倒序 |
| GET | `/api/asset/detail/{assetCode}` | 查单个资产 |
| GET | `/api/asset/expiring?days=30` | 即将到期：权益资产看到期日，实物资产看保修到期 |
| GET | `/api/asset/summary` | 按大类统计资产数量 |

返回体统一是 `com.zifang.util.core.meta.Result` 包装。**这些端点自身没有鉴权**，认证与租户隔离由宿主应用或网关负责。

---

## 🚀 怎么跑起来

### 编译与测试

```bash
mvn clean install -DskipTests   # 全 reactor：聚合根 + 3 个模块
mvn test                        # core 模块：状态机 / 流转校验 / README 主张自守卫
```

构建需要能从 repo1 解析到 `io.github.yuku123:z-boot-parent:1.2.4`（parent 不在磁盘上）。

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

**仓内没有 `z_asset` 的建表 SQL** —— 表结构由 `@TableName("z_asset")` 与 `ZAssetDO` 的 18 个字段声明（`id` 自增 + `asset_code` / `asset_name` / `asset_category` / `asset_type` / `serial_no` / `vendor` / `owner` / `status` / `location` / `cost` / `purchase_date` / `expire_at` / `warranty_expire_at` / `description` / `tenant_code` / `gmt_create` / `gmt_modified`），DDL 由部署侧提供。

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

`z-asset-core` 的测试有四组：

- `AssetLifecycleServiceTest` —— 状态机：合法流转放行、非法流转拒绝、终态不可再流转；
- `AssetServiceTest` —— 流转校验（Mockito 打桩 mapper）：资产不存在、缺责任人、非法状态都拒；
- `AssetMetadataAnnotationTest` —— 注解口径没被摘掉：实体必须带 `@BizModel`、每个字段必须带 `@BizField`、流转方法必须带 `@BizMethod` 与 `@BizCause`；
- `ReadmeClaimsTest` —— README 里的定量主张（枚举值个数、实体字段数、`<module>` 条数、版本号）跟源码重算值逐条对账，README 改数字即红。

`z-asset-web` 有 `AssetControllerContractTest`：每个端点都必须带 `@BizApi`、有摘要、有业务标签。

`z-asset-api` 无测试代码。

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
- [`_doc/003_script/`](_doc/003_script/) — 构建与发布脚本（GPG passphrase 注入器）

---

## 📄 License

MIT，见仓库根 [`LICENSE`](LICENSE)（`Copyright (c) 2026 z-opc-foundation`）。

_Maintained by the z-opc-foundation organization._
