package com.knowledgeretrieval.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgeretrieval.config.DeepSeekConfig;
import com.knowledgeretrieval.dto.ChatMessage;
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
 * 调用 DeepSeek Chat API（OpenAI 兼容），生成文本回答。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeepSeekConfig deepSeekConfig;

    /**
     * 简单对话：发送一条用户消息，获取助手回复。
     */
    public String chat(String userMessage) {
        List<ChatMessage> messages = Collections.singletonList(
                ChatMessage.builder().role("user").content(userMessage).build()
        );
        return chatWithMessages(messages);
    }

    /**
     * 多轮对话：发送完整的消息列表（如系统提示 + 用户消息）。
     */
    public String chatWithMessages(List<ChatMessage> messages) {
        try {
            List<Map<String, String>> apiMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                Map<String, String> apiMsg = new LinkedHashMap<>();
                apiMsg.put("role", msg.getRole());
                apiMsg.put("content", msg.getContent());
                apiMessages.add(apiMsg);
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", deepSeekConfig.getChatModel());
            requestBody.put("messages", apiMessages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1024);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekConfig.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    deepSeekConfig.getChatUrl(), request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("DeepSeek Chat API 返回非 2xx 或空响应");
            }

            return parseChatResponse(response.getBody());
        } catch (Exception e) {
            log.error("调用 DeepSeek Chat API 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Chat API 调用失败", e);
        }
    }

    /**
     * 解析 API 返回的 JSON，提取助手回答文本。
     */
    private String parseChatResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                String content = message.get("content").asText();
                log.debug("LLM 回答长度: {} 字符", content.length());
                return content;
            }
        }
        log.warn("无法解析 LLM 响应结构: {}", responseJson);
        return "Error: 无法解析 LLM 回答";
    }
}
