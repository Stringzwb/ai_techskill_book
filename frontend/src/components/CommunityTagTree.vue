<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronDown, ChevronRight, FolderTree, Tags, X } from '@lucide/vue'
import type { KnowledgeTagNode } from '../types'

const props = defineProps<{
  tags: KnowledgeTagNode[]
  selectedId?: number
}>()

const emit = defineEmits<{
  select: [tagId: number]
  clear: []
}>()

const expandedIds = ref(new Set<number>())
const selectedName = computed(() => findTag(props.tags, props.selectedId)?.name ?? '')

function findTag(nodes: KnowledgeTagNode[], id?: number): KnowledgeTagNode | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    const child = findTag(node.children, id)
    if (child) return child
  }
  return undefined
}

function toggle(node: KnowledgeTagNode): void {
  const next = new Set(expandedIds.value)
  if (next.has(node.id)) next.delete(node.id)
  else next.add(node.id)
  expandedIds.value = next
}

function select(node: KnowledgeTagNode): void {
  emit('select', node.id)
}

watch(() => props.tags, (nodes) => {
  if (!expandedIds.value.size) expandedIds.value = new Set(nodes.map(node => node.id))
}, { immediate: true })
</script>

<template>
  <section class="community-tag-tree" aria-label="知识标签筛选">
    <header class="community-tag-tree__header">
      <span class="community-tag-tree__icon"><FolderTree :size="18" /></span>
      <div><small>KNOWLEDGE TAGS</small><h2>知识标签</h2></div>
      <span class="community-tag-tree__count">{{ tags.length }}</span>
    </header>

    <div v-if="selectedId" class="community-tag-tree__selection">
      <Tags :size="14" /><span>{{ selectedName }}</span><button type="button" title="清除知识标签筛选" @click="emit('clear')"><X :size="14" /></button>
    </div>

    <p v-if="!tags.length" class="community-tag-tree__empty">暂无可用知识标签。</p>
    <div v-else class="community-tag-tree__nodes">
      <template v-for="module in tags" :key="module.id">
        <div class="community-tag-tree__node community-tag-tree__node--level-1" :class="{ 'is-selected': selectedId === module.id }">
          <button v-if="module.children.length" class="community-tag-tree__toggle" type="button" :title="expandedIds.has(module.id) ? '收起子标签' : '展开子标签'" @click="toggle(module)"><ChevronDown v-if="expandedIds.has(module.id)" :size="15" /><ChevronRight v-else :size="15" /></button>
          <span v-else class="community-tag-tree__toggle-placeholder"></span>
          <button class="community-tag-tree__name" type="button" @click="select(module)">{{ module.name }}</button>
        </div>
        <div v-if="expandedIds.has(module.id)" class="community-tag-tree__branch">
          <template v-for="secondary in module.children" :key="secondary.id">
            <div class="community-tag-tree__node community-tag-tree__node--level-2" :class="{ 'is-selected': selectedId === secondary.id }">
              <button v-if="secondary.children.length" class="community-tag-tree__toggle" type="button" :title="expandedIds.has(secondary.id) ? '收起子标签' : '展开子标签'" @click="toggle(secondary)"><ChevronDown v-if="expandedIds.has(secondary.id)" :size="14" /><ChevronRight v-else :size="14" /></button>
              <span v-else class="community-tag-tree__toggle-placeholder"></span>
              <button class="community-tag-tree__name" type="button" @click="select(secondary)">{{ secondary.name }}</button>
            </div>
            <div v-if="expandedIds.has(secondary.id)" class="community-tag-tree__branch community-tag-tree__branch--leaf">
              <div v-for="tertiary in secondary.children" :key="tertiary.id" class="community-tag-tree__node community-tag-tree__node--level-3" :class="{ 'is-selected': selectedId === tertiary.id }">
                <span class="community-tag-tree__toggle-placeholder"></span><button class="community-tag-tree__name" type="button" @click="select(tertiary)">{{ tertiary.name }}</button>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </section>
</template>
