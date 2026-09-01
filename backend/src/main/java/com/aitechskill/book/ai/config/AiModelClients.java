package com.aitechskill.book.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;

/**
 * 按主用、备用顺序保存的 LangChain4j 模型客户端。
 *
 * @param models 模型客户端列表
 */
public record AiModelClients(List<ChatLanguageModel> models) {

    public AiModelClients {
        models = List.copyOf(models);
    }
}
