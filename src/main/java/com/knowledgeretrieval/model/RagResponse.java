package com.knowledgeretrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 查询响应体，包含 LLM 回答与来源引用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {

    /** AI 生成的回答 */
    private String answer;

    /** 原始用户问题 */
    private String question;

    /** 用于生成回答的相关文档块列表（不含嵌入向量） */
    private List<SourceChunk> sources;

    /**
     * 返回给客户端的精简块信息（不含嵌入向量）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {

        /** 块 ID */
        private String chunkId;

        /** 文档名称 */
        private String documentName;

        /** 文本内容 */
        private String content;

        /** 在文档中的序号 */
        private int chunkIndex;

        /** 余弦相似度得分 */
        private double score;

        public static SourceChunk from(DocumentChunk chunk, double score) {
            return SourceChunk.builder()
                    .chunkId(chunk.getId())
                    .documentName(chunk.getDocumentName())
                    .content(chunk.getContent())
                    .chunkIndex(chunk.getChunkIndex())
                    .score(score)
                    .build();
        }
    }
}
