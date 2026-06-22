package com.knowledgeretrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 查询请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagRequest {

    /** 用户问题 */
    private String question;

    /** 检索的相关块数量，默认 5 */
    @Builder.Default
    private int topK = 5;
}
