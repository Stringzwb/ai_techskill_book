package com.aitechskill.book.english.domain;

import com.aitechskill.book.english.domain.response.TechEnglishScenarioTagResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 技术英语语料的固定生活场景标签目录。
 */
public final class TechEnglishScenarioTagCatalog {

    private static final List<TechEnglishScenarioTagResponse> TAGS = List.of(
            new TechEnglishScenarioTagResponse("food", "饮食", "吃饭、点餐、烹饪和饮品"),
            new TechEnglishScenarioTagResponse("travel", "旅游", "旅行计划、景点、住宿和行程"),
            new TechEnglishScenarioTagResponse("work", "工作", "职场沟通、会议、协作和任务"),
            new TechEnglishScenarioTagResponse("housing", "居住", "家庭生活、租房、社区和日常起居"),
            new TechEnglishScenarioTagResponse("transport", "交通", "出行、驾车、公共交通和路线"),
            new TechEnglishScenarioTagResponse("study", "学习", "课程、阅读、练习和知识整理"),
            new TechEnglishScenarioTagResponse("shopping", "购物", "购买商品、支付、退换货和服务"),
            new TechEnglishScenarioTagResponse("social", "社交", "朋友、社群、邀请和一般交流"));

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
                .limit(2)
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
