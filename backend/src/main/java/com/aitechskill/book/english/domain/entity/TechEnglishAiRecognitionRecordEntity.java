package com.aitechskill.book.english.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 技术英语 AI 识图子任务永久记录。
 */
@Getter
@Setter
@TableName("tech_english_ai_recognition_record")
public class TechEnglishAiRecognitionRecordEntity extends BaseEntity {

    /** 记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 页面一次上传会话标识。 */
    private String sessionUuid;

    /** 用户填写的识图批次名称，仅用于识图记录。 */
    private String batchName;

    /** 单个 AI 识别批次标识。 */
    private String batchUuid;

    /** 子任务状态。 */
    private String status;

    /** 识图资料来源。 */
    private String sourceName;

    /** 扩展例句场景。 */
    private String scenario;

    /** 当前子任务序号。 */
    private Integer chunkIndex;

    /** 本次上传的子任务总数。 */
    private Integer chunkCount;

    /** 当前子任务图片数。 */
    private Integer imageCount;

    /** 识别到的语料数。 */
    private Integer itemCount;

    /** 每条语料扩展例句数。 */
    private Integer exampleCount;

    /** 已清洗的识图预览结果 JSON。 */
    private String resultJson;

    /** AI 原始返回内容，便于完整留档和后续重试。 */
    private String rawResultJson;

    /** 已保存的原图对象元数据 JSON。 */
    private String sourceImagesJson;

    /** 失败业务码。 */
    private String errorCode;

    /** 可安全展示的失败说明。 */
    private String errorMessage;

    /** 识别完成或失败时间。 */
    private LocalDateTime completedAt;

    /** 确认入库完成时间。 */
    private LocalDateTime importedAt;
}
