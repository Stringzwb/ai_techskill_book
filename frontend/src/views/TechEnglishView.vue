<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowRight, BookOpenText, FileSearch, FileText, Image, Languages, LogIn, Plus, RotateCcw, Search, Send, SlidersHorizontal, Type, UploadCloud, X } from '@lucide/vue'
import KnowledgeTagSelector from '../components/KnowledgeTagSelector.vue'
import { createTechEnglishCorpus, fetchTechEnglishCorpus } from '../services/techEnglish'
import { authStore } from '../stores/auth'
import type { KnowledgeTagSelection, TechEnglishCorpusCreatePayload, TechEnglishCorpusPage, TechEnglishCorpusType, TechEnglishDifficulty } from '../types'

const keyword = ref('')
const submittedKeyword = ref('')
const corpusType = ref<TechEnglishCorpusType | ''>('')
const selection = ref<KnowledgeTagSelection>({ module: null, secondary: null, tertiary: null })
const result = ref<TechEnglishCorpusPage>({ total: 0, page: 1, size: 12, totalPages: 0, items: [] })
const loading = ref(true)
const errorMessage = ref('')
const selectorKey = ref(0)
const showComposer = ref(false)
const submitting = ref(false)
const submitMessage = ref('')
const submitError = ref('')
const createSelectorKey = ref(0)
const createSelection = ref<KnowledgeTagSelection>({ module: null, secondary: null, tertiary: null })
const imageInput = ref<HTMLInputElement | null>(null)
const form = reactive<TechEnglishCorpusCreatePayload>({
  corpusType: 'VOCABULARY',
  title: '',
  englishText: '',
  phonetic: '',
  explanation: '',
  articleMarkdown: '',
  imageFile: null,
  imageAlt: '',
  sourceName: '',
  sourceUrl: '',
  scenario: '',
  difficulty: 'INTERMEDIATE',
  translationText: '',
  tagIds: [],
})

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
const imageFileName = computed(() => form.imageFile?.name ?? '')
const createSelectionLabel = computed(() => [
  createSelection.value.module?.name,
  createSelection.value.secondary?.name,
  createSelection.value.tertiary?.name,
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

/** 重置主站轻收录表单。 */
function resetCreateForm(): void {
  form.corpusType = 'VOCABULARY'
  form.title = ''
  form.englishText = ''
  form.phonetic = ''
  form.explanation = ''
  form.articleMarkdown = ''
  form.imageFile = null
  form.imageAlt = ''
  form.sourceName = ''
  form.sourceUrl = ''
  form.scenario = ''
  form.difficulty = 'INTERMEDIATE'
  form.translationText = ''
  form.tagIds = []
  createSelection.value = { module: null, secondary: null, tertiary: null }
  createSelectorKey.value += 1
  if (imageInput.value) imageInput.value.value = ''
}

/** 切换收录表单语料类型，并清空类型专属输入。 */
function setCreateType(nextType: TechEnglishCorpusType): void {
  form.corpusType = nextType
  form.englishText = ''
  form.phonetic = ''
  form.articleMarkdown = ''
  form.imageFile = null
  form.imageAlt = ''
  form.sourceName = ''
  form.sourceUrl = ''
  if (imageInput.value) imageInput.value.value = ''
}

/** 收录表单的知识标签选择只绑定最深一级。 */
function updateCreateSelection(nextSelection: KnowledgeTagSelection): void {
  createSelection.value = nextSelection
  const tag = nextSelection.tertiary ?? nextSelection.secondary ?? nextSelection.module
  form.tagIds = tag ? [tag.id] : []
}

/** 打开主站轻收录面板。 */
function openCreateForm(): void {
  submitError.value = ''
  submitMessage.value = ''
  showComposer.value = true
}

/** 接收图片语料文件。 */
function chooseImage(event: Event): void {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  form.imageFile = file
}

/** 提交主站轻收录语料。 */
async function submitCorpus(): Promise<void> {
  submitError.value = ''
  submitMessage.value = ''
  if (!form.tagIds.length) {
    submitError.value = '请选择一个知识标签'
    return
  }
  if ((form.corpusType === 'VOCABULARY' || form.corpusType === 'SENTENCE') && !form.englishText?.trim()) {
    submitError.value = '请填写英文内容'
    return
  }
  if (form.corpusType === 'IMAGE' && !form.imageFile) {
    submitError.value = '请上传图片文件'
    return
  }
  if (form.corpusType === 'ARTICLE' && !form.articleMarkdown?.trim() && !form.sourceUrl?.trim()) {
    submitError.value = '请填写文章正文或文章链接'
    return
  }
  submitting.value = true
  try {
    await createTechEnglishCorpus({
      ...form,
      tagIds: [...form.tagIds],
    })
    resetCreateForm()
    showComposer.value = false
    submitMessage.value = '语料已添加'
    await loadCorpus(1)
  } catch (error) {
    submitError.value = error instanceof Error ? error.message : '语料添加失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadCorpus())
</script>

<template>
  <section class="tech-english-page content-width">
    <header class="tech-english-hero">
      <div class="tech-english-hero__copy">
        <span>TECHNICAL ENGLISH</span>
        <h1>技术英语语料库</h1>
        <p>词汇、句子、图片语境和英语文章统一沉淀到知识标签树。</p>
      </div>
      <div class="tech-english-heading-actions">
        <div class="tech-english-stat"><strong>{{ result.total }}</strong><span>已发布</span></div>
        <button v-if="authStore.state.user" class="primary-button" type="button" @click="openCreateForm">
          <Plus :size="18" />添加语料
        </button>
        <RouterLink v-else class="primary-button" to="/login"><LogIn :size="18" />登录后添加</RouterLink>
      </div>
    </header>

    <Teleport to="body">
      <div v-if="showComposer" class="composer-backdrop" @click.self="showComposer = false">
        <section class="share-composer tech-english-composer" role="dialog" aria-modal="true" aria-labelledby="tech-english-composer-title">
          <header>
            <div>
              <span>NEW CORPUS</span>
              <h2 id="tech-english-composer-title">添加语料</h2>
            </div>
            <button class="icon-button" type="button" title="关闭添加面板" aria-label="关闭添加面板" @click="showComposer = false">
              <X :size="18" />
            </button>
          </header>
          <form @submit.prevent="submitCorpus">
            <div class="share-type-tabs">
              <button v-for="item in corpusTypes" :key="item.value" type="button" :class="{ active: form.corpusType === item.value }" @click="setCreateType(item.value)">
                <component :is="item.icon" :size="16" />{{ item.label }}
              </button>
            </div>
            <label>标题
              <input v-model.trim="form.title" maxlength="160" required placeholder="例如 ArrayList 或 Spring Boot error handling" />
            </label>
            <KnowledgeTagSelector :key="createSelectorKey" @change="updateCreateSelection" />
            <small class="tech-english-composer__tag">{{ createSelectionLabel || '请选择一个知识标签' }}</small>
            <label v-if="form.corpusType === 'VOCABULARY'">单词或术语
              <input v-model.trim="form.englishText" maxlength="20000" required placeholder="例如 idempotent" />
            </label>
            <label v-if="form.corpusType === 'VOCABULARY'">发音提示
              <input v-model.trim="form.phonetic" maxlength="120" placeholder="/aɪˈdempətənt/" />
            </label>
            <label v-if="form.corpusType === 'SENTENCE'">技术语句
              <textarea v-model="form.englishText" required maxlength="20000" placeholder="粘贴一句技术英文表达"></textarea>
            </label>
            <template v-if="form.corpusType === 'ARTICLE'">
              <label>文章正文
                <textarea v-model="form.articleMarkdown" maxlength="50000" placeholder="可粘贴文章摘录或 Markdown 正文"></textarea>
              </label>
              <label>文章链接
                <input v-model.trim="form.sourceUrl" type="url" maxlength="2048" placeholder="https://example.com/article" />
              </label>
              <label>来源名称
                <input v-model.trim="form.sourceName" maxlength="120" placeholder="例如 MDN、AWS Docs" />
              </label>
            </template>
            <template v-if="form.corpusType === 'IMAGE'">
              <label>图片文件
                <button class="tech-english-upload" type="button" @click="imageInput?.click()">
                  <UploadCloud :size="18" />
                  <span>{{ imageFileName || '选择 JPG、PNG、GIF 或 WebP 图片' }}</span>
                </button>
                <input ref="imageInput" class="avatar-file-input" type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="chooseImage" />
              </label>
              <label>图片说明
                <textarea v-model="form.imageAlt" maxlength="300" placeholder="说明图片中的技术语境"></textarea>
              </label>
            </template>
            <div class="tech-english-composer__compact">
              <label>场景
                <input v-model.trim="form.scenario" maxlength="80" placeholder="backend / ai / database" />
              </label>
              <label>难度
                <select v-model="form.difficulty">
                  <option value="BEGINNER">入门</option>
                  <option value="INTERMEDIATE">中级</option>
                  <option value="ADVANCED">高级</option>
                </select>
              </label>
            </div>
            <label>说明
              <textarea v-model="form.explanation" maxlength="1000" placeholder="用法、语境或记忆提示"></textarea>
            </label>
            <label>中文参考
              <textarea v-model="form.translationText" maxlength="5000" placeholder="可选，不接翻译 API"></textarea>
            </label>
            <footer>
              <span v-if="submitError" class="form-error">{{ submitError }}</span>
              <button class="primary-button" type="submit" :disabled="submitting">
                <Send :size="16" />{{ submitting ? '添加中...' : '添加语料' }}
              </button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>

    <p v-if="submitMessage" class="share-message">{{ submitMessage }}</p>

    <section class="tech-english-command">
      <form class="tech-english-search" role="search" @submit.prevent="searchCorpus">
        <Search :size="19" />
        <input v-model="keyword" type="search" maxlength="100" placeholder="搜索英文、标题、说明或标签" aria-label="搜索技术英语语料" />
        <button class="primary-button" type="submit">搜索</button>
      </form>
      <div class="tech-english-type-filter" aria-label="语料类型">
        <button v-for="item in corpusTypes" :key="item.value" type="button" :class="{ active: corpusType === item.value }" @click="toggleType(item.value)">
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </section>

    <div class="tech-english-workspace">
      <aside class="tech-english-sidebar">
        <div class="tech-english-sidebar__title">
          <SlidersHorizontal :size="16" />
          <span>筛选</span>
        </div>
        <KnowledgeTagSelector :key="selectorKey" @change="updateSelection" />
        <div class="active-filter">
          <small>当前范围</small>
          <strong>{{ selectionLabel || '全部知识标签' }}</strong>
        </div>
        <button class="filter-reset" type="button" @click="resetFilters"><RotateCcw :size="15" />重置筛选</button>
      </aside>

      <main class="tech-english-results" aria-live="polite">
        <header class="tech-english-results__header">
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
