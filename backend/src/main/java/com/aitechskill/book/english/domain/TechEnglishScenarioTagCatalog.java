package com.aitechskill.book.english.domain;

import com.aitechskill.book.english.domain.response.TechEnglishScenarioTagResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 技术英语语料的固定场景标签目录。
 */
public final class TechEnglishScenarioTagCatalog {

    private static final List<TechEnglishScenarioTagResponse> TAGS = List.of(
            new TechEnglishScenarioTagResponse("general_work", "通用工作场景", "跨岗位都适用的日常办公与协作语境"),
            new TechEnglishScenarioTagResponse("study_review", "学习复盘", "知识整理、复习、总结经验"),
            new TechEnglishScenarioTagResponse("meeting_sync", "会议沟通", "站会、评审、同步进展、讨论决策"),
            new TechEnglishScenarioTagResponse("project_planning", "项目规划", "需求拆解、排期、里程碑与资源安排"),
            new TechEnglishScenarioTagResponse("requirement_analysis", "需求分析", "需求澄清、边界确认、目标定义"),
            new TechEnglishScenarioTagResponse("product_design", "产品设计", "功能定义、交互说明、方案讨论"),
            new TechEnglishScenarioTagResponse("architecture_design", "架构设计", "系统分层、模块边界、技术方案"),
            new TechEnglishScenarioTagResponse("frontend_ui", "前端界面", "页面布局、组件、交互、视觉与可用性"),
            new TechEnglishScenarioTagResponse("frontend_state", "前端状态管理", "状态同步、表单、缓存、路由与联动"),
            new TechEnglishScenarioTagResponse("backend_api", "后端接口", "REST、RPC、参数、返回值与接口契约"),
            new TechEnglishScenarioTagResponse("backend_service", "后端服务", "服务编排、业务规则、事务与领域逻辑"),
            new TechEnglishScenarioTagResponse("database_sql", "数据库 SQL", "查询、索引、事务、表结构与性能"),
            new TechEnglishScenarioTagResponse("cache_middleware", "缓存中间件", "Redis、队列、消息、限流与异步任务"),
            new TechEnglishScenarioTagResponse("network_protocol", "网络协议", "HTTP、TCP、TLS、域名、请求响应"),
            new TechEnglishScenarioTagResponse("security_access", "安全权限", "认证、授权、审计、密钥与访问控制"),
            new TechEnglishScenarioTagResponse("devops_release", "部署发布", "构建、发布、回滚、脚本与上线检查"),
            new TechEnglishScenarioTagResponse("observability", "监控排障", "日志、指标、链路追踪、告警与诊断"),
            new TechEnglishScenarioTagResponse("testing_quality", "测试质量", "单测、集成测试、回归、验收与缺陷"),
            new TechEnglishScenarioTagResponse("performance_tuning", "性能优化", "延迟、吞吐、压测、热点与资源瓶颈"),
            new TechEnglishScenarioTagResponse("cloud_container", "云原生容器", "Docker、K8s、镜像、编排与弹性"),
            new TechEnglishScenarioTagResponse("system_admin", "系统运维", "主机、进程、磁盘、服务与环境配置"),
            new TechEnglishScenarioTagResponse("ai_prompting", "提示词编排", "提示词、上下文、格式控制、模型输出"),
            new TechEnglishScenarioTagResponse("ai_model_usage", "模型调用", "大模型接口、生成、推理、Embedding"),
            new TechEnglishScenarioTagResponse("data_analysis", "数据分析", "统计、指标、图表、洞察与解释"),
            new TechEnglishScenarioTagResponse("customer_support", "客户支持", "答疑、反馈、问题跟进与沟通确认"),
            new TechEnglishScenarioTagResponse("documentation", "文档写作", "说明文档、知识库、README 与规范"),
            new TechEnglishScenarioTagResponse("code_review", "代码评审", "PR、评审意见、修改建议与共识"),
            new TechEnglishScenarioTagResponse("troubleshooting", "故障排查", "问题定位、复现、临时修复与复盘"));

    private TechEnglishScenarioTagCatalog() {
    }

    /** 返回全部固定场景标签。 */
    public static List<TechEnglishScenarioTagResponse> list() {
        return TAGS;
    }

    /** 根据标签代码返回可展示的固定场景标签。 */
    public static List<TechEnglishScenarioTagResponse> responsesOf(List<String> scenarioTags) {
        List<String> normalized = normalize(scenarioTags);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return normalized.stream()
                .map(TechEnglishScenarioTagCatalog::responseOf)
                .toList();
    }

    /** 将前端或 AI 传入的场景标签收敛为已登记的固定标签代码。 */
    public static List<String> normalize(List<String> scenarioTags) {
        if (scenarioTags == null || scenarioTags.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = TAGS.stream()
                .map(TechEnglishScenarioTagResponse::code)
                .collect(Collectors.toSet());
        return new ArrayList<>(new LinkedHashSet<>(scenarioTags).stream()
                .map(TechEnglishScenarioTagCatalog::normalizeCode)
                .filter(allowed::contains)
                .limit(4)
                .toList());
    }

    /** 将固定标签整理成适合提示词的文本。 */
    public static String promptText() {
        return TAGS.stream()
                .map(tag -> "- " + tag.code() + "：" + tag.label() + " - " + tag.description())
                .collect(Collectors.joining("\n"));
    }

    /** 根据标签代码返回中文名称。 */
    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        return responseOf(code).label();
    }

    private static TechEnglishScenarioTagResponse responseOf(String code) {
        if (code == null) {
            return new TechEnglishScenarioTagResponse("", "", "");
        }
        return TAGS.stream()
                .filter(tag -> tag.code().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(new TechEnglishScenarioTagResponse(code, code, ""));
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
