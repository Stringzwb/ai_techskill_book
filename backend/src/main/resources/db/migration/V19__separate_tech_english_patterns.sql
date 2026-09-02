/* 句式是可复用的语法骨架，不应继续伪装成完整句子。保留原 SENTENCE 语料，
   另建去重后的 PATTERN 语料供独立学习分区使用。 */
INSERT INTO tech_english_corpus
    (corpus_uuid, corpus_type, title, english_text, explanation,
     translation_status, ai_review_status, index_status, content_version,
     status, published_at, createby, updateby, deleted)
SELECT UUID(), 'PATTERN', LEFT(normalized.pattern_text, 160), normalized.pattern_text,
       LEFT(normalized.pattern_explanation, 1000),
       'NONE', 'REVIEWED', 'NOT_INDEXED', 1,
       'PUBLISHED', CURRENT_TIMESTAMP, 0, 0, 0
FROM (
    SELECT MIN(TRIM(sentence_pattern)) AS pattern_text,
           LOWER(TRIM(REGEXP_REPLACE(sentence_pattern, '[[:space:]]+', ' '))) AS normalized_pattern,
           MAX(NULLIF(TRIM(sentence_pattern_explanation), '')) AS pattern_explanation
    FROM tech_english_corpus
    WHERE corpus_type = 'SENTENCE'
      AND deleted = 0
      AND sentence_pattern IS NOT NULL
      AND TRIM(sentence_pattern) <> ''
    GROUP BY LOWER(TRIM(REGEXP_REPLACE(sentence_pattern, '[[:space:]]+', ' ')))
) AS normalized
WHERE NOT EXISTS (
    SELECT 1
    FROM tech_english_corpus AS existing
    WHERE existing.corpus_type = 'PATTERN'
      AND existing.deleted = 0
      AND LOWER(TRIM(REGEXP_REPLACE(existing.english_text, '[[:space:]]+', ' '))) = normalized.normalized_pattern
);
