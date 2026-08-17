package com.aitechskill.book.tag.service;

import com.aitechskill.book.tag.domain.entity.KnowledgeTagEntity;
import com.aitechskill.book.tag.domain.response.KnowledgeTagTreeResponse;
import com.aitechskill.book.tag.mapper.KnowledgeTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面向主平台的知识标签树查询服务。
 */
@Service
public class KnowledgeTagService {

    private final KnowledgeTagMapper knowledgeTagMapper;

    public KnowledgeTagService(KnowledgeTagMapper knowledgeTagMapper) {
        this.knowledgeTagMapper = knowledgeTagMapper;
    }

    /**
     * 查询有效知识标签树，最多返回三级节点。
     *
     * @return 按同级排序排列的标签树
     */
    @Transactional(readOnly = true)
    public List<KnowledgeTagTreeResponse> getTree() {
        List<KnowledgeTagEntity> nodes = knowledgeTagMapper.selectList(new LambdaQueryWrapper<KnowledgeTagEntity>()
                .eq(KnowledgeTagEntity::getDeleted, 0)
                .orderByAsc(KnowledgeTagEntity::getSortOrder)
                .orderByAsc(KnowledgeTagEntity::getId));
        Map<Long, List<KnowledgeTagEntity>> childrenByParent = nodes.stream()
                .collect(Collectors.groupingBy(KnowledgeTagEntity::getParentId));
        return childrenByParent.getOrDefault(0L, List.of()).stream()
                .map(node -> toTreeNode(node, childrenByParent))
                .toList();
    }

    /** 递归组装固定深度的树节点。 */
    private KnowledgeTagTreeResponse toTreeNode(
            KnowledgeTagEntity node,
            Map<Long, List<KnowledgeTagEntity>> childrenByParent) {
        List<KnowledgeTagEntity> children = childrenByParent.getOrDefault(node.getId(), List.of()).stream()
                .sorted(Comparator.comparing(KnowledgeTagEntity::getSortOrder)
                        .thenComparing(KnowledgeTagEntity::getId))
                .toList();
        List<KnowledgeTagTreeResponse> childResponses = node.getLevel() >= 3
                ? List.of()
                : children.stream().map(child -> toTreeNode(child, childrenByParent)).toList();
        return new KnowledgeTagTreeResponse(
                node.getId(),
                node.getName(),
                node.getLevel(),
                node.getSortOrder(),
                node.getDescription(),
                childResponses);
    }
}
