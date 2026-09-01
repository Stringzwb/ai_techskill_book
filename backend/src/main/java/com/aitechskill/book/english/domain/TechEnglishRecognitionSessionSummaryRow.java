package com.aitechskill.book.english.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 识图历史按上传会话聚合的查询结果。
 */
@Getter
@Setter
public class TechEnglishRecognitionSessionSummaryRow {

    /** 上传会话标识。 */
    private String sessionUuid;

    /** 资料来源。 */
    private String sourceName;

    /** 扩展例句场景。 */
    private String scenario;

    /** 声明的子任务数。 */
    private Integer chunkCount;

    /** 已创建的子任务数。 */
    private Long taskCount;

    /** 处理中子任务数。 */
    private Long processingCount;

    /** 识别成功子任务数。 */
    private Long recognizedCount;

    /** 识别失败子任务数。 */
    private Long failedCount;

    /** 已入库子任务数。 */
    private Long importedCount;

    /** 图片总数。 */
    private Long imageCount;

    /** 识别语料总数。 */
    private Long itemCount;

    /** 会话创建时间。 */
    private LocalDateTime createtime;

    /** 会话最后更新时间。 */
    private LocalDateTime updatetime;
}
