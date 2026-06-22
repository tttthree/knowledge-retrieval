package com.knowledgeretrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档块：文档切分后的最小检索单元，包含文本内容与向量嵌入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /** 块唯一标识 */
    private String id;

    /** 所属文档 ID */
    private String documentId;

    /** 文档名称 */
    private String documentName;

    /** 文本内容 */
    private String content;

    /** 向量嵌入 */
    private float[] embedding;

    /** 在文档中的序号 */
    private int chunkIndex;

    /** 附加元数据 */
    private Map<String, Object> metadata;
}
