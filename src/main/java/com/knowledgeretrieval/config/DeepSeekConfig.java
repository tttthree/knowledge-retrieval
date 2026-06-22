package com.knowledgeretrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置，从 application.yml 加载。
 * 提供 Chat API 和 Embedding API 的密钥、地址与模型名。
 *
 * @author zt
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /** API 密钥 */
    private String apiKey;

    /** Chat 接口完整 URL */
    private String chatUrl;

    /** Embedding 接口完整 URL */
    private String embeddingUrl;

    /** 对话模型名称 */
    private String chatModel;

    /** 嵌入模型名称 */
    private String embeddingModel;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatUrl() {
        return chatUrl;
    }

    public void setChatUrl(String chatUrl) {
        this.chatUrl = chatUrl;
    }

    public String getEmbeddingUrl() {
        return embeddingUrl;
    }

    public void setEmbeddingUrl(String embeddingUrl) {
        this.embeddingUrl = embeddingUrl;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
