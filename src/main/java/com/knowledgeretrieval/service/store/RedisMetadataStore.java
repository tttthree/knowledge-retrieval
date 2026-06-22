package com.knowledgeretrieval.service.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgeretrieval.model.DataProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 元数据存储：DataProduct JSON + 嵌入向量。
 * 支持关键词粗筛 + 余弦相似度向量检索。
 *
 * @author zt
 * @version 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMetadataStore {

    private static final String PRODUCT_KEY = "dp:product:";
    private static final String SIG_KEY = "dp:sig:";
    private static final String DOCS_KEY = "dp:docs";
    private static final long TTL_DAYS = 7;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /** 存入 DataProduct + 文本签名 */
    public void put(DataProduct product, Set<String> signature) {
        String json = toJson(product);
        redisTemplate.opsForValue().set(PRODUCT_KEY + product.getId(), json, TTL_DAYS, TimeUnit.DAYS);
        String sigJson = toJson(new ArrayList<>(signature));
        redisTemplate.opsForValue().set(SIG_KEY + product.getId(), sigJson, TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().add(DOCS_KEY, product.getId());
        redisTemplate.expire(DOCS_KEY, TTL_DAYS, TimeUnit.DAYS);
        log.info("数据产品已存入: '{}' (id={})", product.getTitle(), product.getId());
    }

    public DataProduct get(String id) {
        String json = redisTemplate.opsForValue().get(PRODUCT_KEY + id);
        return json != null ? fromJson(json) : null;
    }

    public List<String> listIds() {
        Set<String> ids = redisTemplate.opsForSet().members(DOCS_KEY);
        return ids == null ? List.of() : new ArrayList<>(ids);
    }

    /** Jaccard 相似度检索 */
    public List<ScoredProduct> similaritySearch(Set<String> querySig, int topK) {
        Set<String> allIds = redisTemplate.opsForSet().members(DOCS_KEY);
        if (allIds == null || allIds.isEmpty()) return List.of();

        PriorityQueue<ScoredProduct> heap = new PriorityQueue<>(
                topK, Comparator.comparingDouble(ScoredProduct::score));

        for (String id : allIds) {
            Set<String> sig = getSignature(id);
            if (sig == null) continue;
            double jaccard = jaccard(querySig, sig);
            if (jaccard > 0) {
                ScoredProduct sp = new ScoredProduct(get(id), (int) (jaccard * 100));
                if (heap.size() < topK) {
                    heap.offer(sp);
                } else if (!heap.isEmpty() && jaccard * 100 > heap.peek().score()) {
                    heap.poll();
                    heap.offer(sp);
                }
            }
        }

        List<ScoredProduct> results = new ArrayList<>(heap);
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results;
    }

    private Set<String> getSignature(String id) {
        String json = redisTemplate.opsForValue().get(SIG_KEY + id);
        if (json == null) return null;
        try {
            List<String> list = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return new HashSet<>(list);
        } catch (Exception e) { return null; }
    }

    private double jaccard(Set<String> a, Set<String> b) {
        Set<String> inter = new HashSet<>(a); inter.retainAll(b);
        Set<String> union = new HashSet<>(a); union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    private String toJson(Object obj) { try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { throw new RuntimeException(e); } }
    private DataProduct fromJson(String j) { try { return objectMapper.readValue(j, DataProduct.class); } catch (JsonProcessingException e) { throw new RuntimeException(e); } }

    public static class ScoredProduct {
        private final DataProduct product;
        private final int score;
        public ScoredProduct(DataProduct p, int s) { this.product = p; this.score = s; }
        public DataProduct product() { return product; }
        public int score() { return score; }
    }
}
