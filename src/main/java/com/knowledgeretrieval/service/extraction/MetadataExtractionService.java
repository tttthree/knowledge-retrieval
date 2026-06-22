package com.knowledgeretrieval.service.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgeretrieval.config.DeepSeekConfig;
import com.knowledgeretrieval.model.DataProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 调用 DeepSeek 从 CSV 内容中抽取结构化元数据。
 * 对应论文第二章：元数据抽取层。
 *
 * @author zt
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataExtractionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeepSeekConfig deepSeekConfig;

    /**
     * 从 CSV 原始文本中抽取结构化元数据。
     *
     * @param csvContent CSV 文件内容
     * @param fileName   文件名
     * @return 抽取出的 DataProduct
     */
    public DataProduct extract(String csvContent, String fileName) {
        String prompt = buildExtractionPrompt(csvContent, fileName);
        String jsonResponse = callDeepSeek(prompt);
        return parseMetadataResponse(jsonResponse);
    }

    private String buildExtractionPrompt(String content, String fileName) {
        // 只取前 3000 字符做样本，避免 token 超限
        String sample = content.length() > 3000 ? content.substring(0, 3000) : content;

        return String.join("\n",
                "你是一个数据产品目录的管理员。请根据以下文件内容，抽取结构化元数据。",
                "",
                "## 文件信息",
                "文件名：" + fileName,
                "",
                "## 文件内容（前 3000 字符）",
                "可能是 CSV 表格、HTML 表格、或纯文本数据。请识别其中的数据结构。",
                sample,
                "",
                "## 要求",
                "1. 识别文件中的表格/数据区域，分析列名和数据特征",
                "2. 推断每列的名称、类型（string/number/date）、含义",
                "3. 给出数据产品名称、一句话描述、标签列表",
                "4. 仅返回一个合法 JSON 对象，禁止 markdown、代码块、解释文字",
                "",
                "JSON 格式：",
                "{",
                "  \"title\": \"数据产品名称\",",
                "  \"description\": \"一句话描述数据内容和用途\",",
                "  \"fields\": [",
                "    {\"name\": \"列名\", \"type\": \"string/number/date\", \"description\": \"含义\"}",
                "  ],",
                "  \"tags\": [\"标签1\", \"标签2\"]",
                "}"
        );
    }

    private String callDeepSeek(String prompt) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", deepSeekConfig.getChatModel());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.1);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                deepSeekConfig.getChatUrl(), request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("DeepSeek 元数据抽取 API 返回非 2xx");
        }

        try {
            return objectMapper.readTree(response.getBody())
                    .path("choices").get(0)
                    .path("message").path("content").asText("");
        } catch (Exception e) {
            throw new RuntimeException("解析 DeepSeek 响应失败", e);
        }
    }

    private DataProduct parseMetadataResponse(String json) {
        try {
            // 防御性清洗
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json?|```", "").trim();
            }
            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            return objectMapper.readValue(cleaned, DataProduct.class);
        } catch (Exception e) {
            log.error("解析元数据 JSON 失败: {}", e.getMessage());
            return DataProduct.builder()
                    .title("未知")
                    .description("元数据解析失败")
                    .fields(Collections.emptyList())
                    .tags(Collections.emptyList())
                    .build();
        }
    }
}
