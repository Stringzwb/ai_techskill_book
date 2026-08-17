<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { FolderTree } from '@lucide/vue'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import type { KnowledgeTagNode, KnowledgeTagSelection } from '../types'

const emit = defineEmits<{
  change: [selection: KnowledgeTagSelection]
}>()

const tree = ref<KnowledgeTagNode[]>([])
const moduleId = ref('')
const secondaryId = ref('')
const tertiaryId = ref('')
const loading = ref(true)
const errorMessage = ref('')

const selectedModule = computed(() => tree.value.find((node) => node.id === Number(moduleId.value)) ?? null)
const secondaryOptions = computed(() => selectedModule.value?.children ?? [])
const selectedSecondary = computed(() => secondaryOptions.value.find((node) => node.id === Number(secondaryId.value)) ?? null)
const tertiaryOptions = computed(() => selectedSecondary.value?.children ?? [])
const selectedTertiary = computed(() => tertiaryOptions.value.find((node) => node.id === Number(tertiaryId.value)) ?? null)

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

/** 一级标签切换后清空下级选择。 */
function changeModule(): void {
  secondaryId.value = ''
  tertiaryId.value = ''
}

/** 二级标签切换后清空三级选择。 */
function changeSecondary(): void {
  tertiaryId.value = ''
}

/** 将当前选择同步给使用该组件的页面。 */
function emitSelection(): void {
  emit('change', {
    module: selectedModule.value,
    secondary: selectedSecondary.value,
    tertiary: selectedTertiary.value,
  })
}

watch([selectedModule, selectedSecondary, selectedTertiary], emitSelection)
onMounted(loadTree)
</script>

<template>
  <section class="knowledge-tag-selector" aria-label="知识标签选择">
    <header>
      <span class="tag-selector-icon"><FolderTree :size="21" /></span>
      <div><small>KNOWLEDGE TAGS</small><h2>知识标签</h2></div>
    </header>

    <p v-if="loading" class="tag-selector-state">正在加载标签树…</p>
    <div v-else-if="errorMessage" class="tag-selector-state tag-selector-state--error">
      <span>{{ errorMessage }}</span><button type="button" @click="loadTree">重新加载</button>
    </div>
    <div v-else class="tag-selector-fields">
      <label><span>知识模块</span>
        <select v-model="moduleId" @change="changeModule">
          <option value="">请选择知识模块</option>
          <option v-for="node in tree" :key="node.id" :value="node.id">{{ node.name }}</option>
        </select>
      </label>
      <label><span>二级标签</span>
        <select v-model="secondaryId" :disabled="!selectedModule" @change="changeSecondary">
          <option value="">请选择二级标签</option>
          <option v-for="node in secondaryOptions" :key="node.id" :value="node.id">{{ node.name }}</option>
        </select>
      </label>
      <label><span>三级标签</span>
        <select v-model="tertiaryId" :disabled="!selectedSecondary">
          <option value="">请选择三级标签</option>
          <option v-for="node in tertiaryOptions" :key="node.id" :value="node.id">{{ node.name }}</option>
        </select>
      </label>
    </div>
  </section>
</template>
