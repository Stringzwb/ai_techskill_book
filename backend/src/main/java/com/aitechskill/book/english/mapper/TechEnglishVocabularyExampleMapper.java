package com.aitechskill.book.english.mapper;

import com.aitechskill.book.english.domain.entity.TechEnglishVocabularyExampleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 技术英语词汇例句数据访问接口。
 */
public interface TechEnglishVocabularyExampleMapper extends BaseMapper<TechEnglishVocabularyExampleEntity> {

    /** 锁定指定词汇下的有效例句，避免并发保存时重复创建句子语料。 */
    TechEnglishVocabularyExampleEntity selectActiveByVocabularyAndIdForUpdate(
            @Param("vocabularyCorpusId") long vocabularyCorpusId,
            @Param("exampleId") long exampleId);
}
