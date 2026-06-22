package com.knowledgeretrieval.controller;

import com.knowledgeretrieval.dto.IngestionResult;
import com.knowledgeretrieval.model.RagRequest;
import com.knowledgeretrieval.model.RagResponse;
import com.knowledgeretrieval.service.rag.RagPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * RAG 系统 REST API 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RagController {

    private final RagPipelineService ragPipelineService;

    /**
     * 摄入文档到知识库。
     *
     * POST /api/rag/ingest
     * Content-Type: multipart/form-data
     * Body: file=<document>
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResult> ingestDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            IngestionResult result = ragPipelineService.ingest(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("摄入失败 '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(IngestionResult.builder()
                            .fileName(file.getOriginalFilename())
                            .status("FAILED: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 向 RAG 系统提问。
     *
     * POST /api/rag/query
     * Content-Type: application/json
     * Body: { "question": "...", "topK": 5 }
     */
    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RagResponse> query(@RequestBody RagRequest request) {
        RagResponse response = ragPipelineService.query(
                request.getQuestion(),
                request.getTopK()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 列出所有已摄入文档。
     *
     * GET /api/rag/documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<String>> listDocuments() {
        List<String> documents = ragPipelineService.listDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "knowledge-retrieval-rag"));
    }
}
