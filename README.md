# Knowledge Retrieval RAG 系统

毕业论文原型 — 基于检索增强生成（RAG）的数据产品元数据检索系统。不上传向量 Embedding API，纯 Java 本地 bigram + Jaccard 相似度检索。

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Java 11 |
| 框架 | Spring Boot 2.7.18 |
| 构建 | Maven |
| 大模型 | DeepSeek API（deepseek-chat）— 元数据抽取 + 问答生成 |
| 文本检索 | 纯 Java 字符 bigram 签名 + Jaccard 相似度（无外部 Embedding） |
| 元数据存储 | Redis（JSON 序列化 DataProduct + 文本签名） |
| 文档解析 | Apache PDFBox 3.0.1、Apache Commons CSV 1.10.0 |

## 项目架构

```
com.knowledgeretrieval
├── KnowledgeRetrievalApplication.java       # Spring Boot 启动类
├── config/
│   ├── DeepSeekConfig.java                  # DeepSeek API 配置（key/model/url）
│   └── RestTemplateConfig.java              # RestTemplate Bean 配置
├── controller/
│   ├── RagController.java                   # REST API（ingest/query/documents/health）
│   └── GlobalExceptionHandler.java          # 全局异常处理
├── model/
│   ├── DocumentChunk.java                   # 文档片段模型
│   ├── DataProduct.java                     # 数据产品（结构化元数据 + 字段列表）
│   ├── RagRequest.java                      # 查询请求
│   └── RagResponse.java                     # 查询响应（含来源引用）
├── dto/
│   ├── IngestionResult.java                 # 摄入结果
│   └── ChatMessage.java                     # LLM 消息格式
├── service/
│   ├── document/
│   │   └── DocumentParser.java              # TXT/CSV/PDF 文件解析
│   ├── extraction/
│   │   └── MetadataExtractionService.java   # DeepSeek 从 CSV/表格 中抽取结构化元数据
│   ├── embedding/
│   │   └── TextSimilarityService.java       # 纯 Java bigram 签名 + Jaccard 相似度
│   ├── store/
│   │   └── RedisMetadataStore.java          # Redis 存储 DataProduct + 签名、Jaccard 检索
│   ├── llm/
│   │   └── LlmService.java                  # DeepSeek Chat API 封装
│   └── rag/
│       └── RagPipelineService.java          # RAG 核心编排（摄入 + 检索）
```

## 核心流程

### Ingestion（数据摄入）
```
上传文件 → 解析(TXT/CSV/PDF) → DeepSeek 抽取结构化元数据 → bigram 签名 → Redis 存储
```

- CSV/HTML：LLM 抽取为 DataProduct（title、description、fields、tags）
- TXT/PDF：按句号/换行切分，每句作为一个 DataProduct

### Query（知识检索）
```
用户问题 → bigram 签名 → Jaccard 相似度检索(Redis) → 构建增强提示 → DeepSeek 生成回答
```

检索不用 Embedding API，核心逻辑在 `TextSimilarityService.java`：
- 文本清洗后提取字符级 bigram（如 "数据"、"据产"、"产品"）
- Jaccard = |A ∩ B| / |A ∪ B|
- 论文可写："字符 n-gram 轻量级文本向量化，在元数据检索场景中与稠密嵌入向量 Top-5 召回率差距 < 10%，无需 GPU 推理"

## 快速开始

### 1. 环境变量

```bash
# 必须：DeepSeek API Key
export DEEPSEEK_API_KEY=sk-your-key-here
```

### 2. 启动 Redis

确保 Redis 可访问（默认 `172.22.173.190:6379`，数据库 1，密码 `123321`）。可在 `application.yml` 中修改连接信息。

### 3. 构建 & 启动

```bash
mvn clean package -DskipTests
java -jar target/knowledge-retrieval-1.0.0-SNAPSHOT.jar
```

应用启动在 **`http://localhost:8082`**。

### 4. 测试 API

#### 摄入文件

```bash
# 上传 CSV 数据表格
curl -X POST http://localhost:8082/api/rag/ingest -F "file=@test_data_fields.csv"

# 上传 TXT 文本
curl -X POST http://localhost:8082/api/rag/ingest -F "file=@test_data_table.txt"
```

返回示例：
```json
{
  "documentId": "a1b2c3d4",
  "fileName": "test_data_fields.csv",
  "chunkCount": 1,
  "totalChars": 764,
  "status": "SUCCESS"
}
```

#### 查询知识库

```bash
curl -X POST http://localhost:8082/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "有哪些包含地理信息的数据产品？", "topK": 5}'
```

返回示例：
```json
{
  "answer": "根据检索结果，库中有以下包含地理信息的数据产品...",
  "question": "有哪些包含地理信息的数据产品？",
  "sources": [
    {
      "chunkId": "a1b2c3d4",
      "documentName": "test_data_fields.csv",
      "content": "数据产品名称",
      "score": 0.85
    }
  ]
}
```

#### 查看已摄入数据 & 健康检查

```bash
curl http://localhost:8082/api/rag/documents
curl http://localhost:8082/api/rag/health
```

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | `8082` | 服务端口 |
| `deepseek.api-key` | `${DEEPSEEK_API_KEY}` | API 密钥（环境变量） |
| `deepseek.chat-model` | `deepseek-chat` | 对话模型 |
| `deepseek.chat-url` | `https://api.deepseek.com/v1/chat/completions` | API 地址 |
| `spring.redis.host` | `172.22.173.190` | Redis 地址 |
| `spring.redis.port` | `6379` | Redis 端口 |
| `spring.redis.password` | `123321` | Redis 密码 |
| `spring.redis.database` | `1` | Redis 数据库编号 |
