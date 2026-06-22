package com.knowledgeretrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat API 消息格式（OpenAI 兼容）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 角色：system / user / assistant */
    private String role;

    /** 消息内容 */
    private String content;
}
