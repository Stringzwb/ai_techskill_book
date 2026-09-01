package com.aitechskill.book.english.mapper;

import com.aitechskill.book.english.domain.TechEnglishRecognitionSessionSummaryRow;
import com.aitechskill.book.english.domain.entity.TechEnglishAiRecognitionRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 技术英语 AI 识图记录数据访问接口。
 */
public interface TechEnglishAiRecognitionRecordMapper extends BaseMapper<TechEnglishAiRecognitionRecordEntity> {

    /** 统计当前用户的识图上传会话数。 */
    long countSessions(@Param("userId") long userId);

    /** 分页查询当前用户的识图上传会话。 */
    List<TechEnglishRecognitionSessionSummaryRow> selectSessionPage(
            @Param("userId") long userId,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 查询当前用户一次上传会话下的全部子任务。 */
    List<TechEnglishAiRecognitionRecordEntity> selectSessionTasks(
            @Param("userId") long userId,
            @Param("sessionUuid") String sessionUuid);
}
