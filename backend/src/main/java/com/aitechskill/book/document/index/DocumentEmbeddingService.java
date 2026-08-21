package com.aitechskill.book.document.index;

import java.util.List;

/** 文档片段向量生成接口。 */
public interface DocumentEmbeddingService {

    /** 为多个片段生成向量，顺序与输入一致。 */
    List<float[]> embedAll(List<String> texts);
}
