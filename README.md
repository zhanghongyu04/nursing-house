<h1 align="center">智慧康养管理平台</h1>

<p align="center">
  <a href="./README_EN.md">English</a> | 简体中文
</p>

面向养老机构、护理团队和监管管理人员的数字化康养管理系统。项目采用前后端分离架构，后端为 Java 17 + Spring Boot 3.5 多模块工程，前端为 Vue 3 + TypeScript + Vite 工程，覆盖机构管理、老人档案、护理任务、护理日志、权限治理、文件存储、视频监测、系统监控、知识库和智能问答等场景。

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-42B883?style=flat-square&logo=vue.js&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?style=flat-square&logo=typescript&logoColor=white)
![License](https://img.shields.io/badge/License-AGPL--3.0-blue?style=flat-square)

## 目录

- [功能概览](#功能概览)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [外部服务准备](#外部服务准备)
- [初始化数据与账号](#初始化数据与账号)
- [业务操作流程](#业务操作流程)
- [接口与页面](#接口与页面)
- [开发指南](#开发指南)
- [生产部署建议](#生产部署建议)
- [验收清单](#验收清单)
- [常见问题](#常见问题)
- [许可证](#许可证)

## 功能概览

### 业务管理

- 养老机构管理：维护机构基础信息、统一社会信用代码、床位数、联系人、机构照片和机构详情。
- 老人档案管理：维护老人基本信息、所属机构、监护人、入住信息、自理能力和附件材料。
- 护理任务模板：按日、周、月等规则维护周期性护理任务模板。
- 护理任务派发：为老人下发护理任务，指定护理员、计划时间、任务类型、优先级和备注。
- 护理日志：护理员完成任务后填写护理记录，机构管理员可筛选、查看和导出日志。
- 视频实时监测：管理摄像头设备，获取萤石播放配置，支持状态记录、告警处理和云台控制。
- 文件管理：通过 RustFS/S3 兼容对象存储支撑头像、机构图片、老人附件、护理日志附件和知识库文件。

### 平台能力

- 用户、角色、资源权限和数据范围管理。
- JWT 鉴权、验证码登录、在线用户和登录日志管理。
- Redis 缓存监控、服务监控、Druid 数据源监控和 Knife4j 接口文档。
- 前端根据用户资源路径动态展示菜单，路由进入前刷新用户权限信息。

### 智能化能力

- 多轮智能问答、流式响应、会话历史和上下文记忆。
- DashScope 聊天模型与 Embedding 模型接入。
- Qdrant 向量库管理、文档上传、分块、检索和知识库问答。
- 受控内部数据查询工具，用于按权限查询系统内统计信息。
- 可按配置开关启用或关闭 LLM、RAG、工具调用、记忆和防护能力。

## 系统架构

<p align="center">
  <img src="./docs/images/system-architecture.png" alt="智慧康养管理平台分层架构图" width="100%">
</p>

后端唯一启动入口是 `nh-gateway`，主类为 `com.zhiling.gateway.NhGatewayApplication`。`nh-system` 承载核心业务，`nh-agent` 承载智能问答，`nh-framework` 提供安全、Redis、MyBatis、监控、存储和 LLM Port 抽象，`nh-common` 放置公共模型、常量、异常和工具。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.5.0、Spring Security、Spring Web |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 6.x+ |
| 数据源 | Druid |
| 接口文档 | Knife4j / OpenAPI |
| AI 框架 | Spring AI、Spring AI Alibaba |
| 模型服务 | DashScope |
| 向量数据库 | Qdrant |
| 对象存储 | RustFS / S3 Compatible |
| 视频平台 | 萤石云开放平台、EZUIKit |
| 前端框架 | Vue 3、TypeScript、Vite |
| 前端生态 | Pinia、Vue Router、Element Plus、ECharts、Axios |

## 仓库结构

```text
nursing-house/
├── nh-common/       公共常量、实体、DTO、VO、异常、工具和上下文模型
├── nh-framework/    平台抽象、安全上下文、Port 接口、MyBatis 配置和基础设施能力
├── nh-system/       用户权限、机构、老人、护理、文件、监控、视频、向量库等业务模块
├── nh-agent/        智能问答、会话、RAG、提示词、工具调用、多模态处理
├── nh-gateway/      Spring Boot 启动模块，负责装配后端运行时
├── nh-front/        Vue 3 前端工程
├── sql/             MySQL 初始化脚本
├── pom.xml          Maven 父工程
├── README.md        项目说明
└── LICENSE          AGPL-3.0 许可证
```

## 环境要求

| 依赖 | 建议版本 | 必需 | 说明 |
| --- | --- | --- | --- |
| JDK | 17 | 是 | 后端编译和运行，父工程已指定 Java 17 |
| Maven | 3.8+ | 是 | 后端依赖管理、编译、打包 |
| Node.js | 18+ / 20+ | 是 | 前端开发、类型检查和构建 |
| npm | 随 Node 安装 | 是 | 前端依赖安装 |
| MySQL | 8.x | 是 | 业务数据、账号、权限、配置和日志 |
| Redis | 6.x+ | 是 | 验证码、Token、在线状态、Agent 记忆 |
| DashScope | 在线服务 | 可选 | 聊天模型、Embedding、RAG |
| Qdrant | 1.13.x | 可选 | 知识库向量检索 |
| RustFS | S3 兼容版本 | 可选 | 文件上传、头像、附件、机构图片 |
| 萤石云 | 在线服务 | 可选 | 真实摄像头播放、云台和告警能力 |

只验证登录、菜单、机构、老人、护理任务和护理日志时，至少准备 MySQL、Redis、后端和前端。文件、视频、知识库、智能问答可以在验证对应功能前再配置。

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/zhanghongyu04/smart-nursing-house.git
cd nursing-house
```

### 2. 创建并导入数据库

```sql
CREATE DATABASE IF NOT EXISTS `nursing-home`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

```bash
mysql -u root -p nursing-home < sql/nursing-home.sql
```

### 3. 设置开发环境变量

PowerShell：

```powershell
$env:NURSING_HOUSE_DB_USERNAME="root"
$env:NURSING_HOUSE_DB_PASSWORD="your_mysql_password"
$env:NURSING_HOUSE_DRUID_PASSWORD="your_druid_password"
$env:NURSING_HOUSE_DEFAULT_PASSWORD="InitPwd@123"
$env:NURSING_HOUSE_TOKEN_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
$env:NURSING_HOUSE_REDIS_PASSWORD=""
```

Bash：

```bash
export NURSING_HOUSE_DB_USERNAME=root
export NURSING_HOUSE_DB_PASSWORD=your_mysql_password
export NURSING_HOUSE_DRUID_PASSWORD=your_druid_password
export NURSING_HOUSE_DEFAULT_PASSWORD=InitPwd@123
export NURSING_HOUSE_TOKEN_SECRET=replace-with-a-long-random-secret-at-least-32-bytes
export NURSING_HOUSE_REDIS_PASSWORD=
```

如需验证文件、AI 或视频能力，继续配置 RustFS、DashScope、Qdrant、萤石云相关变量。详见 [配置说明](#配置说明)。

### 4. 启动后端

```bash
mvn -pl nh-gateway -am spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
mvn -pl nh-gateway -am clean package -DskipTests
java -jar nh-gateway/target/nh-gateway-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

默认后端地址：

```text
http://localhost:8080
```

### 5. 启动前端

```bash
cd nh-front
npm install
npm run dev
```

当前 Vite 开发端口为 `5175`，并将 `/api` 代理到 `http://localhost:8080`。

```text
http://localhost:5175
```

### 6. 访问常用入口

| 地址 | 说明 |
| --- | --- |
| `http://localhost:5175` | 前端开发服务 |
| `http://localhost:8080/doc.html` | Knife4j 接口文档 |
| `http://localhost:8080/druid/` | Druid 数据源监控 |
| `http://localhost:8080/api/v1/captcha` | 验证码接口 |

## 配置说明

默认 profile 在 `nh-gateway/src/main/resources/application.yml` 中配置为 `${SPRING_PROFILES_ACTIVE:dev}`。未设置 `SPRING_PROFILES_ACTIVE` 时会使用 `dev`。

### dev 最小配置

`application-dev.yml` 默认使用：

- 后端端口：`8080`
- MySQL：`localhost:3306/nursing-home`
- Redis：`localhost:6379`
- Qdrant gRPC：`localhost:6334`
- RustFS：`http://localhost:9000`
- 前端端口：`5175`

| 环境变量 | 默认值 | 必填性 | 用途 |
| --- | --- | --- | --- |
| `NURSING_HOUSE_DB_USERNAME` | `root` | 否 | MySQL 用户名 |
| `NURSING_HOUSE_DB_PASSWORD` | 无 | 是 | MySQL 密码 |
| `NURSING_HOUSE_DRUID_USERNAME` | `admin` | 否 | Druid 监控用户名 |
| `NURSING_HOUSE_DRUID_PASSWORD` | 无 | 是 | Druid 监控密码 |
| `NURSING_HOUSE_REDIS_PASSWORD` | 空 | 按 Redis 配置 | Redis 密码，无密码可留空 |
| `NURSING_HOUSE_DEFAULT_PASSWORD` | 无 | 建议必填 | 新建或重置用户时使用的默认密码 |
| `NURSING_HOUSE_TOKEN_SECRET` | 无 | 是 | JWT HS256 签名密钥，建议 32 字节以上随机值 |
| `NURSING_HOUSE_DASHSCOPE_API_KEY` | 无 | AI 功能必填 | DashScope API Key |
| `NURSING_HOUSE_RUSTFS_ACCESS_KEY_ID` | 无 | 文件功能必填 | RustFS Access Key |
| `NURSING_HOUSE_RUSTFS_SECRET_ACCESS_KEY` | 无 | 文件功能必填 | RustFS Secret Key |
| `NURSING_HOUSE_EZVIZ_APP_KEY` | 空 | 真实视频必填 | 萤石开放平台 App Key |
| `NURSING_HOUSE_EZVIZ_APP_SECRET` | 空 | 真实视频必填 | 萤石开放平台 App Secret |

### prod 基础配置

生产 profile 需要显式设置 `SPRING_PROFILES_ACTIVE=prod`。生产环境的数据库、Redis、Qdrant、RustFS 等地址不应写死在配置文件中，推荐通过环境变量注入。

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SERVER_PORT="8080"

$env:NURSING_HOUSE_DB_URL="jdbc:mysql://127.0.0.1:3306/nursing-home?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true"
$env:NURSING_HOUSE_DB_USERNAME="nh_user"
$env:NURSING_HOUSE_DB_PASSWORD="your_mysql_password"

$env:NURSING_HOUSE_REDIS_HOST="127.0.0.1"
$env:NURSING_HOUSE_REDIS_PORT="6379"
$env:NURSING_HOUSE_REDIS_PASSWORD=""

$env:NURSING_HOUSE_TOKEN_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
$env:NURSING_HOUSE_DEFAULT_PASSWORD="InitPwd@123"
```

| 环境变量 | 默认值 | 必填性 | 用途 |
| --- | --- | --- | --- |
| `SERVER_PORT` | `8080` | 否 | 后端服务端口 |
| `NURSING_HOUSE_DB_URL` | 无 | 是 | MySQL JDBC URL |
| `NURSING_HOUSE_DB_USERNAME` | 无 | 是 | MySQL 用户名 |
| `NURSING_HOUSE_DB_PASSWORD` | 无 | 是 | MySQL 密码 |
| `NURSING_HOUSE_REDIS_HOST` | 无 | 是 | Redis 主机 |
| `NURSING_HOUSE_REDIS_PORT` | `6379` | 否 | Redis 端口 |
| `NURSING_HOUSE_REDIS_DATABASE` | `0` | 否 | 默认 Redis DB |
| `NURSING_HOUSE_REDIS_AUTH_DATABASE` | `0` | 否 | 认证、验证码、Token 使用的 Redis DB |
| `NURSING_HOUSE_REDIS_AGENT_DATABASE` | `1` | 否 | Agent 记忆使用的 Redis DB |
| `NURSING_HOUSE_TOKEN_HEADER` | `Authorization` | 否 | Token 请求头 |
| `NURSING_HOUSE_TOKEN_SECRET` | 无 | 是 | JWT 签名密钥 |
| `NURSING_HOUSE_TOKEN_EXPIRE_TIME` | `1800000` | 否 | Token 有效期，单位毫秒 |
| `NURSING_HOUSE_DEFAULT_PASSWORD` | 无 | 建议必填 | 新建或重置用户默认密码 |
| `NURSING_HOUSE_LOG_FILE` | `logs/nursing-home.log` | 否 | 日志文件路径 |

### MySQL 与 Druid 调优

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NURSING_HOUSE_DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | JDBC 驱动 |
| `NURSING_HOUSE_DB_INITIAL_SIZE` | `5` | Druid 初始连接数 |
| `NURSING_HOUSE_DB_MIN_IDLE` | `5` | 最小空闲连接 |
| `NURSING_HOUSE_DB_MAX_ACTIVE` | `20` | 最大连接数 |
| `NURSING_HOUSE_DB_MAX_WAIT` | `60000` | 获取连接最大等待时间 |
| `NURSING_HOUSE_DB_VALIDATION_QUERY` | `SELECT 1` | 连接检测 SQL |
| `NURSING_HOUSE_DRUID_STAT_ENABLED` | `false` | 生产 profile 是否启用 Druid 监控页 |
| `NURSING_HOUSE_DRUID_USERNAME` | `admin` | Druid 监控用户名 |
| `NURSING_HOUSE_DRUID_PASSWORD` | `admin` | Druid 监控密码，生产环境应修改 |

### DashScope 与 LLM

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NURSING_HOUSE_DASHSCOPE_API_KEY` | 无 | DashScope API Key |
| `NURSING_HOUSE_DASHSCOPE_BASE_URL` | `https://dashscope.aliyuncs.com` | DashScope API 地址 |
| `NURSING_HOUSE_DASHSCOPE_CHAT_MODEL` | `qwen3.5-plus` | 聊天模型 |
| `NURSING_HOUSE_DASHSCOPE_MULTI_MODEL` | `true` | 多模态模型开关 |
| `NURSING_HOUSE_DASHSCOPE_EMBEDDING_MODEL` | `text-embedding-v3` | Embedding 模型 |
| `NURSING_HOUSE_DASHSCOPE_EMBEDDING_DIMENSIONS` | `1024` | Embedding 维度 |
| `NURSING_HOUSE_LLM_ENABLED` | `true` | LLM 总开关 |
| `NURSING_HOUSE_LLM_RAG_ENABLED` | `true` | RAG 开关 |
| `NURSING_HOUSE_LLM_TOOL_ENABLED` | `true` | 工具调用开关 |
| `NURSING_HOUSE_LLM_MEMORY_ENABLED` | `true` | 记忆开关 |
| `NURSING_HOUSE_LLM_CHAT_EXPOSE_API` | `true` | 是否暴露聊天 API |
| `NURSING_HOUSE_LLM_CONTEXT_MESSAGE_LIMIT` | `12` | 拼接历史消息条数 |

`NURSING_HOUSE_DASHSCOPE_EMBEDDING_DIMENSIONS`、`NURSING_HOUSE_QDRANT_VECTOR_SIZE` 和 Qdrant collection 的实际向量维度必须一致，当前推荐为 `1024`。

### Qdrant

项目同时使用 `spring.ai.vectorstore.qdrant` 和 `nursing-house.qdrant` 两组配置。两组 host、port、collection、api-key 应保持一致。

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NURSING_HOUSE_QDRANT_HOST` | dev 为 `localhost`，prod 无 | Qdrant 主机 |
| `NURSING_HOUSE_QDRANT_GRPC_PORT` | `6334` | Qdrant gRPC 端口，不是 HTTP 端口 `6333` |
| `NURSING_HOUSE_QDRANT_COLLECTION` | `nursing-home-docs` | 知识库集合名 |
| `NURSING_HOUSE_QDRANT_INITIALIZE_SCHEMA` | dev 为 `true`，prod 为 `false` | 是否自动创建集合 |
| `NURSING_HOUSE_QDRANT_VECTOR_SIZE` | `1024` | 向量维度 |
| `NURSING_HOUSE_QDRANT_API_KEY` | 空 | Qdrant 开启认证时填写 |
| `NURSING_HOUSE_QDRANT_USE_TLS` | `false` | Spring AI Qdrant 是否使用 TLS |
| `NURSING_HOUSE_QDRANT_USE_HTTPS` | `false` | 自定义 Qdrant Client 是否使用 HTTPS |
| `NURSING_HOUSE_QDRANT_BATCHING_STRATEGY` | `TOKEN_COUNT` | Spring AI 文档批处理策略 |

### RustFS

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NURSING_HOUSE_RUSTFS_ENABLED` | `true` | 是否启用文件存储能力 |
| `NURSING_HOUSE_RUSTFS_ENDPOINT` | dev 为 `http://localhost:9000`，prod 无 | RustFS S3 API 地址 |
| `NURSING_HOUSE_RUSTFS_ACCESS_KEY_ID` | 无 | Access Key |
| `NURSING_HOUSE_RUSTFS_SECRET_ACCESS_KEY` | 无 | Secret Key |
| `NURSING_HOUSE_RUSTFS_BUCKET_NAME` | `nursing-home` | Bucket 名称 |
| `NURSING_HOUSE_RUSTFS_REGION` | `us-east-1` | S3 region |
| `NURSING_HOUSE_RUSTFS_PATH_STYLE_ACCESS_ENABLED` | `true` | Path-style 访问，RustFS 通常需要保持开启 |

后端启动后可访问 `/api/v1/rustfs/health` 检查 RustFS 连通性。

### 萤石云

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `NURSING_HOUSE_EZVIZ_APP_KEY` | 空 | 萤石开放平台 App Key |
| `NURSING_HOUSE_EZVIZ_APP_SECRET` | 空 | 萤石开放平台 App Secret |
| `NURSING_HOUSE_EZVIZ_BASE_URL` | `https://open.ys7.com` | 萤石开放平台 API 地址 |
| `NURSING_HOUSE_EZVIZ_TOKEN_REFRESH_BEFORE_EXPIRE_SECONDS` | `300` | Token 提前刷新秒数 |
| `NURSING_HOUSE_EZVIZ_CONNECT_TIMEOUT_MS` | `5000` | HTTP 连接超时 |
| `NURSING_HOUSE_EZVIZ_READ_TIMEOUT_MS` | `10000` | HTTP 读取超时 |

如果环境变量为空，系统会尝试从数据库第三方平台配置表读取可用萤石配置。接入真实摄像头时，还需要在视频管理中维护设备序列号、通道号、设备验证码和清晰度。

### 前端

| 变量或配置 | 默认值 | 用途 |
| --- | --- | --- |
| `VITE_ENABLE_PROMPT_CONSOLE` | dev 为 `true`，prod 为 `false` | 是否显示提示词控制台入口 |
| `VITE_API_BASE_URL` | 空 | Axios API 基地址；为空时走同源相对路径 |
| `vite.server.port` | `5175` | 前端开发服务端口 |
| `vite.server.proxy['/api'].target` | `http://localhost:8080` | 开发环境后端代理地址 |

生产环境若通过 Nginx 同源代理 `/api/`，通常不需要设置 `VITE_API_BASE_URL`。如果前端直接请求独立 API 域名，可在构建前设置该变量。

## 外部服务准备

### MySQL

建议使用 MySQL 8.x。创建数据库后导入 `sql/nursing-home.sql`。如果启动后登录菜单为空，通常是 SQL 未完整导入、角色资源缺失或账号权限关系缺失。

### Redis

默认端口 `6379`。系统使用 DB0 保存验证码、Token、在线状态等认证相关数据，使用 DB1 保存 Agent 记忆相关数据。Redis 设置密码时必须同步配置 `NURSING_HOUSE_REDIS_PASSWORD`。

### Qdrant

Qdrant HTTP/Web UI 通常是 `6333`，本项目连接的是 gRPC `6334`。

```bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant:v1.13.6
```

首次部署可将 `NURSING_HOUSE_QDRANT_INITIALIZE_SCHEMA=true`，确认集合创建后再按运维策略决定是否关闭自动建 schema。

### RustFS

RustFS 默认 S3 API 端口为 `9000`，Console 通常为 `9001`。项目会检查并尝试创建默认 bucket `nursing-home`。endpoint 必须带协议，例如 `http://localhost:9000`。

### DashScope

到 DashScope 或阿里云百炼控制台创建 API Key，并配置 `NURSING_HOUSE_DASHSCOPE_API_KEY`。如果知识库上传失败，需要同时检查 DashScope 网络连通性、Embedding 模型、Qdrant gRPC 端口和向量维度。

### 萤石云

到萤石云开放平台创建应用，获取 App Key 和 App Secret。真实播放还需要摄像头设备序列号、通道号、设备验证码和设备在线状态。初始化数据中的摄像头仅作为页面和数据结构示例。

## 初始化数据与账号

`sql/nursing-home.sql` 包含系统运行所需的菜单、资源、角色、字典、平台配置、示例账号和示例机构。

| 账号 | 初始密码 | 角色口径 | 机构范围 | 主要可见模块 |
| --- | --- | --- | --- | --- |
| `admin1` | `123456` | 政府管理员 / 平台管理员 | 全局 | 机构、老人、系统管理、知识库、智能体、视频、系统监控 |
| `user1` | `admin123` | 母机构管理员 | `149`、`150` | 机构、老人、用户管理、知识库、智能体、视频、护理管理 |
| `user2` | `admin123` | 子机构管理员 | `150` | 机构、老人、用户管理、知识库、智能体、视频、护理管理 |
| `nurse` | `admin123` | 护理人员 | `149` | 老人信息、养护智能体、我的护理 |

初始化 SQL 中保存的是 bcrypt 哈希，登录时使用上表中的初始密码。投入长期使用前，建议登录后及时修改各账号密码。

初始化数据中老人档案、护理任务、护理日志、智能问答会话、会话消息和机构图片记录为空，便于从干净状态开始录入和验收业务流程。示例机构保留 `149 青山康养中心` 和 `150 暖阳护理院分院`。

## 业务操作流程

### 登录与权限

1. 打开 `http://localhost:5175/login`。
2. 前端请求 `GET /api/v1/captcha` 获取验证码。
3. 登录时请求 `POST /api/v1/login`。
4. 登录成功后请求 `GET /api/v1/user/getUserNavInfo` 获取角色、机构范围和资源路径。
5. 前端导航栏根据资源路径显示菜单，不同账号看到的功能不同。

### 机构管理

1. `admin1` 或有权限账号进入 `/nursingHomeList`。
2. 列表查询调用 `POST /api/v1/sanatorium/page`。
3. 新增、编辑、删除机构分别对应 `/api/v1/sanatorium/add`、`/update`、`/delete`。
4. 机构详情页 `/nursingHomeDetail` 可查看自理能力分布、机构老人列表和机构照片。
5. 机构照片先通过 `/api/v1/commonFile/upload` 上传，再通过 `/api/v1/sanaImage/add` 绑定。

### 老人档案

1. 进入 `/elderInfo`。
2. 列表查询调用 `POST /api/v1/elder/page`。
3. 新增老人需选择所属机构，并填写姓名、性别、年龄、电话、入住类型、自理能力、床位、房间、费用、监护人、入院时间等。
4. 老人附件使用 `/api/v1/elder/attachments/upload` 上传，列表和删除分别走 `/api/v1/elder/attachments` 与 `/delete`。

### 护理任务与日志

1. `user1` 或 `user2` 进入 `/nursingTaskTemplate` 创建周期性任务模板。
2. 进入 `/nursingTaskDispatch` 下发护理任务，选择老人、护理员、计划时间、任务内容和优先级。
3. `nurse` 进入 `/myNursingTask` 查看分配给自己的任务。
4. 护理员完成任务后跳转 `/writeNursingLog` 填写护理日志，可上传附件并标记异常情况。
5. 机构管理员在 `/nursingLog` 查看、筛选和导出护理日志。

### 视频监测

1. 进入 `/Monitor`。
2. 通过 `GET /api/v1/video/list` 获取设备列表。
3. 通过 `GET /api/v1/video/{cameraId}/play-config` 获取播放配置。
4. 设备添加、编辑、删除和云台控制需要萤石凭据、真实设备序列号、通道号和验证码。

### 智能问答与知识库

1. 进入 `/vectorStore` 上传 PDF、Word 或 TXT 文档，系统会分块、生成 Embedding 并写入 Qdrant。
2. 进入 `/agent` 创建会话并发送问题。
3. 会话创建、列表、删除和消息历史分别走 `/api/v1/agent/session`、`/session/list`、`/session/{conversationId}` 和 `/api/v1/agent/{type}/{chatId}`。
4. 智能问答依赖 DashScope、Qdrant 和 Redis，会根据账号权限和机构范围处理上下文。

## 接口与页面

### 常用接口分组

| 能力 | 主要接口 |
| --- | --- |
| 登录认证 | `/api/v1/login`、`/api/v1/logout`、`/api/v1/captcha` |
| 用户导航 | `/api/v1/user/getUserNavInfo` |
| 机构管理 | `/api/v1/sanatorium/**` |
| 老人档案 | `/api/v1/elder/**` |
| 护理任务模板 | `/api/v1/nursing-task-template/**` |
| 护理任务 | `/api/v1/nursing-task/**` |
| 护理日志 | `/api/v1/nursing-log/**` |
| 文件服务 | `/api/v1/commonFile/**`、`/api/v1/rustfs/**` |
| 视频监控 | `/api/v1/video/**` |
| 系统监控 | `/api/v1/monitor/**` |
| 知识库 | `/api/v1/vector-store/**` |
| 智能问答 | `/api/v1/agent/**` |
| LLM 兼容接口 | `/api/llm/**` |

权限资源通常使用 `/web/...` 口径，前端路由守卫按用户资源路径放行页面，业务请求实际调用 `/api/v1/...`。

### 前端主要页面

| 路由 | 功能 |
| --- | --- |
| `/login` | 登录 |
| `/` | 首页统计 |
| `/nursingHomeList` | 机构列表 |
| `/nursingHomeDetail` | 机构详情 |
| `/elderInfo` | 老人档案 |
| `/userManage` | 用户管理 |
| `/permission` | 权限管理 |
| `/Monitor` | 视频监控 |
| `/CacheControl` | Redis 监控 |
| `/ServiceControl` | 服务监控 |
| `/LoginMonitor` | 登录与在线用户监控 |
| `/agent` | 智能问答 |
| `/vectorStore` | 知识库管理 |
| `/nursingTaskTemplate` | 护理任务模板 |
| `/nursingTaskDispatch` | 护理任务派发 |
| `/nursingLog` | 护理日志管理 |
| `/myNursingTask` | 护理员任务 |
| `/writeNursingLog` | 护理员填写日志 |

## 开发指南

### 后端

```bash
# 编译全部模块
mvn clean package -DskipTests

# 编译启动模块及其依赖
mvn -pl nh-gateway -am package -DskipTests

# 启动开发环境
mvn -pl nh-gateway -am spring-boot:run -Dspring-boot.run.profiles=dev
```

开发建议：

- Controller 保持薄层，复杂业务下沉到 application/domain/service。
- 跨模块能力优先通过 Port 接口调用，避免业务模块直接互相耦合。
- 新增接口时同步维护权限资源、角色授权、前端路由和菜单显示逻辑。
- 新增表字段时同步更新实体、Mapper、DTO/VO、前端类型和初始化 SQL。
- 涉及文件、AI、视频的功能要提供外部服务不可用时的友好提示或降级路径。

### 前端

```bash
cd nh-front
npm install
npm run dev
npm run build
npm run preview
```

开发建议：

- API 调用统一放在 `src/api/`。
- 页面放在 `src/views/`，复用组件放在 `src/components/`。
- 登录态、用户信息和权限资源通过 Pinia store 管理。
- 新增页面时同步配置路由、菜单资源和后端权限。
- 开发环境只代理 `/api` 到后端；如果新增了非 `/api` 前缀请求，需要同步调整 Vite 代理或请求封装。

## 生产部署建议

### 后端

```bash
mvn -pl nh-gateway -am clean package -DskipTests
java -jar nh-gateway/target/nh-gateway-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

建议通过进程管理工具或容器平台托管后端服务，并把 MySQL、Redis、DashScope、Qdrant、RustFS、萤石等配置放在环境变量或运行平台的 Secret/Config 中。

### 前端

```bash
cd nh-front
npm install
npm run build
```

构建产物位于 `nh-front/dist`。推荐使用 Nginx 托管静态资源，并将 `/api/` 反向代理到后端：

```nginx
server {
    listen 80;
    server_name nursing-house.example.com;

    root /opt/nursing-house/nh-front/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    client_max_body_size 50m;
}
```

生产注意事项：

- 使用 HTTPS。
- 使用独立数据库账号，按最小权限授权。
- Redis、MySQL、RustFS、Qdrant 不建议直接暴露公网。
- JWT 密钥、数据库密码、DashScope Key、RustFS Key、萤石 Key 只通过环境变量或 Secret 管理。
- Druid、Knife4j、监控类入口建议限制内网访问。
- 根据并发调整 Druid 连接池、Redis 连接池、JVM 参数和 Nginx 超时时间。
- 文件上传大小需要同时检查后端 multipart 限制、Nginx `client_max_body_size` 和对象存储策略。

## 验收清单

| 项目 | 检查内容 | 期望结果 |
| --- | --- | --- |
| 环境 | MySQL、Redis、后端、前端启动 | 后端 8080、前端 5175 可访问，日志无启动异常 |
| 数据 | 导入 `sql/nursing-home.sql` | 四个示例账号可用于登录 |
| 权限 | 分别登录 `admin1`、`user1`、`user2`、`nurse` | 菜单与角色范围匹配 |
| 机构 | 查询机构列表和详情 | 只能看到授权机构 |
| 老人 | 新增老人并上传附件 | 老人出现在列表，附件可回显 |
| 护理 | 管理员派发任务，护理员完成并写日志 | 任务状态和日志记录正常 |
| 文件 | 调用 `/api/v1/rustfs/health` 并上传文件 | RustFS 健康，页面可访问文件 |
| AI | 上传知识库文档并在智能体提问 | Qdrant 有数据，问答接口返回正常 |
| 视频 | 配置真实萤石设备并打开监控页 | 能获取播放配置并播放 |
| 文档 | 打开 `/doc.html` | Knife4j 接口文档可访问 |

## 常见问题

### 后端启动时报占位符无法解析

检查无默认值变量是否设置，尤其是 `NURSING_HOUSE_DB_PASSWORD`、`NURSING_HOUSE_DRUID_PASSWORD`、`NURSING_HOUSE_TOKEN_SECRET`、`NURSING_HOUSE_DEFAULT_PASSWORD`、`NURSING_HOUSE_DASHSCOPE_API_KEY`、`NURSING_HOUSE_RUSTFS_ACCESS_KEY_ID`、`NURSING_HOUSE_RUSTFS_SECRET_ACCESS_KEY`。如果只验证基础业务，可先关闭或跳过 AI、文件、视频相关功能验证。

### 数据库连接失败

检查 MySQL 是否启动，库名是否为 `nursing-home`，账号密码是否正确，JDBC URL 是否包含正确端口、时区、字符集和 `allowPublicKeyRetrieval=true`。

### 登录后菜单为空

通常是 SQL 未完整导入、账号未绑定角色、角色未绑定资源，或前端登录态过期。重新导入初始化 SQL 后再登录验证。

### Redis 连接失败

检查 Redis 是否启动，端口是否为 `6379`，密码是否与 `NURSING_HOUSE_REDIS_PASSWORD` 一致。Agent 记忆默认使用 DB1，认证相关数据默认使用 DB0。

### 前端请求不到后端

开发环境确认访问的是 `http://localhost:5175`，后端运行在 `8080`，Vite 代理 `/api` 到 `http://localhost:8080`。生产环境确认 Nginx 已正确代理 `/api/`，或构建前设置了正确的 `VITE_API_BASE_URL`。

### AI 问答或知识库不可用

检查 DashScope API Key、模型名称、网络连通性、Qdrant gRPC 端口 `6334`、集合 `nursing-home-docs` 和向量维度 `1024`。上传知识库文档会同时依赖 DashScope Embedding 和 Qdrant。

### 文件上传失败

检查 RustFS endpoint 是否带 `http://` 或 `https://`，Access Key 与 Secret Key 是否正确，bucket `nursing-home` 是否可创建，Nginx 和后端 multipart 大小限制是否满足上传文件大小。

### 视频无法播放

检查萤石 App Key、App Secret、设备序列号、通道号、设备验证码、设备在线状态和浏览器播放能力。初始化设备数据仅用于页面结构验证，真实播放需要替换为可用设备。

## 许可证

本项目采用 AGPL-3.0 协议，详见 [LICENSE](./LICENSE)。
