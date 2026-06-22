package com.knowledgeretrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档摄入结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {

    /** 文档唯一标识 */
    private String documentId;

    /** 原始文件名 */
    private String fileName;

    /** 文档被切分为多少块 */
    private int chunkCount;

    /** 文档总字符数 */
    private int totalChars;

    /** 状态信息 */
    private String status;
}
