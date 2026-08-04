package com.aitechskill.book.home;

import com.aitechskill.book.article.ArticleSummary;
import com.aitechskill.book.article.KnowledgeArticleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公开首页内容服务。
 */
@Service
public class KnowledgeHomeService {

    private static final List<CategoryDefinition> CATEGORY_DEFINITIONS = List.of(
            new CategoryDefinition("Java 后端", "JAVA", "Spring Boot、JVM 与高并发服务"),
            new CategoryDefinition("前端工程", "WEB", "Vue、TypeScript 与工程化体系"),
            new CategoryDefinition("AI 应用", "AI", "大模型、RAG 与智能体工程"),
            new CategoryDefinition("数据工程", "DATA", "MySQL、缓存与实时数据链路"),
            new CategoryDefinition("云原生", "CLOUD", "Linux、容器与可观测性"),
            new CategoryDefinition("架构进阶", "ARCH", "系统设计、稳定性与技术决策"));

    private final KnowledgeArticleRepository repository;

    public KnowledgeHomeService(KnowledgeArticleRepository repository) {
        this.repository = repository;
    }

    /**
     * 汇总首页分类和精选专题。
     *
     * @return 首页内容
     */
    @Transactional(readOnly = true)
    public HomeResponse getHome() {
        List<CategorySummary> categories = CATEGORY_DEFINITIONS.stream()
                .map(definition -> new CategorySummary(
                        definition.name(),
                        definition.code(),
                        definition.description(),
                        repository.countByCategory(definition.name())))
                .toList();
        List<ArticleSummary> featured = repository
                .findTop6ByFeaturedTrueOrderByCreatedAtDesc()
                .stream()
                .map(ArticleSummary::from)
                .toList();
        return new HomeResponse(
                "技术岗AI知识库",
                "把复杂技术，变成可执行的成长路径",
                repository.count(),
                4,
                categories.size(),
                categories,
                featured);
    }

    private record CategoryDefinition(String name, String code, String description) {
    }
}
