package com.knowledgeretrieval.service.rag;

import com.knowledgeretrieval.dto.IngestionResult;
import com.knowledgeretrieval.model.DataProduct;
import com.knowledgeretrieval.model.RagResponse;
import com.knowledgeretrieval.service.document.DocumentParser;
import com.knowledgeretrieval.service.embedding.TextSimilarityService;
import com.knowledgeretrieval.service.extraction.MetadataExtractionService;
import com.knowledgeretrieval.service.llm.LlmService;
import com.knowledgeretrieval.service.store.RedisMetadataStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 元数据检索核心编排服务。
 *
 * 管线：
 * 1. 摄入：文件 → 解析 → DeepSeek 抽取元数据 → 文本签名 → Redis
 * 2. 检索：问题 → 签名 → Jaccard 相似度 → DeepSeek 回答
 *
 * Token 仅用于 DeepSeek 的抽取和问答，检索用纯 Java 本地计算。
 *
 * @author zt
 * @version 3.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineService {

    private final DocumentParser documentParser;
    private final MetadataExtractionService metadataExtractionService;
    private final TextSimilarityService textSimilarity;
    private final LlmService llmService;
    private final RedisMetadataStore metadataStore;

    public IngestionResult ingest(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String documentId = UUID.randomUUID().toString().substring(0, 8);
        log.info("摄入: '{}' (id={})", fileName, documentId);

        String rawText = documentParser.parse(file.getInputStream(), fileName);

        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        if (lowerName.endsWith(".csv") || lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            ingestDataProduct(documentId, fileName, rawText);
        } else {
            ingestDocument(documentId, fileName, rawText);
        }

        log.info("摄入成功: '{}' (id={})", fileName, documentId);
        return IngestionResult.builder().documentId(documentId).fileName(fileName)
                .chunkCount(1).totalChars(rawText.length()).status("SUCCESS").build();
    }

    private void ingestDataProduct(String id, String name, String content) {
        DataProduct product = metadataExtractionService.extract(content, name);
        product.setId(id); product.setSource(name); product.setTrustLevel("public");
        String searchText = buildSearchText(product);
        Set<String> sig = textSimilarity.signature(searchText);
        metadataStore.put(product, sig);
    }

    private void ingestDocument(String id, String name, String content) {
        for (String part : content.split("[。\\n]")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            DataProduct dp = DataProduct.builder().id(id + "-" + t.hashCode())
                    .source(name).title(name).description(t).build();
            Set<String> sig = textSimilarity.signature(t);
            metadataStore.put(dp, sig);
        }
    }

    public RagResponse query(String question, int topK) {
        log.info("查询: '{}' (topK={})", question, topK);
        Set<String> querySig = textSimilarity.signature(question);
        List<RedisMetadataStore.ScoredProduct> results = metadataStore.similaritySearch(querySig, topK);

        String prompt = buildAnswerPrompt(question, results);
        String answer = llmService.chat(prompt);

        List<RagResponse.SourceChunk> sources = results.stream()
                .map(sp -> RagResponse.SourceChunk.builder()
                        .chunkId(sp.product().getId()).documentName(sp.product().getSource())
                        .content(sp.product().getTitle()).score(sp.score() / 100.0).build())
                .collect(Collectors.toList());
        return RagResponse.builder().answer(answer).question(question).sources(sources).build();
    }

    public List<String> listDocuments() {
        return metadataStore.listIds().stream()
                .map(id -> { DataProduct dp = metadataStore.get(id); return dp != null ? id + " | " + dp.getTitle() : id; })
                .collect(Collectors.toList());
    }

    private String buildSearchText(DataProduct dp) {
        StringBuilder sb = new StringBuilder();
        if (dp.getTitle() != null) sb.append(dp.getTitle()).append(" ");
        if (dp.getDescription() != null) sb.append(dp.getDescription()).append(" ");
        if (dp.getFields() != null) dp.getFields().forEach(f -> sb.append(f.getName()).append(" "));
        if (dp.getTags() != null) dp.getTags().forEach(t -> sb.append(t).append(" "));
        return sb.toString();
    }

    private String buildAnswerPrompt(String question, List<RedisMetadataStore.ScoredProduct> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数据产品目录管理员。请根据检索到的产品信息回答用户问题。\n\n");
        if (products.isEmpty()) {
            sb.append("库中暂无匹配资产，请告知用户。\n\n");
        } else {
            sb.append("## 检索到的数据产品\n\n");
            for (int i = 0; i < products.size(); i++) {
                DataProduct dp = products.get(i).product();
                double sim = products.get(i).score() / 100.0;
                sb.append(String.format("【%d】(相似度: %.2f) %s | %s\n  描述: %s\n",
                        i + 1, sim, dp.getTitle(), dp.getSource(), dp.getDescription()));
                if (dp.getFields() != null && !dp.getFields().isEmpty()) {
                    sb.append("  字段: ");
                    dp.getFields().forEach(f -> sb.append(f.getName()).append(" "));
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        sb.append("## 回答要求\n综合产品信息回答，注明引用来源。\n\n## 用户问题\n").append(question).append("\n");
        return sb.toString();
    }
}
