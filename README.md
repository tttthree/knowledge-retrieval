# Knowledge Retrieval RAG 系统

毕业论文原型 — 基于检索增强生成（RAG）的知识检索系统。

## 技术栈

- **语言**: Java 11
- **框架**: Spring Boot 2.7.18
- **构建**: Maven
- **大模型**: DeepSeek API（deepseek-chat）
- **向量存储**: Redis（余弦相似度检索）
- **文档解析**: Apache PDFBox 3.0.1

## 项目架构

```
com.knowledgeretrieval
├── KnowledgeRetrievalApplication.java       # 启动类
├── config/
│   └── DeepSeekConfig.java                  # DeepSeek API 配置
├── controller/
│   ├── RagController.java                   # REST API
│   └── GlobalExceptionHandler.java          # 全局异常处理
├── model/
│   ├── DocumentChunk.java                   # 文档块模型
│   ├── RagRequest.java                      # 查询请求
│   └── RagResponse.java                     # 查询响应（含来源引用）
├── dto/
│   ├── IngestionResult.java                 # 摄入结果
│   └── ChatMessage.java                     # 消息格式
├── service/
│   ├── document/
│   │   ├── DocumentParser.java              # TXT/PDF 解析
│   │   └── TextChunker.java                 # 文本切分（500字+50重叠）
│   ├── embedding/
│   │   └── EmbeddingService.java            # DeepSeek Embedding API
│   ├── vectorstore/
│   │   └── RedisVectorStore.java            # Redis 向量存储+余弦检索
│   ├── llm/
│   │   └── LlmService.java                  # DeepSeek Chat API
│   └── rag/
│       └── RagPipelineService.java          # RAG 核心编排
```

## 快速开始

### 1. 配置环境

```bash
# 必须：DeepSeek API Key
export DEEPSEEK_API_KEY=sk-your-key-here

# 可选：Redis 连接（默认 localhost:6379 db=1）
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
```

### 2. 启动 Redis

```bash
# 确保 Redis 在 localhost:6379 运行
redis-cli ping   # 应返回 PONG
```

### 3. 构建项目

```bash
mvn clean package -DskipTests
```

### 4. 启动应用

```bash
java -jar target/knowledge-retrieval-1.0.0-SNAPSHOT.jar
```

应用启动在 `http://localhost:8081`。

### 5. 测试 API

#### 摄入文档

```bash
# 上传文本文件
curl -X POST http://localhost:8081/api/rag/ingest \
  -F "file=@test.txt"

# 上传 PDF 文件
curl -X POST http://localhost:8081/api/rag/ingest \
  -F "file=@paper.pdf"
```

返回示例：
```json
{
  "documentId": "a1b2c3d4",
  "fileName": "test.txt",
  "chunkCount": 8,
  "totalChars": 3500,
  "status": "SUCCESS"
}
```

#### 查询知识库

```bash
curl -X POST http://localhost:8081/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "这份文档的主要内容是什么？", "topK": 5}'
```

返回示例：
```json
{
  "answer": "根据【片段 1】和【片段 2】，这份文档主要讨论了...",
  "question": "这份文档的主要内容是什么？",
  "sources": [
    {
      "chunkId": "a1b2c3d4-chunk-0",
      "documentName": "test.txt",
      "content": "文档片段内容...",
      "chunkIndex": 0,
      "score": 0.9234
    }
  ]
}
```

#### 查看已摄入文档

```bash
curl http://localhost:8081/api/rag/documents
```

#### 健康检查

```bash
curl http://localhost:8081/api/rag/health
```

## RAG 流程说明

### Ingestion（文档摄入）
```
上传文件 → 解析(TXT/PDF) → 文本切块 → DeepSeek Embedding → Redis 向量存储
```

### Query（知识检索）
```
用户问题 → Query Embedding → 余弦相似度检索 → 构建增强提示 → DeepSeek 生成回答
```

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `deepseek.api-key` | 环境变量 `DEEPSEEK_API_KEY` | API 密钥 |
| `deepseek.chat-model` | `deepseek-chat` | 对话模型 |
| `deepseek.embedding-model` | `deepseek-chat` | 嵌入模型 |
| `server.port` | `8081` | 服务端口 |
| `spring.redis.host` | `localhost` | Redis 地址 |
| `spring.redis.port` | `6379` | Redis 端口 |
| `spring.redis.database` | `1` | Redis 数据库编号 |
| `chunking.chunk-size` | `500` | 文本块大小（字符） |
| `chunking.overlap-size` | `50` | 块重叠大小（字符） |
