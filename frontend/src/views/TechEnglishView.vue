<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpenText, FileSearch, FileText, Image, Languages, RotateCcw, Search, Type } from '@lucide/vue'
import KnowledgeTagSelector from '../components/KnowledgeTagSelector.vue'
import { fetchTechEnglishCorpus } from '../services/techEnglish'
import type { KnowledgeTagSelection, TechEnglishCorpusPage, TechEnglishCorpusType, TechEnglishDifficulty } from '../types'

const keyword = ref('')
const submittedKeyword = ref('')
const corpusType = ref<TechEnglishCorpusType | ''>('')
const selection = ref<KnowledgeTagSelection>({ module: null, secondary: null, tertiary: null })
const result = ref<TechEnglishCorpusPage>({ total: 0, page: 1, size: 12, totalPages: 0, items: [] })
const loading = ref(true)
const errorMessage = ref('')
const selectorKey = ref(0)

const corpusTypes: Array<{ value: TechEnglishCorpusType; label: string; icon: typeof Type }> = [
  { value: 'VOCABULARY', label: '技术词汇', icon: Type },
  { value: 'SENTENCE', label: '技术语句', icon: Languages },
  { value: 'IMAGE', label: '语料图片', icon: Image },
  { value: 'ARTICLE', label: '英语文章', icon: FileText },
]

const activeTag = computed(() => selection.value.tertiary ?? selection.value.secondary ?? selection.value.module)
const selectionLabel = computed(() => [
  selection.value.module?.name,
  selection.value.secondary?.name,
  selection.value.tertiary?.name,
].filter(Boolean).join(' / '))

/** 转换语料类型展示文案。 */
function typeLabel(value: TechEnglishCorpusType): string {
  return corpusTypes.find((item) => item.value === value)?.label ?? value
}

/** 转换难度展示文案。 */
function difficultyLabel(value: TechEnglishDifficulty): string {
  const labels: Record<TechEnglishDifficulty, string> = { BEGINNER: '入门', INTERMEDIATE: '中级', ADVANCED: '高级' }
  return labels[value] ?? value
}

/** 生成列表卡片摘要。 */
function summaryText(item: TechEnglishCorpusPage['items'][number]): string {
  return item.englishText || item.explanation || item.translationText || item.imageAlt || '打开查看完整语料。'
}

/** 加载技术英语语料列表。 */
async function loadCorpus(page = 1): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    result.value = await fetchTechEnglishCorpus({
      keyword: submittedKeyword.value,
      corpusType: corpusType.value,
      tagId: activeTag.value?.id,
      page,
      size: 12,
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '技术英语语料加载失败'
  } finally {
    loading.value = false
  }
}

/** 提交关键词搜索。 */
function searchCorpus(): void {
  submittedKeyword.value = keyword.value.trim()
  void loadCorpus(1)
}

/** 标签变化后立即应用筛选。 */
function updateSelection(nextSelection: KnowledgeTagSelection): void {
  selection.value = nextSelection
  void loadCorpus(1)
}

/** 切换语料类型筛选。 */
function toggleType(nextType: TechEnglishCorpusType): void {
  corpusType.value = corpusType.value === nextType ? '' : nextType
  void loadCorpus(1)
}

/** 清空全部筛选条件。 */
function resetFilters(): void {
  keyword.value = ''
  submittedKeyword.value = ''
  corpusType.value = ''
  selection.value = { module: null, secondary: null, tertiary: null }
  selectorKey.value += 1
  void loadCorpus(1)
}

onMounted(() => loadCorpus())
</script>

<template>
  <section class="inner-page content-width tech-english-library-page">
    <header class="document-library-heading">
      <div>
        <span>TECHNICAL ENGLISH</span>
        <h1>技术英语</h1>
        <p>按技术主题积累英文词汇、语句、图片语境和文章材料。</p>
      </div>
      <div class="document-count"><strong>{{ result.total }}</strong><span>条已发布语料</span></div>
    </header>

    <form class="document-searchbar" role="search" @submit.prevent="searchCorpus">
      <Search :size="19" />
      <input v-model="keyword" type="search" maxlength="100" placeholder="搜索英文、标题、说明或标签" aria-label="搜索技术英语语料" />
      <button class="primary-button" type="submit">搜索</button>
    </form>

    <section class="tech-english-type-filter" aria-label="语料类型">
      <button v-for="item in corpusTypes" :key="item.value" type="button" :class="{ active: corpusType === item.value }" @click="toggleType(item.value)">
        <component :is="item.icon" :size="17" />
        <span>{{ item.label }}</span>
      </button>
    </section>

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
          <div><span>ENGLISH CORPUS</span><h2>{{ submittedKeyword ? `“${submittedKeyword}”的结果` : '最新语料' }}</h2></div>
          <small v-if="!loading">{{ corpusType ? typeLabel(corpusType) : activeTag ? activeTag.name : '全部语料' }}</small>
        </header>

        <div v-if="loading" class="document-result-state"><FileSearch :size="25" />正在检索语料…</div>
        <div v-else-if="errorMessage" class="document-result-state document-result-state--error">
          <span>{{ errorMessage }}</span><button type="button" @click="loadCorpus(result.page)">重新加载</button>
        </div>
        <div v-else-if="!result.items.length" class="document-result-state">
          <BookOpenText :size="27" /><strong>没有找到匹配语料</strong><span>可以更换关键词、语料类型或知识标签。</span>
        </div>
        <div v-else class="tech-english-result-list">
          <RouterLink v-for="item in result.items" :key="item.id" class="tech-english-result-item" :to="`/tech-english/${item.id}`">
            <div class="tech-english-result-item__meta">
              <span>{{ typeLabel(item.corpusType) }}</span>
              <small>{{ item.scenario || 'general' }} · {{ difficultyLabel(item.difficulty) }}</small>
            </div>
            <h3>{{ item.title }}</h3>
            <p class="tech-english-result-item__english">{{ summaryText(item) }}</p>
            <p v-if="item.translationText" class="tech-english-result-item__translation">{{ item.translationText }}</p>
            <figure v-if="item.corpusType === 'IMAGE' && item.imageUrl">
              <img :src="item.imageUrl" :alt="item.imageAlt || item.title" />
            </figure>
            <footer>
              <div class="document-result-tags"><span v-for="tag in item.knowledgeTags" :key="tag.id">{{ tag.name }}</span></div>
              <i>查看<ArrowRight :size="15" /></i>
            </footer>
          </RouterLink>
        </div>

        <nav v-if="result.totalPages > 1" class="document-pagination" aria-label="技术英语语料分页">
          <button type="button" :disabled="result.page <= 1 || loading" @click="loadCorpus(result.page - 1)">上一页</button>
          <span>{{ result.page }} / {{ result.totalPages }}</span>
          <button type="button" :disabled="result.page >= result.totalPages || loading" @click="loadCorpus(result.page + 1)">下一页</button>
        </nav>
      </main>
    </div>
  </section>
</template>
