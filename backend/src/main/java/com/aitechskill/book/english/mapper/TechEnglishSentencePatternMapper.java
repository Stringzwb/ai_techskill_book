package com.aitechskill.book.english.mapper;

import com.aitechskill.book.english.domain.entity.TechEnglishSentencePatternEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 技术英语句式框架及其语料关联的数据访问接口。
 */
public interface TechEnglishSentencePatternMapper extends BaseMapper<TechEnglishSentencePatternEntity> {

    /** 按规范化句式读取当前可用的独立框架。 */
    TechEnglishSentencePatternEntity selectActiveByNormalizedPattern(
            @Param("normalizedPattern") String normalizedPattern);

    /** 幂等关联一句句子语料与一个句式框架。 */
    int insertCorpusLink(
            @Param("sentencePatternId") long sentencePatternId,
            @Param("corpusId") long corpusId,
            @Param("userId") long userId);
}
