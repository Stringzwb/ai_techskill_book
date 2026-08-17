<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, Clock3, FileSearch, RotateCcw, Search } from '@lucide/vue'
import KnowledgeTagSelector from '../components/KnowledgeTagSelector.vue'
import { fetchDocuments } from '../services/documents'
import type { DocumentPage, KnowledgeTagSelection } from '../types'

const keyword = ref('')
const submittedKeyword = ref('')
const selection = ref<KnowledgeTagSelection>({ module: null, secondary: null, tertiary: null })
const result = ref<DocumentPage>({ total: 0, page: 1, size: 12, totalPages: 0, items: [] })
const loading = ref(true)
const errorMessage = ref('')
const selectorKey = ref(0)

const activeTag = computed(() => selection.value.tertiary ?? selection.value.secondary ?? selection.value.module)
const selectionLabel = computed(() => [
  selection.value.module?.name,
  selection.value.secondary?.name,
  selection.value.tertiary?.name,
].filter(Boolean).join(' / '))

/** 格式化文档发布时间。 */
function formatDate(value: string | null): string {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
}

/** 根据当前关键词、标签和页码加载文档。 */
async function loadDocuments(page = 1): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    result.value = await fetchDocuments({
      keyword: submittedKeyword.value,
      tagId: activeTag.value?.id,
      page,
      size: 12,
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档加载失败'
  } finally {
    loading.value = false
  }
}

/** 提交关键词搜索。 */
function searchDocuments(): void {
  submittedKeyword.value = keyword.value.trim()
  void loadDocuments(1)
}

/** 标签变化后立即应用筛选。 */
function updateSelection(nextSelection: KnowledgeTagSelection): void {
  selection.value = nextSelection
  void loadDocuments(1)
}

/** 清空所有搜索条件。 */
function resetFilters(): void {
  keyword.value = ''
  submittedKeyword.value = ''
  selection.value = { module: null, secondary: null, tertiary: null }
  selectorKey.value += 1
  void loadDocuments(1)
}

onMounted(() => loadDocuments())
</script>

<template>
  <section class="inner-page content-width document-library-page">
    <header class="document-library-heading">
      <div>
        <span>DOCUMENT LIBRARY</span>
        <h1>文档库</h1>
        <p>按主题定位工程知识，阅读经过整理的 Markdown 技术文档。</p>
      </div>
      <div class="document-count"><strong>{{ result.total }}</strong><span>篇已发布文档</span></div>
    </header>

    <form class="document-searchbar" role="search" @submit.prevent="searchDocuments">
      <Search :size="19" />
      <input v-model="keyword" type="search" maxlength="100" placeholder="搜索标题或摘要" aria-label="搜索文档" />
      <button class="primary-button" type="submit">搜索</button>
    </form>

    <div class="document-library-layout">
      <aside class="document-filter-panel">
        <KnowledgeTagSelector :key="selectorKey" @change="updateSelection" />
        <div class="active-filter">
          <small>当前范围</small>
          <strong>{{ selectionLabel || '全部知识标签' }}</strong>
        </div>
        <button class="filter-reset" type="button" @click="resetFilters"><RotateCcw :size="15" />重置筛选</button>
      </aside>

      <main class="document-results" aria-live="polite">
        <header>
          <div><span>SEARCH RESULTS</span><h2>{{ submittedKeyword ? `“${submittedKeyword}”的结果` : '最新文档' }}</h2></div>
          <small v-if="!loading">{{ activeTag ? activeTag.name : '全部标签' }}</small>
        </header>

        <div v-if="loading" class="document-result-state"><FileSearch :size="25" />正在检索文档…</div>
        <div v-else-if="errorMessage" class="document-result-state document-result-state--error">
          <span>{{ errorMessage }}</span><button type="button" @click="loadDocuments(result.page)">重新加载</button>
        </div>
        <div v-else-if="!result.items.length" class="document-result-state">
          <FileSearch :size="27" /><strong>没有找到匹配文档</strong><span>可以更换关键词或选择其他标签。</span>
        </div>
        <div v-else class="document-result-list">
          <RouterLink v-for="item in result.items" :key="item.id" class="document-result-item" :to="`/documents/${item.id}`">
            <div class="document-result-item__meta">
              <span><Clock3 :size="14" />{{ item.readingMinutes }} 分钟</span>
              <time>{{ formatDate(item.publishedAt) }}</time>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary }}</p>
            <footer>
              <div class="document-result-tags"><span v-for="tag in item.tags" :key="tag.id">{{ tag.name }}</span></div>
              <i><BookOpen :size="15" />阅读<ArrowRight :size="15" /></i>
            </footer>
          </RouterLink>
        </div>

        <nav v-if="result.totalPages > 1" class="document-pagination" aria-label="文档分页">
          <button type="button" :disabled="result.page <= 1 || loading" @click="loadDocuments(result.page - 1)">上一页</button>
          <span>{{ result.page }} / {{ result.totalPages }}</span>
          <button type="button" :disabled="result.page >= result.totalPages || loading" @click="loadDocuments(result.page + 1)">下一页</button>
        </nav>
      </main>
    </div>
  </section>
</template>
