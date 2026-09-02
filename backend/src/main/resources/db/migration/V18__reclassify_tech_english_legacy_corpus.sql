UPDATE `tech_english_corpus`
SET `corpus_type` = 'PHRASE',
    `title` = COALESCE(NULLIF(TRIM(`title`), ''), NULLIF(TRIM(`image_alt`), ''), '短语语料'),
    `english_text` = COALESCE(NULLIF(TRIM(`english_text`), ''), NULLIF(TRIM(`image_alt`), ''), NULLIF(TRIM(`title`), ''), '短语语料')
WHERE `corpus_type` = 'IMAGE'
  AND `deleted` = 0;

UPDATE `tech_english_corpus`
SET `corpus_type` = 'SENTENCE'
WHERE `deleted` = 0
  AND `corpus_type` IN ('VOCABULARY', 'PHRASE')
  AND (
      (`sentence_pattern` IS NOT NULL AND TRIM(`sentence_pattern`) <> '')
      OR REGEXP_LIKE(TRIM(COALESCE(NULLIF(`english_text`, ''), `title`, '')), '[.!?]$')
      OR REGEXP_LIKE(
          TRIM(COALESCE(NULLIF(`english_text`, ''), `title`, '')),
          '^(This|That|It|There|We|You|They|I|The|A|An|If|When|While|Because|Before|After|During)[[:space:]].*[[:space:]](is|are|was|were|be|been|being|can|could|should|would|will|may|might|must|has|have|had|does|do|did)[[:space:]]'
      )
  );

UPDATE `tech_english_corpus`
SET `corpus_type` = 'PHRASE'
WHERE `deleted` = 0
  AND `corpus_type` = 'VOCABULARY'
  AND REGEXP_LIKE(TRIM(COALESCE(NULLIF(`english_text`, ''), `title`, '')), '[[:space:]]');
