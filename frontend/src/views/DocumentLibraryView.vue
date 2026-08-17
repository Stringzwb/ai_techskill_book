<script setup lang="ts">
import { computed, ref } from 'vue'
import { FileText } from '@lucide/vue'
import KnowledgeTagSelector from '../components/KnowledgeTagSelector.vue'
import type { KnowledgeTagSelection } from '../types'

const selection = ref<KnowledgeTagSelection>({ module: null, secondary: null, tertiary: null })

// 组合已选择的标签路径，用于后续文档录入与筛选功能。
const selectionLabel = computed(() => [
  selection.value.module?.name,
  selection.value.secondary?.name,
  selection.value.tertiary?.name,
].filter(Boolean).join(' / '))

/** 接收标签选择组件更新的路径。 */
function updateSelection(nextSelection: KnowledgeTagSelection): void {
  selection.value = nextSelection
}
</script>

<template>
  <section class="inner-page content-width document-library-page">
    <div class="page-heading">
      <span>DOCUMENT LIBRARY</span>
      <h1>文档库</h1>
      <p>文档内容将在后续接入；当前可按知识标签树选择归属路径。</p>
    </div>

    <KnowledgeTagSelector @change="updateSelection" />

    <section class="document-library-empty" aria-live="polite">
      <span><FileText :size="25" /></span>
      <div>
        <small>当前选择</small>
        <strong>{{ selectionLabel || '尚未选择知识标签' }}</strong>
      </div>
    </section>
  </section>
</template>
