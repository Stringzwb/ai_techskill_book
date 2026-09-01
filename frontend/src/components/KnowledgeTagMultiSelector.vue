<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { FolderTree } from '@lucide/vue'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import type { KnowledgeTagNode } from '../types'

const emit = defineEmits<{
  change: [tagIds: number[]]
}>()

const tree = ref<KnowledgeTagNode[]>([])
const selectedIds = ref<number[]>([])
const loading = ref(true)
const errorMessage = ref('')

interface FlatTagOption {
  id: number
  name: string
  level: 1 | 2 | 3
  path: string
}

const flatTags = computed<FlatTagOption[]>(() => {
  const options: FlatTagOption[] = []
  const walk = (nodes: KnowledgeTagNode[], parents: string[] = []) => {
    nodes.forEach((node) => {
      const path = [...parents, node.name]
      options.push({ id: node.id, name: node.name, level: node.level, path: path.join(' / ') })
      walk(node.children, path)
    })
  }
  walk(tree.value)
  return options
})

/** 加载后台维护的知识标签树。 */
async function loadTree(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    tree.value = await fetchKnowledgeTagTree()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '知识标签加载失败'
  } finally {
    loading.value = false
  }
}

/** 将原生多选框的值转换为数字标签 ID。 */
function emitSelection(): void {
  emit('change', selectedIds.value.map(Number).filter((id) => Number.isSafeInteger(id) && id > 0))
}

watch(selectedIds, emitSelection)
onMounted(loadTree)
</script>

<template>
  <section class="knowledge-tag-multi-selector" aria-label="知识标签筛选">
    <header>
      <span class="tag-selector-icon"><FolderTree :size="19" /></span>
      <div><small>KNOWLEDGE TAGS</small><h2>知识标签</h2></div>
    </header>

    <p v-if="loading" class="tag-selector-state">正在加载标签...</p>
    <div v-else-if="errorMessage" class="tag-selector-state tag-selector-state--error">
      <span>{{ errorMessage }}</span><button type="button" @click="loadTree">重新加载</button>
    </div>
    <label v-else class="knowledge-tag-multi-selector__field">
      <span>可多选标签</span>
      <select v-model="selectedIds" multiple size="8" aria-label="多选知识标签">
        <option v-for="tag in flatTags" :key="tag.id" :value="tag.id">
          {{ `${'  '.repeat(tag.level - 1)}${tag.name}` }}
        </option>
      </select>
      <small>{{ selectedIds.length ? `已选择 ${selectedIds.length} 个标签` : '未选择时显示全部语料' }}</small>
    </label>
  </section>
</template>
