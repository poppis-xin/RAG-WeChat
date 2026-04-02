# 基于知识检索与大语言模型的校园信息问答系统

## 1. 项目简介

本项目是一个面向校园场景的智能问答系统，采用“知识检索 + 大语言模型生成”的 RAG 架构，目标是为师生提供可追溯、可解释、可演示的校园信息问答能力。

项目由两部分组成：

- `campus-qa-backend`：基于 Java 21、Spring Boot 3.3.6、Spring AI 的后端服务
- `campus-qa-mini`：基于原生微信小程序的前端应用

系统当前支持以下核心能力：

- 官方校园知识文档管理与检索
- 学生问答列表、详情、发布、删除
- 基于向量检索的 AI 问答
- SSE 流式输出
- 无 API Key 场景下的本地 Mock ChatModel 兜底
- 本地 ONNX Embedding 模型离线运行
- 向量索引本地持久化，减少重复 Embedding 和冷启动耗时

本项目适合作为：

- 毕业设计 / 课程设计展示项目
- 校园知识问答系统原型
- Spring AI + RAG + 微信小程序的整合示例

---

## 2. 系统目标

传统校园信息查询存在几个典型问题：

- 信息分散在教务处、图书馆、后勤、宿管等不同渠道
- 通知、制度、学生经验混杂，难以判断权威性
- 普通搜索只能做关键词匹配，不能理解提问语义

本系统希望解决的问题是：

- 让用户可以直接用自然语言提问
- 优先基于官方知识文档进行语义检索
- 将“官方依据”和“学生经验”分开组织
- 在大模型不可用时仍然保证系统可启动、可响应、可演示

---

## 3. 功能概览

### 3.1 后端功能

- 官方知识库加载与查询
- 学生问答列表、详情、创建、删除
- 基于 `SimpleVectorStore` 的向量检索
- 基于本地 ONNX 的 Embedding
- AI 问答同步接口
- AI 问答 SSE 流式接口
- 本地 Mock ChatModel 降级回复
- 向量索引持久化加载与保存
- 全局异常处理与友好错误提示

### 3.2 小程序功能

- 登录页
- 首页问题列表
- 分类筛选与关键词搜索
- 问题详情页
- AI 助手对话页
- 发布问题页
- 个人页
- 本地 Mock 数据兜底

### 3.3 AI 回答特性

- 回答结构分为“直接结论 / 官方依据 / 学生经验”
- 引用匹配到的知识来源与问题列表
- 流式增量输出
- 大模型不可用时自动切换到本地模式
- 检索或 Embedding 异常时仍返回 200，不直接抛 500

---

## 4. 技术栈

### 4.1 后端

- Java 21
- Spring Boot 3.3.6
- Spring AI 1.0.0-M6
- SimpleVectorStore
- TransformersEmbeddingModel
- ONNX 本地 Embedding 模型：`all-MiniLM-L6-v2`
- SSE：`SseEmitter`
- Maven

### 4.2 前端

- 原生微信小程序
- WXML / WXSS / JavaScript
- 自定义 TabBar
- 本地缓存：`wx.setStorageSync`
- 分块流式响应解析：`enableChunked`

---

## 5. 项目结构

```text
基于知识检索与大语言模型的校园信息问答系统设计与实现
├── campus-qa-backend
│   ├── pom.xml
│   ├── data
│   │   └── vector-store
│   │       ├── knowledge-vector-store.json
│   │       ├── knowledge-vector-store.json.sha256
│   │       ├── question-vector-store.json
│   │       └── question-vector-store.json.sha256
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com.campusqa.demo
│   │       │       ├── config
│   │       │       ├── controller
│   │       │       ├── dto
│   │       │       ├── exception
│   │       │       ├── model
│   │       │       ├── service
│   │       │       └── support
│   │       └── resources
│   │           ├── application.yml
│   │           ├── knowledge-data.json
│   │           └── models
│   │               └── all-MiniLM-L6-v2
│   │                   ├── model.onnx
│   │                   └── tokenizer.json
├── campus-qa-mini
│   ├── app.js
│   ├── app.json
│   ├── app.wxss
│   ├── custom-tab-bar
│   ├── mock
│   ├── pages
│   │   ├── login
│   │   ├── index
│   │   ├── ai
│   │   ├── ask
│   │   ├── detail
│   │   └── profile
│   └── utils
└── README.md
```

---

## 6. 系统架构说明

### 6.1 整体架构

```text
微信小程序
    ↓
Spring Boot REST / SSE
    ↓
AiService
    ↓
KnowledgeService / QuestionService
    ↓
SimpleVectorStore + ONNX Embedding
    ↓
知识文档 / 问答数据
```

### 6.2 问答流程

1. 用户在小程序输入问题
2. 后端接收问题后分别检索：
   - 官方知识库
   - 学生问答库
3. 构造证据上下文
4. 若真实 `ChatModel` 可用，则调用大模型生成回答
5. 若真实 `ChatModel` 不可用，则使用本地 Mock ChatModel 返回结果
6. 接口返回结构化回答，并携带来源、学生经验、匹配问题列表

### 6.3 本地模式说明

在没有 `ZHIPUAI_API_KEY` 时，系统仍会正常启动。

此时行为如下：

- `EmbeddingModel` 使用本地 ONNX 模型
- `ChatModel` 使用本地 Mock 实现
- AI 回复会返回形如：

```text
[本地模式] 检索到信息：...
```

这保证了项目在离线、无 Key、演示环境中仍可完整运行。

---

## 7. 后端设计说明

### 7.1 端口策略

后端端口固定为 `8081`。

启动成功后日志会明确打印：

```text
Server is running on 8081
```

### 7.2 Embedding 模型策略

项目已将 ONNX 模型文件内置到资源目录：

- `src/main/resources/models/all-MiniLM-L6-v2/model.onnx`
- `src/main/resources/models/all-MiniLM-L6-v2/tokenizer.json`

优势：

- 不依赖运行时联网下载
- 更适合实验室、宿舍、答辩机等离线环境
- 部署行为更稳定、可控

### 7.3 向量索引持久化

项目使用 `SimpleVectorStore.save/load` 做本地索引持久化，索引文件保存到：

```text
campus-qa-backend/data/vector-store/
```

首次启动：

- 读取知识数据
- 生成向量
- 写入本地向量索引文件

后续启动：

- 优先加载已有向量索引
- 签名一致时不重复全量重建

这能显著减少重复 Embedding 带来的启动耗时。

### 7.4 异常与降级策略

为保证系统稳定性，后端做了多层防御：

- `ChatModel` 缺失时自动走本地 Mock
- 向量检索失败时自动退化为关键词匹配
- Embedding / 向量写入异常时保留基本检索能力
- AI 生成失败时返回本地友好文案
- SSE 异常时回退到本地流式文本
- 全局异常统一封装为标准 JSON 错误结构

---

## 8. 前端设计说明

### 8.1 页面结构

根据 `campus-qa-mini/app.json`，当前页面包括：

- `pages/login/login`：登录页
- `pages/index/index`：首页问题列表
- `pages/ai/ai`：AI 对话页
- `pages/ask/ask`：提问页
- `pages/detail/detail`：问题详情页
- `pages/profile/profile`：个人页

### 8.2 交互特点

- 首页支持关键词搜索与分类筛选
- AI 页支持快捷提问
- AI 回复支持流式显示
- 接口不可用时，小程序可切换到本地演示数据
- 用户数据、问题列表、统计信息会写入本地缓存

### 8.3 后端地址

小程序默认通过 `campus-qa-mini/app.js` 中的配置访问后端：

```js
apiBaseUrl: "http://localhost:8081/api"
```

若使用真机调试，需要将其改为电脑局域网 IP 或可访问地址。

---

## 9. 快速启动

### 9.1 环境要求

- JDK 21
- Maven 3.9+
- 微信开发者工具
- Windows 环境优先（当前已在 Windows + PowerShell 环境完成验证）

### 9.2 启动后端

进入后端目录：

```bash
cd campus-qa-backend
```

编译项目：

```bash
mvn -DskipTests compile
```

启动项目：

```bash
mvn spring-boot:run
```

成功标志：

- 控制台出现 `Tomcat started on port 8081`
- 控制台出现 `Server is running on 8081`

### 9.3 启动小程序

1. 打开微信开发者工具
2. 导入 `campus-qa-mini` 目录
3. 使用测试号或自己的 AppID
4. 编译运行

---

## 10. 大模型配置

### 10.1 本地模式

不配置任何 Key 时，系统默认使用：

- 本地 ONNX Embedding
- 本地 Mock ChatModel

这也是当前推荐的开箱即用模式。

### 10.2 接入智谱 AI

若后续希望接入真实大模型，可配置环境变量：

```env
ZHIPUAI_API_KEY=your_api_key
```

说明：

- 当前工程入口已排除 `ZhiPuAiAutoConfiguration`
- 若要恢复真实智谱模型接入，需要根据项目中的 Profile/配置设计重新启用相关自动配置

---

## 11. 接口说明

当前后端接口前缀统一为：

```text
/api
```

### 11.1 元数据接口

#### `GET /api/meta`

返回分类和标签信息。

响应示例：

```json
{
  "categories": ["食堂餐饮", "图书馆", "快递服务"],
  "tags": ["营业时间", "位置导航", "借阅规则"]
}
```

### 11.2 问题接口

#### `GET /api/questions`

查询问题列表。

参数：

- `keyword`：关键词，可选
- `category`：分类，可选

#### `GET /api/questions/{id}`

获取问题详情。

#### `POST /api/questions`

创建问题。

请求体示例：

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

#### `DELETE /api/questions/{id}`

删除问题。

当前实现使用 `author` 参数做简单校验，仅适合演示环境，不适合生产。

### 11.3 知识库接口

#### `GET /api/knowledge`

查询知识文档列表。

参数：

- `keyword`：关键词，可选
- `category`：分类，可选

#### `GET /api/knowledge/{id}`

获取知识文档详情。

### 11.4 AI 问答接口

#### `POST /api/ai/answer`

同步返回 AI 回答。

请求体：

```json
{
  "question": "图书馆周末几点闭馆？"
}
```

响应体示例：

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

#### `POST /api/ai/stream`

流式返回 AI 回答，响应类型为：

```text
text/event-stream
```

事件内容包含：

- `meta`
- `delta`
- `done`

---

## 12. 数据说明

### 12.1 官方知识数据

官方知识文档来源于：

- `campus-qa-backend/src/main/resources/knowledge-data.json`

单条知识文档字段包括：

- `id`
- `title`
- `category`
- `department`
- `summary`
- `content`
- `tags`
- `sourceName`
- `sourceUrl`
- `publishDate`
- `campus`
- `official`

### 12.2 问答数据

系统内置了示例问答数据，并支持运行时新增问题。

问题字段包括：

- `id`
- `title`
- `content`
- `category`
- `tags`
- `author`
- `authorName`
- `createdAt`
- `views`
- `hot`
- `aiAnswer`
- `answers`

---

## 13. 当前实现中的工程优化

相较于最初的可运行版本，当前工程已经完成以下关键优化：

- 新增本地 Mock ChatModel，解决无 API Key 时启动失败问题
- AI 回答可将检索到的知识片段直接填入本地回复
- 服务端端口固定为 `8081`
- 启动日志明确输出 `Server is running on 8081`
- Embedding 模型改为项目内本地资源加载
- 向量索引改为本地持久化加载
- 检索 / Embedding 异常时优先返回友好结果
- 全局异常增加日志记录
- SSE 执行器增加生命周期释放

---

## 14. 运行产物说明

运行过程中会生成以下文件：

```text
campus-qa-backend/data/vector-store/knowledge-vector-store.json
campus-qa-backend/data/vector-store/knowledge-vector-store.json.sha256
campus-qa-backend/data/vector-store/question-vector-store.json
campus-qa-backend/data/vector-store/question-vector-store.json.sha256
```

说明：

- `json` 文件为持久化的向量索引
- `sha256` 文件为数据签名，用于判断是否需要重建索引

---

## 15. 已知限制

当前版本仍然存在一些面向演示场景的限制：

- 删除问题的身份校验仍是轻量级实现，不适合生产环境
- 当前数据源以静态知识文件和内存问答数据为主
- 未接入数据库
- 未接入正式鉴权体系
- 智谱真实大模型接入仍需按实际部署方式恢复配置
- 小程序默认后端地址为 `localhost`，真机运行时需要修改

---

## 16. 后续可扩展方向

- 接入 MySQL / PostgreSQL
- 接入 Redis 缓存
- 接入真实身份认证与权限系统
- 增加后台管理端
- 支持文档批量导入与自动切片
- 支持向量数据库替换为 Milvus / pgvector / Elasticsearch
- 支持多模型切换
- 为 AI 回答增加引用跳转与来源卡片

---

## 17. 适用场景

本项目适合以下展示或开发场景：

- 毕业设计答辩
- 校园智能问答原型验证
- Java + Spring AI 综合课程项目
- 小程序前后端一体化示例
- 本地离线可演示的 RAG 项目模板

---

## 18. 启动验证结果

当前版本已经完成本地验证：

- 后端可正常启动
- 服务固定运行在 `8081`
- 启动日志可打印 `Server is running on 8081`
- 本地 ONNX Embedding 模型可用
- 知识向量库可持久化并在后续启动时直接加载
- 问答向量库可持久化并在后续启动时直接加载
- `/api/meta` 接口可正常返回 `200 OK`

---

## 19. 致谢

本项目使用了以下关键开源组件与平台能力：

- Spring Boot
- Spring AI
- DJL / Transformers
- ONNX Runtime
- 微信小程序

---

## 20. 说明

本 README 以当前代码仓库中的实际实现为准，已经同步反映：

- 本地 Mock ChatModel 方案
- 本地 ONNX Embedding 模型
- 向量索引本地持久化
- 固定端口 `8081`
- 当前接口与页面结构

