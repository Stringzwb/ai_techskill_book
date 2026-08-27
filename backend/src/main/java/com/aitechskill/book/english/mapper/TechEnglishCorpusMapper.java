package com.aitechskill.book.english.mapper;

import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 已发布技术英语语料数据访问接口。
 */
public interface TechEnglishCorpusMapper extends BaseMapper<TechEnglishCorpusEntity> {

    /** 查询符合关键词、类型和标签条件的已发布语料。 */
    List<TechEnglishCorpusEntity> selectPublishedPage(
            @Param("keyword") String keyword,
            @Param("corpusType") String corpusType,
            @Param("tagId") Long tagId,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 统计符合关键词、类型和标签条件的已发布语料。 */
    long countPublished(
            @Param("keyword") String keyword,
            @Param("corpusType") String corpusType,
            @Param("tagId") Long tagId);

    /** 查询一条已发布语料。 */
    TechEnglishCorpusEntity selectPublishedById(@Param("id") long id);

    /** 批量查询语料关联知识标签。 */
    List<DocumentTagRecord> selectTagsByCorpusIds(@Param("corpusIds") List<Long> corpusIds);
}
