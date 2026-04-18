<div align="center">

# 🎓 Campus QA — 校园智能问答系统

**基于 RAG 架构的校园信息问答系统 · Spring Boot + Spring AI + 微信小程序**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-blue?logo=spring)](https://docs.spring.io/spring-ai/reference/)
[![WeChat Mini Program](https://img.shields.io/badge/WeChat-Mini%20Program-07C160?logo=wechat)](https://developers.weixin.qq.com/miniprogram/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

</div>

---

## 📖 项目简介

Campus QA 是一个面向校园场景的智能问答系统，采用 **"知识检索 + 大语言模型生成"** 的 RAG（Retrieval-Augmented Generation）架构，为师生提供可追溯、可解释、开箱即用的校园信息问答能力。

### ✨ 核心亮点

| 特性 | 说明 |
|------|------|
| 🔍 **语义检索** | 基于 `SimpleVectorStore` 向量检索，理解用户意图而非简单关键词匹配 |
| 🤖 **AI 增强回答** | 接入阿里云百炼（DashScope）大模型，回答结构化、有据可查 |
| 📡 **SSE 流式输出** | 逐字回复，实时感受 AI 思考过程 |
| 🔌 **离线可运行** | 本地 ONNX Embedding + Mock ChatModel，无 API Key 也能完整演示 |
| 💾 **向量持久化** | 向量索引本地缓存，重启免重建，冷启动秒级完成 |
| 📱 **微信小程序** | 原生开发，自定义 TabBar，支持流式渲染与本地 Mock 数据 |

### 🎯 适用场景

- 毕业设计 / 课程设计答辩展示
- 校园知识问答系统原型验证
- Spring AI + RAG + 微信小程序整合学习
- 本地离线可演示的 RAG 项目模板

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      微信小程序 (WXML/WXSS/JS)                   │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐          │
│  │ 登录 │ │ 首页  │ │  AI  │ │ 提问 │ │ 详情 │ │ 我的  │          │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘          │
└───────────────────────────┬─────────────────────────────────────┘
                            │  HTTP / SSE
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot 3.3.6 (Java 21)                   │
│                                                                 │
│  ┌─────────────┐  ┌─────────────────┐  ┌──────────────────┐     │
│  │  Controller │  │    AiService    │  │  ExceptionHandler│     │
│  │  REST / SSE │──│  Prompt + RAG   │  │  全局异常处理     │     │
│  └─────────────┘  └────────┬────────┘  └──────────────────┘     │
│                            │                                    │
│         ┌─────────────────┼─────────────────┐                   │
│         ▼                 ▼                  ▼                  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────┐           │
│  │KnowledgeService│ │QuestionService│ │  ChatModel  │           │
│  │  官方知识检索 │ │  问答数据检索 │ │ DashScope/Mock │           │
│  └──────┬───────┘ └──────┬───────┘ └────────────────┘           │
│         └─────────┬──────┘                                      │
│                   ▼                                             │
│  ┌─────────────────────────────────────┐                        │
│  │         SimpleVectorStore            │                       │
│  │  ONNX Embedding (all-MiniLM-L6-v2)  │                        │
│  └─────────────────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### 问答流程

```
用户输入问题 ──▶ 语义向量化 ──▶ 并行检索知识库 & 问答库
                                        │
                            ┌───────────┴───────────┐
                            ▼                       ▼
                     官方知识匹配              学生问答匹配
                            │                       │
                            └───────────┬───────────┘
                                        ▼
                              构造证据上下文 (Context)
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                               ▼
              ChatModel 可用？                    Mock ChatModel
              调用大模型生成                    本地模板回复
                        │                               │
                        └───────────────┬───────────────┘
                                        ▼
                        返回结构化回答 + 引用来源
```

---

## 📁 项目结构

```
RAG-WeChat/
├── campus-qa-backend/                   # 后端服务
│   ├── pom.xml                          # Maven 依赖配置
│   ├── data/vector-store/               # 向量索引持久化目录（运行时生成）
│   │   ├── knowledge-vector-store.json
│   │   └── question-vector-store.json
│   └── src/main/
│       ├── java/com/campusqa/demo/
│       │   ├── CampusQaApplication.java # 启动入口
│       │   ├── config/                  # 配置类（CORS、Embedding、Mock 等）
│       │   ├── controller/              # REST 控制器
│       │   ├── dto/                     # 数据传输对象
│       │   ├── exception/               # 异常处理
│       │   ├── model/                   # 数据模型
│       │   ├── service/                 # 业务逻辑（AI、知识库、问答）
│       │   └── support/                 # 辅助工具类 
│       └── resources/
│           ├── application.yml          # 应用配置
│           ├── knowledge-data.json      # 校园知识数据源
│           └── models/all-MiniLM-L6-v2/ # 本地 ONNX 模型
│               ├── model.onnx
│               └── tokenizer.json
│
├── campus-qa-mini/                      # 微信小程序前端
│   ├── app.js / app.json / app.wxss     # 小程序入口与全局样式
│   ├── custom-tab-bar/                  # 自定义底部导航栏
│   ├── mock/                            # 本地 Mock 数据
│   ├── pages/
│   │   ├── login/                       # 登录页
│   │   ├── index/                       # 首页 · 问题列表
│   │   ├── ai/                          # AI 助手 · 智能对话
│   │   ├── ask/                         # 发布提问
│   │   ├── detail/                      # 问题详情
│   │   └── profile/                     # 个人中心
│   └── utils/                           # 工具函数
│
└── README.md
```

---

## 🛠️ 技术栈

### 后端

| 类别 | 技术 |
|------|------|
| 语言 & 运行时 | Java 21（启用虚拟线程） |
| 框架 | Spring Boot 3.3.6 |
| AI 框架 | Spring AI 1.0.0-M6 |
| 大模型接口 | 阿里云百炼 DashScope（OpenAI 兼容协议） |
| 默认 Chat 模型 | `qwen-plus` |
| 默认 Embedding 模型 | `text-embedding-v4`（远程）/ `all-MiniLM-L6-v2`（本地 ONNX） |
| 向量存储 | `SimpleVectorStore`（本地持久化） |
| 流式输出 | `SseEmitter` |
| 构建工具 | Maven |

### 前端

| 类别 | 技术 |
|------|------|
| 平台 | 微信小程序（原生开发） |
| 视图层 | WXML / WXSS |
| 逻辑层 | JavaScript |
| 导航 | 自定义 TabBar |
| 流式解析 | `enableChunked` 分块响应 |
| 数据缓存 | `wx.setStorageSync` 本地缓存 |

---

## 🚀 快速启动

### 环境要求

| 工具 | 版本要求 |
|------|---------|
| JDK | 21+ |
| Maven | 3.9+ |
| 微信开发者工具 | 最新稳定版 |
| 操作系统 | Windows / macOS / Linux |

### 1️⃣ 启动后端

```bash
# 进入后端目录
cd campus-qa-backend

# 编译项目
mvn -DskipTests compile

# 启动服务
mvn spring-boot:run
```

> **✅ 启动成功标志**：控制台输出 `Tomcat started on port 8082`

默认模式下无需配置任何 API Key，系统自动使用本地 ONNX Embedding + Mock ChatModel。

### 2️⃣ 启动小程序

1. 打开 **微信开发者工具**
2. 导入 `campus-qa-mini` 目录
3. 使用测试号或自己的 AppID
4. 编译运行即可

> [!TIP]
> 小程序默认连接 `http://localhost:8081/api`，可在 `app.js` 的 `globalData.apiBaseUrl` 中修改。
> 若使用真机调试，需改为电脑局域网 IP。

### 3️⃣ 接入大模型（可选）

如需接入阿里云百炼（DashScope）大模型，设置以下环境变量：

```bash
# 必填 — DashScope API Key
export DASHSCOPE_API_KEY=your_api_key

# 可选 — 自定义模型（默认值已在 application.yml 中配置）
export DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export DASHSCOPE_EMBEDDING_MODEL=text-embedding-v4
export DASHSCOPE_CHAT_MODEL=qwen-plus
```

> [!NOTE]
> 未设置 `DASHSCOPE_API_KEY` 时，系统自动降级为本地模式，不影响启动和演示。

---

## 📡 API 接口文档

基础路径：`/api`

### 元数据

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/meta` | 获取分类和标签列表 |

<details>
<summary>响应示例</summary>

```json
{
  "categories": ["食堂餐饮", "图书馆", "快递服务"],
  "tags": ["营业时间", "位置导航", "借阅规则"]
}
```
</details>

### 问题管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/questions` | 查询问题列表（支持 `keyword`、`category` 筛选） |
| `GET` | `/api/questions/{id}` | 获取问题详情 |
| `POST` | `/api/questions` | 创建问题 |
| `DELETE` | `/api/questions/{id}` | 删除问题 |

<details>
<summary>创建问题 · 请求体示例</summary>

```json
{
  "title": "图书馆周末几点闭馆？",
  "content": "想问一下周末图书馆开放到几点。",
  "category": "图书馆",
  "tags": ["借阅规则", "自习室"],
  "author": "20220001",
  "authorName": "张同学"
}
```
</details>

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/knowledge` | 查询知识文档（支持 `keyword`、`category` 筛选） |
| `GET` | `/api/knowledge/{id}` | 获取知识文档详情 |

### AI 问答

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/ai/answer` | 同步 AI 回答 |
| `POST` | `/api/ai/stream` | SSE 流式 AI 回答（`text/event-stream`） |

<details>
<summary>同步回答 · 请求与响应示例</summary>

**请求体：**
```json
{
  "question": "图书馆周末几点闭馆？"
}
```

**响应体：**
```json
{
  "answer": "[本地模式] 检索到信息：图书馆周末通常 22:00 闭馆。",
  "refs": ["图书馆开放时间通知"],
  "officialRefs": ["图书馆开放时间通知"],
  "studentExperiences": ["王同学：考试周可能延长开放时间。"],
  "matchedQuestions": [
    {
      "id": "q1001",
      "title": "图书馆晚上几点闭馆？",
      "category": "图书馆"
    }
  ],
  "remote": false,
  "mode": "local"
}
```
</details>

<details>
<summary>流式回答 · SSE 事件说明</summary>

| 事件类型 | 说明 |
|---------|------|
| `meta` | 回答元数据（引用来源、匹配问题列表等） |
| `delta` | 增量文本片段 |
| `done` | 回答完成信号 |
</details>

---

## 🛡️ 异常与降级策略

系统实现了多层防御机制，确保在各种异常场景下都能返回有意义的响应：

```
ChatModel 不可用     --->   自动切换 Mock ChatModel（本地模板回复）
向量检索失败          --->   退化为关键词匹配
Embedding 异常        --->  保留基本检索能力
AI 生成失败           --->   返回友好文案
SSE 连接异常          --->   回退本地流式文本
未捕获异常            --->   全局异常处理 → 标准 JSON 错误结构
```

---

## 📊 数据模型

### 知识文档（knowledge-data.json）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 唯一标识 |
| `title` | String | 标题 |
| `category` | String | 分类 |
| `department` | String | 发布部门 |
| `summary` | String | 摘要 |
| `content` | String | 正文内容 |
| `tags` | String[] | 标签 |
| `sourceName` | String | 来源名称 |
| `sourceUrl` | String | 来源链接 |
| `publishDate` | String | 发布日期 |
| `campus` | String | 校区 |
| `official` | Boolean | 是否官方 |

### 问答数据

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 唯一标识 |
| `title` | String | 问题标题 |
| `content` | String | 问题内容 |
| `category` | String | 分类 |
| `tags` | String[] | 标签 |
| `author` / `authorName` | String | 作者信息 |
| `createdAt` | String | 创建时间 |
| `views` / `hot` | Number | 浏览量 / 热度 |
| `aiAnswer` | Object | AI 回答 |
| `answers` | Object[] | 用户回答列表 |

---

## ⚙️ 工程设计亮点

### 本地 ONNX Embedding

模型文件已内置于 `src/main/resources/models/all-MiniLM-L6-v2/`：

- ✅ 不依赖运行时联网下载
- ✅ 适合实验室、宿舍、答辩机等离线环境
- ✅ 部署行为稳定可控

### 向量索引持久化

索引文件位于 `campus-qa-backend/data/vector-store/`：

- **首次启动**：读取知识数据 → 生成向量 → 写入索引文件
- **后续启动**：校验 SHA256 签名 → 签名一致则直接加载，跳过重建
- **效果**：显著减少重复 Embedding 的启动耗时

### Java 21 虚拟线程

通过 `spring.threads.virtual.enabled=true` 启用，提升高并发场景下的吞吐量。

### AI 回答结构化

回答分为三个层次：
1. **直接结论** — 针对问题的简明回答
2. **官方依据** — 来自知识库的权威引用
3. **学生经验** — 来自问答社区的补充信息

---

## ⚠️ 已知限制

| 限制项 | 说明 |
|-------|------|
| 身份校验 | 问题删除使用轻量级 author 校验，不适合生产环境 |
| 数据存储 | 使用静态 JSON 文件和内存数据，未接入数据库 |
| 鉴权体系 | 未接入正式身份认证与权限系统 |
| 后端地址 | 小程序默认连接 `localhost`，真机需要手动修改 |

---

## 🗺️ 后续扩展方向

- [ ] 接入 MySQL / PostgreSQL 持久化存储
- [ ] 接入 Redis 缓存热点数据
- [ ] 实现正式身份认证与权限管理
- [ ] 开发后台管理端
- [ ] 支持文档批量导入与自动切片
- [ ] 向量数据库替换为 Milvus / pgvector / Elasticsearch
- [ ] 支持多模型动态切换
- [ ] AI 回答引用跳转与来源卡片

---

## 🙏 致谢

本项目使用了以下开源组件与平台能力：

- [Spring Boot](https://spring.io/projects/spring-boot) — Web 应用框架
- [Spring AI](https://docs.spring.io/spring-ai/reference/) — AI 集成框架
- [DJL / Transformers](https://djl.ai/) — 深度学习推理
- [ONNX Runtime](https://onnxruntime.ai/) — 本地模型推理引擎
- [阿里云百炼 DashScope](https://dashscope.aliyuncs.com/) — 大模型 API 服务
- [微信小程序](https://developers.weixin.qq.com/miniprogram/) — 前端平台

---

## 📄 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
