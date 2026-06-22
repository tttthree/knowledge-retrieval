package com.knowledgeretrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据产品：LLM 从 CSV 中抽取的结构化元数据。
 *
 * @author zt
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataProduct {

    /** 唯一标识 */
    private String id;

    /** 来源文件名 */
    private String source;

    /** 数据产品名称 */
    private String title;

    /** 描述 */
    private String description;

    /** 字段列表 */
    private List<FieldMeta> fields;

    /** 标签/关键词 */
    private List<String> tags;

    /** 可信等级（占位，第三阶段实现） */
    private String trustLevel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldMeta {
        private String name;
        private String type;
        private String description;
    }
}
