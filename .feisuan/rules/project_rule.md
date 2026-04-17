
# Nova Health Agent 开发规范指南

> **作者**：18128
> **创建时间**：2026-04-17
> **项目路径**：`C:\Users\18128\Desktop\nova-health-agent`

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目环境与技术栈

- **操作系统**：Windows 11
- **工作目录**：`C:\Users\18128\Desktop\nova-health-agent`
- **构建工具**：Maven
- **JDK 版本**：Java 21
- **主框架**：Spring Boot 3.5.13
- **核心依赖**：
  - `spring-boot-starter-web` (Web 服务)
  - `spring-ai-alibaba-starter-dashscope` (阿里云灵积 AI)
  - `spring-ai-starter-mcp-client` (Model Context Protocol 客户端)
  - `knife4j-openapi3-jakarta-spring-boot-starter` (API 文档)
  - `hutool-all` (工具类库)
  - `lombok` (代码简化)

## 二、项目目录结构

本项目为前后端分离架构，后端基于 Spring Boot，前端位于 `nova-front`。

```text
nova-health-agent
├── nova-front                 # 前端项目目录
    └── src
        ├── api               # API 接口定义
        ├── components        # 公共组件
        ├── router            # 路由配置
        └── views             # 页面视图
├── nova-health-back          # 后端核心模块
    ├── src
    │   ├── main
    │   │   ├── java
    │   │   │   └── com
    │   │   │       └── nova
    │   │   │           ├── advisor      # AI 顾问/增强器配置
    │   │   │           ├── agent        # AI Agent 代理逻辑
    │   │   │           │   └── model    # Agent 相关数据模型
    │   │   │           ├── app          # 应用核心业务逻辑
    │   │   │           ├── chatmemory   # 聊天记忆持久化
    │   │   │           ├── config       # Spring 配置类
    │   │   │           ├── constant     # 常量定义
    │   │   │           ├── controller   # 控制器层
    │   │   │           ├── rag          # 检索增强生成 (RAG) 逻辑
    │   │   │           └── tools        # AI 工具/函数调用定义
    │   │   └── resources
    │   │       └── document      # RAG 知识库文档源文件
    │   └── test
    │       └── java
    │           └── com
    │               └── nova
    │                   ├── agent        # Agent 测试
    │                   └── tools        # 工具测试
    └── tmp                     # 临时文件目录 (不纳入版本控制)
        ├── chat-mempry         # 聊天记录临时存储
        ├── download            # 下载文件临时存储
        ├── file                # 通用文件临时存储
        └── pdf                 # PDF 生成/处理临时存储
└── tmp                         # 根级临时目录
    └── pdf
```

## 三、分层架构规范

| 层级        | 职责说明                                       | 开发约束与注意事项                                               |
|-------------|------------------------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口             | 不得包含业务逻辑，必须调用 `app` 或 `agent` 层服务；统一返回格式 |
| **Agent**      | AI 代理编排层，处理大模型交互与工具调用         | 负责与 DashScope 及 MCP 通信；管理 Prompt 和 Chat Memory        |
| **App/Service**| 核心业务逻辑层                                   | 处理具体的业务计算、数据转换及非 AI 相关的逻辑                  |
| **Tools**      | AI 函数工具层                                   | 供 Agent 调用的具体功能实现（如搜索、计算、IO 操作）             |
| **RAG**        | 检索增强生成层                                  | 负责文档加载、切分、向量化及检索逻辑                            |
| **Config**     | 配置层                                          | 管理 Bean 的配置，如 AI Client、Vector Store 等                 |

### 接口与实现分离

- 建议将复杂业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。

## 四、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（位于 `jakarta.validation.constraints.*`）。
- 禁止手动拼接 SQL 字符串（尽管本项目当前未直接暴露 JDBC，但需遵循安全原则）。
- 敏感信息（如 API Key）应配置在 `application.yml` 中或环境变量中，严禁硬编码在代码里。

### AI 调用与资源管理

- **Chat Memory**：合理管理对话历史，避免 Token 消耗过大，使用 `chatmemory` 包进行持久化或清理。
- **MCP 连接**：MCP 客户端配置在 `application.yml` 的 `spring.ai.mcp` 节点下，连接地址需通过配置文件管理。

### 事务管理

- `@Transactional` 注解仅用于 **Service/App 层**方法。
- 避免在循环中频繁提交事务。

## 五、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `HealthAgentImpl`     |
| 方法/变量  | lowerCamelCase       | `generateResponse()`  |
| 常量       | UPPER_SNAKE_CASE     | `MAX_TOKEN_LIMIT`     |

### 注释规范

- **语言要求**：请使用**中文**编写注释，确保团队成员能快速理解。
- 所有类、方法、字段需添加 **Javadoc** 注释。
- Agent 的 Prompt 模板或工具描述建议使用清晰的中文注释说明用途。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例             |
|------|------------------------------|------------------|
| DTO  | 数据传输对象                 | `PatientDTO`     |
| VO   | 视图展示对象（返回给前端）   | `ReportVO`       |
| Query| 查询参数封装对象             | `AgentQuery`     |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `@Slf4j` (用于日志)

## 六、扩展性与日志规范

### 日志记录

- 统一使用 `@Slf4j` 注解进行日志记录。
- 禁止使用 `System.out.println`。
- AI 交互过程中的关键步骤（如 Prompt 发送、Tool 调用、接收结果）应记录 INFO 级别日志。

### API 文档

- 使用 **Knife4j** (增强版 Swagger) 进行接口文档管理。
- 访问地址通常为：`http://localhost:9999/api/doc.html`。
- 所有 Controller 接口需添加中文注解 `@ApiOperation` 和 `@ApiParam`。

## 七、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性（如工具类提取）   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞                           |
