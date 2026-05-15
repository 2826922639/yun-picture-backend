# 基于LLM与YOLO情感识别的交互式智能协同云图库系统

> **Interactive Intelligent Collaborative Cloud Image Library Based on LLM and YOLO Emotion Recognition**

## 📖 项目简介

本项目是一个**面向多场景的智能云图库平台**，融合了**大语言模型（LLM）**、**YOLO 面部情感识别**、**RAG 知识增强检索**、**AI 零代码应用生成**等前沿技术。系统不仅提供传统图库的存储、管理和协作功能，更创新性地实现了**基于面部情绪识别的智能配图推荐**、**多模态 AI 对话**、**自然语言驱动的零代码网站生成**等特色能力。

**技术栈概览：**

| 层级 | 技术栈 |
|------|--------|
| **前端** | Vue 3 + TypeScript + Vite 6, Vant 4 (移动端), Ant Design Vue 4 (桌面端), ONNX Runtime Web, ECharts |
| **后端** | Spring Boot 3.5.7 + Java 21, MyBatis-Plus 3.5.15, LangChain4j 1.9.1, ShardingSphere JDBC 5.2.0 |
| **数据库** | MySQL 8.0 + Redis (会话/缓存/限流/聊天记忆) |
| **AI 模型** | DeepSeek (通用对话), Qwen-Coder-Turbo (代码生成), Qwen3-VL-Flash (图像理解), Qwen3-Coder-Plus (推理) |
| **对象存储** | 腾讯云 COS |
| **前端AI推理** | ONNX Runtime Web (YOLOv11n 面部检测 + HSEmotion 情感分类) |

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           前端 (Vue 3 + TypeScript)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐ │
│  │ 图片管理  │ │ AI 对话  │ │ 零代码生成│ │ 空间协作  │ │ 客户端AI推理  │ │
│  │ 上传/搜索 │ │ RAG/搜图 │ │ Vue/HTML  │ │ WebSocket│ │ YOLO+情感ONNX │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────────────┘ │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ HTTP/SSE/WebSocket
┌──────────────────────────────┴──────────────────────────────────────────┐
│                       后端 (Spring Boot 3.5.7 + Java 21)                 │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────────────────┐ │
│  │ 用户/空间  │ │  图片服务  │ │  AI 编排层 │ │    基础设施             │ │
│  │ 权限管理   │ │  上传/搜索 │ │  LangChain │ │  限流/监控/分片/缓存    │ │
│  └────────────┘ └────────────┘ └────────────┘ └────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
   ┌──────────┐         ┌──────────┐          ┌──────────────┐
   │  MySQL   │         │  Redis   │          │  外部 AI API  │
   │ 分库分表  │         │ 会话/缓存 │          │ DeepSeek/Qwen│
   └──────────┘         └──────────┘          └──────────────┘
```

---

## 🚀 创新功能一：前端情绪识别 + 后端动态配图

### 1.1 功能概述

用户在对话界面开启摄像头后，系统在**浏览器端**运行 YOLOv11n 面部检测 + HSEmotion 情感分类模型，实时识别用户当前情绪（开心、悲伤、惊讶等 8 种），并自动触发后端 AI 根据情绪颜色映射**从图库中检索匹配氛围的图片**推荐给用户。

### 1.2 前端：浏览器端 ONNX 推理 (`src/components/model/useEmotionDetection.ts`)

**双模型管线：**

```
摄像头视频流 (getUserMedia)
        │
        ▼
┌──────────────────────────┐
│  YOLOv11n 面部检测       │  输入: 640×640  输出: 8400个锚点
│  模型: model.onnx        │  后处理: 置信度>0.5, 解码边界框
└──────────┬───────────────┘
           │ 人脸ROI
           ▼
┌──────────────────────────┐
│  HSEmotion 情感分类      │  输入: 224×224  输出: 8类情感
│  模型: enet_b0_8_best    │  标准化: ImageNet mean/std
│  _vgaf.onnx              │  Softmax → argmax
└──────────┬───────────────┘
           │
           ▼
     情感标签 (愤怒/轻蔑/厌恶/恐惧/开心/平静/悲伤/惊讶)
```

**关键技术细节：**

- **IndexedDB 模型缓存**：ONNX 模型文件首次加载后存入 IndexedDB，后续启动直接从本地读取，避免重复下载
- **共享内存缓冲区**：预分配 `Float32Array` 避免 GC 抖动，保障实时推理性能
- **平滑策略**：置信度 > 0.85 立即接受；否则在 60 帧内追踪最佳结果，10 秒超时
- **人脸区域扩展**：检测到的人脸区域向外扩展 25%，覆盖额头和下巴以提高情感识别准确率

### 1.3 后端：情绪-颜色映射 + 数据库颜色搜索 (`MoodPictureSearchTool.java`)

**情绪 → 颜色映射表：**

| 情绪 | 目标颜色 | RGB |
|------|---------|-----|
| 开心 (happy) | 暖橙色 | (96, 32, 0) |
| 平静 (neutral) | 深棕色 | (81, 59, 43) |
| 惊讶 (surprise) | 灰褐色 | (110, 91, 72) |
| 悲伤 (sad) | 浅灰色 | (224, 224, 224) |
| 厌恶 (disgust) | 深灰色 | (65, 56, 57) |
| 愤怒 (angry) | 深黑色 | (32, 0, 0) |
| 恐惧 (fear) | 纯黑色 | (0, 0, 0) |
| 轻蔑 (contempt) | 纯白色 | (255, 255, 255) |

**核心 SQL：基于 RGB 欧几里得距离排序**

```sql
SELECT * FROM picture
WHERE picColor LIKE '0x%' AND LENGTH(picColor) = 8 AND spaceId = 0
ORDER BY POW(CONV(SUBSTR(picColor,3,2),16,10) - R, 2)
       + POW(CONV(SUBSTR(picColor,5,2),16,10) - G, 2)
       + POW(CONV(SUBSTR(picColor,7,2),16,10) - B, 2)
LIMIT 5
```

对匹配到的图片，进一步调用 **Qwen3-VL-Flash 视觉模型**生成一句话的氛围描述，最终返回包含图片 URL 和 AI 理解文本的 JSON 结果。

---

## 🚀 创新功能二：AI RAG 知识增强问答

### 2.1 功能概述

系统内置 RAG (Retrieval-Augmented Generation) 引擎，支持基于本地知识库的智能问答。用户上传的文档（`.txt`）会被向量化存储，AI 回答问题时自动检索相关文档片段作为上下文，大幅提升回答的专业性和准确性。

### 2.2 技术实现 (`AiChatWithRAGServiceFactory.java`)

**RAG 数据流：**

```
用户提问
    │
    ▼
┌──────────────────────┐
│ 1. 文档加载与嵌入     │  FileSystemDocumentLoader 从 documents/ 目录加载 .txt
│                      │  InMemoryEmbeddingStore 存储文档向量
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ 2. 内容检索          │  EmbeddingStoreContentRetriever 语义相似度检索
│                      │  返回最相关的文档片段
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ 3. 上下文增强生成     │  检索结果 + 用户问题 → LLM 生成回答
│                      │  Redis 持久化对话记忆 (最多50条)
└──────┬───────────────┘
       │
       ▼
   SSE 流式返回
```

**关键技术细节：**

| 层面 | 技术方案 |
|------|---------|
| **文档加载** | `FileSystemDocumentLoader` 扫描 `documents/` 目录下的 `.txt` 文件 |
| **向量嵌入** | `InMemoryEmbeddingStore<TextSegment>` 内存级向量存储 |
| **内容检索** | `EmbeddingStoreContentRetriever` 语义相似度检索 |
| **对话记忆** | `MessageWindowChatMemory`(最多50条) + `RedisChatMemoryStore` 持久化 |
| **历史恢复** | 每次会话从 MySQL `chat_history` 表加载最近 20 条消息 |
| **安全护栏** | `PromptSafetyInputGuardrail` — 检测提示注入、敏感词、超长输入 |
| **服务缓存** | Caffeine (1000条, 30分钟写入过期, 10分钟访问过期) |
| **流式输出** | Project Reactor `Flux<String>` → SSE (Server-Sent Events) |
| **AI模型** | DeepSeek (通用对话) |

**对话记忆架构：**

```
┌─────────────┐    ┌──────────────┐    ┌──────────────────┐
│ 当前会话     │───▶│ Redis 持久化  │◀───│ MySQL 持久化     │
│ (内存)       │    │ (跨进程共享)  │    │ chat_history 表  │
│ 最多50条消息  │    │ TTL: 3600秒  │    │ (永久归档)       │
└─────────────┘    └──────────────┘    └──────────────────┘
```

---

## 🚀 创新功能三：AI 零代码应用生成

### 3.1 功能概述

用户只需**输入自然语言描述 + 上传参考图片**，系统即可自动生成完整的 Web 应用。支持三种生成模式，从简单静态页面到完整 Vue 工程，真正实现"零代码"开发。

### 3.2 三种生成模式

| 模式 | CodeGenType | 适用场景 | 输出 |
|------|------------|---------|------|
| **原生 HTML 模式** | `HTML` | 简单静态页面 | 单个 `index.html` (内联 CSS/JS) |
| **原生多文件模式** | `MULTI_FILE` | 多页面简单交互 | `index.html` + `style.css` + `script.js` |
| **Vue 工程模式** | `VUE_PROJECT` | 复杂多页面应用 | 完整 Vite + Vue 3 项目 (含路由) |

### 3.3 智能路由：自动选择生成策略 (`AiCodeGenTypeRoutingService`)

用户提交需求后，系统先用轻量级 AI（`qwen-turbo`, 100 tokens）对需求进行分类，自动判断应该生成哪种类型的应用：

```
用户输入: "帮我做一个电商首页"
    │
    ▼
┌──────────────────────────┐
│ AiCodeGenTypeRoutingService │  (qwen-turbo, max_tokens=100)
│                          │
│ HTML: 简单静态页面        │
│ MULTI_FILE: 多页面简单交互│
│ VUE_PROJECT: 复杂多页面   │
└──────────┬───────────────┘
           │ → VUE_PROJECT
           ▼
    开始代码生成
```

### 3.4 完整代码生成流水线 (`AiCodeGeneratorFacade.java`)

```
用户消息 + 参考图片URL
        │
        ▼
┌─────────────────────────────────┐
│ Step 1: 图片理解预处理 (带缓存)  │  Qwen3-VL-Flash 逐张描述图片内容
│  "这是一张电商Banner图..."       │  结果缓存到 Caffeine (按appId)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Step 2: AI 代码生成 (流式)       │  HTML/MULTI_FILE: qwen-coder-turbo
│  系统提示词注入:                  │  VUE_PROJECT: qwen3-coder-plus + 工具调用
│  - ImageUrls                    │  支持 6 种文件操作工具
│  - generateImageUnderstand      │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Step 3: 代码解析                 │  CodeParserExecutor
│  HTML: 正则提取 ```html 代码块   │  MultiFileCodeParser
│  MULTI_FILE: 分别提取 HTML/CSS/JS│  HtmlCodeParser
│  VUE_PROJECT: AI直接写文件       │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Step 4: 磁盘持久化               │  CodeFileSaverExecutor
│  输出到 tmp/code_output/          │  模板方法模式:
│  {type}_{appId}/                 │  HtmlCodeFileSaverTemplate
│                                  │  MultiFileCodeFileSaverTemplate
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Step 5: 项目构建 (仅Vue)         │  VueProjectBuilder
│  npm install (300s超时)          │  失败自动修复: 将错误信息反馈给AI
│  npm run build (180s超时)        │  AI重新生成 package.json 配置
│  验证 dist/ 目录生成              │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Step 6: 部署                     │
│  复制到部署目录                   │  AppController.deployApp
│  生成6位部署密钥                  │  → 可公开访问的URL
│  异步生成应用截图                 │  → Selenium截图 + COS上传
└─────────────────────────────────┘
```

### 3.5 AI 工具调用系统 (Vue 工程模式专属)

Vue 工程模式下，AI 不只是输出代码，而是像一个**真实的开发者**一样使用文件操作工具：

| 工具 | 名称 | 功能 |
|------|------|------|
| `FileWriteTool` | 文件写入 | 创建新文件并写入内容 |
| `FileReadTool` | 文件读取 | 读取已有文件内容 |
| `FileModifyTool` | 文件修改 | 精确替换文件中的指定内容 |
| `FileDeleteTool` | 文件删除 | 删除指定文件 |
| `FileDirReadTool` | 目录读取 | 列出目录结构 |
| `WebCrawlerSearchTool` | 联网搜索 | 百度搜索获取参考资料 |
| `ExitTool` | 退出 | 标记项目完成 |

**工具调用消息流 (SSE)：**

```
ToolRequestMessage  →  {"type":"tool_request","id":"1","name":"FileWriteTool","arguments":{...}}
ToolExecutedMessage →  {"type":"tool_executed","id":"1","result":"success"}
AiResponseMessage   →  {"type":"ai_response","data":"已创建 src/App.vue..."}
```

前端实时展示 AI 的操作过程（选择工具 → 执行 → 结果），用户可以看到 AI 一步步构建应用的完整过程。

### 3.6 构建失败自修复机制

```
npm run build 失败
    │
    ▼
捕获错误输出 (stderr)
    │
    ▼
将错误信息作为新消息发送给 AI:
"构建失败了，错误如下: {error}，请修复 package.json"
    │
    ▼
AI 使用 FileModifyTool 更新配置文件
    │
    ▼
重新 npm run build → 成功 ✅
```

### 3.7 可视化编辑器 (`visualEditor.ts`)

生成的 Web 应用支持**可视化编辑**：启用编辑模式后，iframe 内注入编辑脚本，用户可以点击选中任意页面元素，获取其 CSS 选择器，然后在对话中描述修改需求（如"把这个按钮改成红色"），AI 精确定位并修改对应元素。

---

## 📦 模块详解

### 图片管理模块

| 功能 | 技术实现 |
|------|---------|
| **文件上传** | 支持本地上传 (`MultipartFile`) 和 URL 上传，统一上传至腾讯云 COS |
| **缩略图处理** | COS 万象图片处理：>20KB 自动生成 256×256 WebP 缩略图 |
| **批量抓取** | Jsoup 爬取 Bing 图片搜索结果，支持按关键词批量入库 |
| **颜色搜索** | 基于 RGB 欧几里得距离的图片主色调相似度检索 |
| **以图搜图** | 对接外部以图搜图 API，返回相似图片和来源链接 |
| **图片审核** | 三级审核状态：待审核(0) → 通过(1) / 拒绝(2)，管理员审核 |
| **AI 扩图** | 对接阿里云 AI 扩图 API，支持画布扩展，轮询任务状态 |
| **超分辨率** | 浏览器端 ONNX 模型 (4× 放大)，256→1024 超分重建 |
| **图片裁剪** | VueCropper 组件，内置一寸/两寸等证件照尺寸预设 |
| **实时协作编辑** | WebSocket + Disruptor 环形缓冲区，图片编辑锁互斥控制 |

### 空间管理模块

| 功能 | 技术实现 |
|------|---------|
| **空间类型** | 私有空间 (个人) + 团队空间 (多人协作) |
| **空间等级** | 普通(100张/100MB) / 专业(1000张/1GB) / 旗舰(10000张/10GB) |
| **权限模型** | 基于 Sa-Token 的细粒度权限：上传/删除/编辑/查看/成员管理 |
| **团队角色** | Viewer（只读）/ Editor（编辑）/ Admin（完全控制） |
| **空间分析** | 使用量/分类/标签/大小分布/用户行为/排名 6 维度 ECharts 可视化 |
| **应用限额** | 按空间等级限制生成应用数量 (5/10/100) 和对话次数 (5/9/100) |

### AI 对话模块

| 功能 | 技术实现 |
|------|---------|
| **三种对话模式** | 图片上下文对话 / RAG 知识问答 / 情绪搜图 |
| **流式响应** | SSE (Server-Sent Events)，实时展示 AI 逐字输出 |
| **会话管理** | 按模式分组的会话列表，支持历史回溯和删除 |
| **Markdown 渲染** | markdown-it + highlight.js 代码高亮，自定义图片链接处理 |
| **多模态输入** | 支持图片作为对话上下文，AI 基于图片内容回答 |
| **监控指标** | Micrometer → Prometheus：请求计数/Token用量/响应时间/错误率 |

### 用户与安全模块

| 功能 | 技术实现 |
|------|---------|
| **认证** | Session + Cookie + Sa-Token 双认证体系 |
| **密码加密** | MD5 + 固定盐值 |
| **角色管理** | 普通用户 / 管理员 |
| **限流保护** | Redisson RRateLimiter + AOP：支持全局限流/用户限流/IP限流 |
| **安全护栏** | 输入检测：提示注入、敏感词、超长输入、正则匹配 |
| **CORS** | 全局跨域配置 |

---

## 📂 项目结构

```
毕业设计/
├── 前端/                              # Vue 3 前端项目
│   ├── src/
│   │   ├── api/                       # API 接口层 (10个控制器模块)
│   │   │   ├── aiChatController.ts    # AI 对话 API (SSE)
│   │   │   ├── appController.ts       # 应用 CRUD + 代码生成 API
│   │   │   ├── pictureController.ts   # 图片上传/搜索/编辑 API
│   │   │   ├── spaceController.ts     # 空间管理 API
│   │   │   └── ...
│   │   ├── components/
│   │   │   ├── AiChatDrawer.vue       # AI 对话抽屉组件
│   │   │   ├── ImageCropper.vue       # 图片裁剪 + 协作编辑
│   │   │   ├── ImageOutPainting.vue   # AI 扩图组件
│   │   │   ├── model/
│   │   │   │   ├── SuperResolveDrawer.vue    # 超分辨率 (ONNX)
│   │   │   │   └── useEmotionDetection.ts   # 情感检测 Composable
│   │   │   ├── PictureUpload.vue      # 图片上传
│   │   │   └── ...
│   │   ├── pages/
│   │   │   ├── AiChatPage.vue         # AI 对话主页 (情绪/搜图/RAG)
│   │   │   ├── app/
│   │   │   │   ├── AppGenFromPicturesPage.vue  # 零代码应用生成
│   │   │   │   ├── AppEditPage.vue
│   │   │   │   └── AppPage.vue
│   │   │   ├── ImageEditPage.vue      # 图片编辑 (移动端)
│   │   │   ├── SpaceDetailPage.vue    # 空间详情
│   │   │   ├── SpaceAnalyzePage.vue   # 空间分析
│   │   │   └── ...
│   │   ├── utils/
│   │   │   ├── PictureEditWebSocket.ts  # WebSocket 协同编辑
│   │   │   ├── visualEditor.ts          # 可视化编辑器
│   │   │   └── codeGenTypes.ts
│   │   ├── router/index.ts           # 路由 (17条路由)
│   │   └── stores/useLoginUserStore.ts # Pinia 状态管理
│   └── package.json
│
├── 后端/                              # Spring Boot 后端项目
│   ├── src/main/java/com/hhh/yunpicturebackend/
│   │   ├── ai/                        # AI 子系统 ⭐
│   │   │   ├── core/
│   │   │   │   ├── AiChatFacade.java          # AI 对话编排器
│   │   │   │   ├── AiCodeGeneratorFacade.java  # 代码生成编排器
│   │   │   │   ├── builder/VueProjectBuilder.java
│   │   │   │   ├── parser/            # 代码解析器 (策略模式)
│   │   │   │   │   ├── HtmlCodeParser.java
│   │   │   │   │   ├── MultiFileCodeParser.java
│   │   │   │   │   └── CodeParserExecutor.java
│   │   │   │   ├── saver/             # 代码持久化 (模板方法模式)
│   │   │   │   │   ├── HtmlCodeFileSaverTemplate.java
│   │   │   │   │   ├── MultiFileCodeFileSaverTemplate.java
│   │   │   │   │   └── CodeFileSaverExecutor.java
│   │   │   │   └── handler/           # SSE 流处理器
│   │   │   ├── tools/                 # AI 工具集
│   │   │   │   ├── MoodPictureSearchTool.java   # 情绪搜图工具
│   │   │   │   ├── WebCrawlerSearchTool.java    # 联网搜索工具
│   │   │   │   ├── FileWriteTool.java           # 文件写入工具
│   │   │   │   ├── FileReadTool.java
│   │   │   │   ├── FileModifyTool.java
│   │   │   │   ├── FileDeleteTool.java
│   │   │   │   └── ToolManager.java
│   │   │   ├── guardrail/             # 安全护栏
│   │   │   │   └── PromptSafetyInputGuardrail.java
│   │   │   ├── model/message/         # 流消息模型
│   │   │   └── enums/                 # 枚举定义
│   │   ├── controller/                # REST 控制器 (10个)
│   │   ├── service/                   # 业务服务层
│   │   ├── manager/                   # 管理器层
│   │   │   ├── CosManager.java        # 腾讯云COS对象存储
│   │   │   ├── auth/                  # 空间权限管理
│   │   │   ├── sharding/              # 数据库分片
│   │   │   ├── upload/                # 文件上传 (模板方法)
│   │   │   └── websockert/            # WebSocket + Disruptor
│   │   ├── model/                     # 实体/DTO/VO/枚举
│   │   ├── monitor/                   # AI模型监控 (Prometheus)
│   │   ├── ratelimter/                # 限流 (Redisson + AOP)
│   │   └── config/                    # 配置类
│   ├── src/main/resources/
│   │   ├── prompt/                    # AI 系统提示词
│   │   │   ├── mood-system-prompt.txt
│   │   │   ├── image-system-prompt.txt
│   │   │   ├── image-understand-prompt.txt
│   │   │   ├── codegen-html-system-prompt.txt
│   │   │   ├── codegen-multi-file-system-prompt.txt
│   │   │   ├── codegen-vue-project-system-prompt.txt
│   │   │   └── codegen-routing-system-prompt.txt
│   │   └── documents/                 # RAG 知识库文档
│   └── sql/creat_table.sql            # 数据库建表脚本
```

---

## 🔧 关键设计模式

| 设计模式 | 应用场景 | 位置 |
|---------|---------|------|
| **策略模式** | 代码解析器 (HTML/MultiFile) | `parser/CodeParserExecutor.java` |
| **模板方法模式** | 代码文件保存 (HTML/MultiFile) | `saver/CodeFileSaverTemplate.java` |
| **工厂模式** | AI 服务创建 (RAG/Image/Search) | `ai/*Factory.java` |
| **外观模式** | AI 对话统一入口 | `AiChatFacade.java` |
| **建造者模式** | Vue 项目构建 | `builder/VueProjectBuilder.java` |
| **观察者模式** | AI 模型监控 | `monitor/AiModelMonitorListener.java` |
| **命令模式** | AI 工具调用 | `ai/tools/` |
| **生产者-消费者模式** | Disruptor 环形缓冲区 | `websockert/disruptor/` |

---

## 🎯 技术亮点总结

1. **浏览器端 AI 推理**：YOLOv11n + HSEmotion 双模型在浏览器运行，IndexedDB 缓存模型文件，零服务端推理成本
2. **情绪-色彩检索链路**：前端情感识别 → 后端颜色映射 → SQL RGB 欧几里得距离排序 → AI 图像理解描述，端到端闭环
3. **RAG 知识增强**：文档向量化 + 语义检索 + Redis 记忆持久化 + 对话历史恢复，完整的 RAG 技术栈
4. **AI 零代码生成**：自然语言 → 需求分类 → 图片理解 → 代码生成 → 解析 → 保存 → 构建 → 部署 → 截图，全自动化流水线
5. **AI 工具调用**：Vue 工程模式下 AI 像开发者一样使用文件读写/修改/删除工具，支持构建失败后 AI 自动修复
6. **可视化编辑器**：iframe 注入编辑脚本，元素选择 → CSS 选择器定位 → AI 精准修改
7. **实时协同编辑**：WebSocket + Disruptor (262K 环形缓冲区)，编辑锁互斥，操作实时广播
8. **多模型路由**：根据任务复杂度智能分配不同 AI 模型（轻量分类/代码生成/多模态理解/深度推理）
9. **分层缓存**：Caffeine 本地缓存 + Redis 分布式缓存的二级缓存策略
10. **可观测性**：AI 调用全链路 Prometheus 指标监控（请求/Token/耗时/错误）

---

## 🚀 快速启动

### 环境要求

- **前端**: Node.js 18+, npm
- **后端**: JDK 21, Maven 3.8+, MySQL 8.0+, Redis 7.0+
- **AI 模型文件** (放在前端 `public/` 目录):
  - `model.onnx` (YOLOv11n 面部检测)
  - `enet_b0_8_best_vgaf.onnx` (HSEmotion 情感分类)
  - `1.onnx` (超分辨率模型)

### 后端启动

```bash
cd 后端

# 初始化数据库
mysql -u root -p < sql/creat_table.sql

# 配置 application-local.yml 中的数据库密码和 AI API Key

# 启动
mvn spring-boot:run
```

### 前端启动

```bash
cd 前端
npm install
npm run dev
```

### 访问地址

- 前端: `http://localhost:5173`
- 后端 API: `http://localhost:8123`
- API 文档 (Knife4j): `http://localhost:8123/doc.html`
- Prometheus 指标: `http://localhost:8123/actuator/prometheus`

---

## 📝 许可证

本项目仅供学习和研究使用。
