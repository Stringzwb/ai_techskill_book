package com.aitechskill.book.bootstrap;

import com.aitechskill.book.article.KnowledgeArticle;
import com.aitechskill.book.article.KnowledgeArticleRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final KnowledgeArticleRepository repository;

    public DemoDataInitializer(KnowledgeArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new KnowledgeArticle(
                        "Spring Boot 生产级配置清单",
                        "从线程池、连接池到健康检查，建立一套可复用的后端上线基线。",
                        "Java 后端", "进阶", 18, "Spring Boot,JVM,生产实践", true),
                new KnowledgeArticle(
                        "Vue 3 组合式 API 的工程化边界",
                        "掌握组件拆分、状态管理和请求层设计，让前端项目持续可维护。",
                        "前端工程", "中级", 14, "Vue 3,TypeScript,Vite", true),
                new KnowledgeArticle(
                        "从零搭建企业级 RAG 知识库",
                        "理解文档切分、向量检索、重排与评测，构建可靠的知识增强应用。",
                        "AI 应用", "进阶", 24, "RAG,大模型,向量数据库", true),
                new KnowledgeArticle(
                        "MySQL 索引失效的系统排查法",
                        "用执行计划、统计信息与慢查询日志定位真实瓶颈。",
                        "数据工程", "中级", 16, "MySQL,索引,SQL优化", true),
                new KnowledgeArticle(
                        "Linux 服务稳定性检查手册",
                        "围绕 CPU、内存、磁盘、网络与 systemd 建立故障定位路径。",
                        "云原生", "基础", 12, "Linux,systemd,运维", true),
                new KnowledgeArticle(
                        "高并发系统的限流与降级设计",
                        "从流量模型出发选择合适的限流算法，并设计清晰的降级策略。",
                        "架构进阶", "高级", 20, "系统设计,限流,稳定性", true)));
    }
}
