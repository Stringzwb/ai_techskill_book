<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ArrowRight, BookOpenText, Check, FileSearch, FileText, Image, Languages, LogIn, Plus, RotateCcw, Search, Send, SlidersHorizontal, Sparkles, Trash2, Type, UploadCloud, X } from '@lucide/vue'
import { useRoute } from 'vue-router'
import KnowledgeTagSelector from '../components/KnowledgeTagSelector.vue'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import { confirmTechEnglishScreenshotImport, createTechEnglishCorpus, fetchTechEnglishCorpus, importTechEnglishScreenshots } from '../services/techEnglish'
import { authStore } from '../stores/auth'
import type { KnowledgeTagNode, KnowledgeTagSelection, TechEnglishAiConfirmPayload, TechEnglishAiImportResponse, TechEnglishAiItemTagAssignment, TechEnglishAiRecognitionResponse, TechEnglishCorpusCreatePayload, TechEnglishCorpusPage, TechEnglishCorpusType, TechEnglishDifficulty, TechEnglishVocabularyExampleInput } from '../types'

interface FlatTagOption {
  id: number
  name: string
  level: 1 | 2 | 3
  path: string
}

interface AiImagePreview {
  id: string
  file: File
  url: string
}

const AI_MAX_IMAGES = 20
const AI_CHUNK_SIZE = 5

const keyword = ref('')
const route = useRoute()
const isAiImportPage = computed(() => route.name === 'tech-english-import')
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
const tagTree = ref<KnowledgeTagNode[]>([])
const tagSearch = ref('')
const tagLoading = ref(false)
const tagError = ref('')
const selectedCreateTagId = ref<number | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const aiImageInput = ref<HTMLInputElement | null>(null)
const aiScenario = ref('')
const aiExampleCount = ref(2)
const aiTagSearch = ref('')
const selectedAiTagId = ref<number | null>(null)
const aiImages = ref<AiImagePreview[]>([])
const aiDragActive = ref(false)
const aiImporting = ref(false)
const aiConfirming = ref(false)
const aiImportError = ref('')
const aiSessionUuid = ref('')
const aiRecognitionResults = ref<TechEnglishAiRecognitionResponse[]>([])
const aiImportResults = ref<TechEnglishAiImportResponse[]>([])
const aiItemTagAssignments = reactive<Record<string, number[]>>({})
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
  vocabularyExamples: [{ englishText: '', translationText: '' }],
  syncExamplesToSentences: true,
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
const flatTags = computed<FlatTagOption[]>(() => {
  const options: FlatTagOption[] = []
  const walk = (nodes: KnowledgeTagNode[], parents: string[] = []) => {
    nodes.forEach((node) => {
      const pathNames = [...parents, node.name]
      options.push({ id: node.id, name: node.name, level: node.level, path: pathNames.join(' / ') })
      walk(node.children, pathNames)
    })
  }
  walk(tagTree.value)
  return options
})
const selectedCreateTag = computed(() => flatTags.value.find((tag) => tag.id === selectedCreateTagId.value) ?? null)
const filteredCreateTags = computed(() => {
  const query = tagSearch.value.trim().toLowerCase()
  const source = query
    ? flatTags.value.filter((tag) => `${tag.name} ${tag.path}`.toLowerCase().includes(query))
    : flatTags.value
  return source.slice(0, 24)
})
const selectedAiTag = computed(() => flatTags.value.find((tag) => tag.id === selectedAiTagId.value) ?? null)
const filteredAiTags = computed(() => {
  const query = aiTagSearch.value.trim().toLowerCase()
  const source = query
    ? flatTags.value.filter((tag) => `${tag.name} ${tag.path}`.toLowerCase().includes(query))
    : flatTags.value
  return source.slice(0, 18)
})
const aiRemainingImages = computed(() => Math.max(0, AI_MAX_IMAGES - aiImages.value.length))
const aiRecognitionCount = computed(() => aiRecognitionResults.value.length)
const aiRecognizedItemCount = computed(() => aiRecognitionResults.value.reduce((total, batch) => total + batch.items.length, 0))
const aiImportedCount = computed(() => aiImportResults.value.reduce((total, batch) => total + batch.createdCount, 0))

/** 根据标签 ID 返回可读路径。 */
function tagPath(tagId: number): string {
  return flatTags.value.find((tag) => tag.id === tagId)?.path ?? `#${tagId}`
}

/** 生成分组上传的会话标识。 */
function createAiSessionUuid(): string {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

/** 按固定长度切分截图。 */
function splitAiImages(images: AiImagePreview[], size: number): AiImagePreview[][] {
  const chunks: AiImagePreview[][] = []
  for (let index = 0; index < images.length; index += size) {
    chunks.push(images.slice(index, index + size))
  }
  return chunks
}

/** 清空某条识图结果的标签。 */
function clearAiItemAssignment(itemKey: string): void {
  delete aiItemTagAssignments[itemKey]
}

/** 为某条识图结果追加当前选中的标签。 */
function assignCurrentTagToAiItem(itemKey: string): void {
  if (!selectedAiTagId.value) return
  const next = new Set(aiItemTagAssignments[itemKey] ?? [])
  next.add(selectedAiTagId.value)
  aiItemTagAssignments[itemKey] = Array.from(next)
}

/** 将当前标签填充到所有尚未标注的识图结果。 */
function fillCurrentTagToUnassignedItems(): void {
  if (!selectedAiTagId.value) return
  aiRecognitionResults.value.forEach((batch) => {
    batch.items.forEach((item) => {
      if (!(aiItemTagAssignments[item.itemKey]?.length ?? 0)) {
        aiItemTagAssignments[item.itemKey] = [selectedAiTagId.value as number]
      }
    })
  })
}

/** 构建提交给后端的标签映射。 */
function buildItemTagAssignments(batch: TechEnglishAiRecognitionResponse): TechEnglishAiConfirmPayload['itemTagAssignments'] {
  return batch.items.map((item) => ({
    itemKey: item.itemKey,
    tagIds: [...(aiItemTagAssignments[item.itemKey] ?? [])],
  }))
}

/** 检查单条识图结果是否已完成标签选择。 */
function hasItemTags(itemKey: string): boolean {
  return Boolean(aiItemTagAssignments[itemKey]?.length)
}

/** 判断单个识图批次是否已完成标签选择。 */
function isBatchTagged(batch: TechEnglishAiRecognitionResponse): boolean {
  return batch.items.every((item) => hasItemTags(item.itemKey))
}

/** 判断全部识图结果是否都已完成标签选择。 */
const allAiItemsTagged = computed(() => aiRecognitionResults.value.every((batch) => batch.items.every((item) => hasItemTags(item.itemKey))))

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

/** 加载添加表单使用的知识标签。 */
async function loadCreateTags(): Promise<void> {
  if (tagTree.value.length || tagLoading.value) return
  tagLoading.value = true
  tagError.value = ''
  try {
    tagTree.value = await fetchKnowledgeTagTree()
  } catch (error) {
    tagError.value = error instanceof Error ? error.message : '知识标签加载失败'
  } finally {
    tagLoading.value = false
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
  form.vocabularyExamples = [{ englishText: '', translationText: '' }]
  form.syncExamplesToSentences = true
  tagSearch.value = ''
  selectedCreateTagId.value = null
  if (imageInput.value) imageInput.value.value = ''
}

/** 切换收录表单语料类型，并清空类型专属输入。 */
function setCreateType(nextType: TechEnglishCorpusType): void {
  form.corpusType = nextType
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
  form.translationText = ''
  if (nextType !== 'VOCABULARY') {
    form.vocabularyExamples = [{ englishText: '', translationText: '' }]
    form.syncExamplesToSentences = false
  } else {
    form.syncExamplesToSentences = true
  }
  if (imageInput.value) imageInput.value.value = ''
}

/** 选择收录语料要绑定的知识标签。 */
function selectCreateTag(tag: FlatTagOption): void {
  selectedCreateTagId.value = tag.id
  form.tagIds = [tag.id]
  tagSearch.value = tag.path
}

/** 选择截图识别结果要绑定的知识标签。 */
function selectAiTag(tag: FlatTagOption): void {
  selectedAiTagId.value = tag.id
  aiTagSearch.value = tag.path
}

/** 将选择或拖入的图片追加到待识别队列。 */
function addAiImages(files: File[]): void {
  aiImportError.value = ''
  aiRecognitionResults.value = []
  aiImportResults.value = []
  Object.keys(aiItemTagAssignments).forEach((key) => delete aiItemTagAssignments[key])
  const supported = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])
  const typeValidFiles = files.filter((file) => supported.has(file.type))
  const validFiles = typeValidFiles.filter((file) => file.size <= 10 * 1024 * 1024)
  if (typeValidFiles.length !== files.length) {
    aiImportError.value = '仅支持 JPG、PNG、GIF 或 WebP 图片'
  } else if (validFiles.length !== typeValidFiles.length) {
    aiImportError.value = '每张截图不能超过 10MB'
  }
  if (!validFiles.length) return
  const existing = new Set(aiImages.value.map((item) => `${item.file.name}:${item.file.size}:${item.file.lastModified}`))
  const uniqueFiles = validFiles.filter((file) => !existing.has(`${file.name}:${file.size}:${file.lastModified}`))
  const accepted = uniqueFiles.slice(0, aiRemainingImages.value)
  aiImages.value = [
    ...aiImages.value,
    ...accepted.map((file, index) => ({
      id: `${file.name}-${file.size}-${file.lastModified}-${index}`,
      file,
      url: URL.createObjectURL(file),
    })),
  ]
  if (uniqueFiles.length > accepted.length) {
    aiImportError.value = `单次最多上传 ${AI_MAX_IMAGES} 张截图，超出的图片未加入队列`
  }
}

/** 接收文件选择器中的多张截图。 */
function chooseAiImages(event: Event): void {
  const input = event.target as HTMLInputElement
  addAiImages(Array.from(input.files ?? []))
  input.value = ''
}

/** 接收拖放到上传区域的截图。 */
function dropAiImages(event: DragEvent): void {
  aiDragActive.value = false
  addAiImages(Array.from(event.dataTransfer?.files ?? []))
}

/** 从截图识别队列移除一张图片。 */
function removeAiImage(id: string): void {
  const target = aiImages.value.find((item) => item.id === id)
  if (target) URL.revokeObjectURL(target.url)
  aiImages.value = aiImages.value.filter((item) => item.id !== id)
  aiRecognitionResults.value = []
  aiImportResults.value = []
  Object.keys(aiItemTagAssignments).forEach((key) => delete aiItemTagAssignments[key])
}

/** 清空截图识别队列及上一次结果。 */
function resetAiImport(): void {
  aiImages.value.forEach((item) => URL.revokeObjectURL(item.url))
  aiImages.value = []
  aiScenario.value = ''
  aiExampleCount.value = 2
  aiTagSearch.value = ''
  selectedAiTagId.value = null
  aiImportError.value = ''
  aiRecognitionResults.value = []
  aiImportResults.value = []
  Object.keys(aiItemTagAssignments).forEach((key) => delete aiItemTagAssignments[key])
  aiSessionUuid.value = ''
  Object.keys(aiItemTagAssignments).forEach((key) => delete aiItemTagAssignments[key])
  if (aiImageInput.value) aiImageInput.value.value = ''
}

/** 上传截图并调用 AI 生成等待用户确认的识别草稿。 */
async function submitAiImport(): Promise<void> {
  aiImportError.value = ''
  aiRecognitionResults.value = []
  aiImportResults.value = []
  if (!authStore.state.user) {
    aiImportError.value = '请先登录后再使用截图智能入库'
    return
  }
  if (!aiImages.value.length) {
    aiImportError.value = '请至少上传一张截图'
    return
  }
  const chunks = splitAiImages(aiImages.value, AI_CHUNK_SIZE)
  if (chunks.length > 4) {
    aiImportError.value = '最多支持 4 组并发识别，请把截图控制在 20 张以内'
    return
  }
  if (!Number.isInteger(aiExampleCount.value) || aiExampleCount.value < 0 || aiExampleCount.value > 5) {
    aiImportError.value = '例句数量必须在 0 到 5 之间'
    return
  }
  aiImporting.value = true
  try {
    const sessionUuid = createAiSessionUuid()
    aiSessionUuid.value = sessionUuid
    const settled = await Promise.allSettled(chunks.map((chunk, index) => importTechEnglishScreenshots({
      sessionUuid,
      chunkIndex: index + 1,
      chunkCount: chunks.length,
      scenario: aiScenario.value.trim(),
      exampleCount: aiExampleCount.value,
      images: chunk.map((item) => item.file),
    })))
    const results = settled
      .filter((item): item is PromiseFulfilledResult<TechEnglishAiRecognitionResponse> => item.status === 'fulfilled')
      .map((item) => item.value)
    const failures = settled
      .map((item, index) => (item.status === 'rejected' ? `第 ${index + 1} 组：${item.reason instanceof Error ? item.reason.message : '识别失败'}` : ''))
      .filter(Boolean)
    aiRecognitionResults.value = results
    results.forEach((batch) => {
      batch.items.forEach((item) => {
        if (!aiItemTagAssignments[item.itemKey]) {
          aiItemTagAssignments[item.itemKey] = []
        }
      })
    })
    if (failures.length) {
      aiImportError.value = failures.join('；')
    }
    selectedAiTagId.value = null
    aiTagSearch.value = ''
    if (!results.length) {
      aiImportError.value = failures.length ? failures.join('；') : '没有任何识别分组成功'
      return
    }
  } catch (error) {
    aiImportError.value = error instanceof Error ? error.message : '截图识别失败，请稍后重试'
  } finally {
    aiImporting.value = false
  }
}

/** 用户为单个识图批次选择标签后，确认保存该批次截图和语料。 */
async function confirmAiBatch(batch: TechEnglishAiRecognitionResponse): Promise<void> {
  aiImportError.value = ''
  if (!isBatchTagged(batch)) {
    aiImportError.value = `请先为第 ${batch.chunkIndex} 组的每条识图结果选择知识标签`
    return
  }
  aiConfirming.value = true
  try {
    const result = await confirmTechEnglishScreenshotImport({
      batchUuid: batch.batchUuid,
      itemTagAssignments: buildItemTagAssignments(batch),
      images: aiImages.value.slice(
        (batch.chunkIndex - 1) * AI_CHUNK_SIZE,
        (batch.chunkIndex - 1) * AI_CHUNK_SIZE + batch.imageCount,
      ).map((item) => item.file),
    })
    aiImportResults.value = [...aiImportResults.value, result]
    aiRecognitionResults.value = aiRecognitionResults.value.filter((item) => item.batchUuid !== batch.batchUuid)
  } catch (error) {
    aiImportError.value = error instanceof Error ? error.message : `第 ${batch.chunkIndex} 组入库失败，请稍后重试`
  } finally {
    aiConfirming.value = false
  }
}

/** 打开主站轻收录面板。 */
function openCreateForm(): void {
  submitError.value = ''
  submitMessage.value = ''
  showComposer.value = true
  void loadCreateTags()
}

/** 接收图片语料文件。 */
function chooseImage(event: Event): void {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  form.imageFile = file
}

/** 新增一组词汇例句。 */
function addVocabularyExample(): void {
  form.vocabularyExamples = [...(form.vocabularyExamples ?? []), { englishText: '', translationText: '' }]
}

/** 移除一组词汇例句。 */
function removeVocabularyExample(index: number): void {
  const examples = [...(form.vocabularyExamples ?? [])]
  examples.splice(index, 1)
  form.vocabularyExamples = examples.length ? examples : [{ englishText: '', translationText: '' }]
}

/** 返回已填写的词汇例句。 */
function filledVocabularyExamples(): TechEnglishVocabularyExampleInput[] {
  return (form.vocabularyExamples ?? [])
    .map((example) => ({
      englishText: example.englishText.trim(),
      translationText: example.translationText.trim(),
    }))
    .filter((example) => example.englishText)
}

/** 提交主站轻收录语料。 */
async function submitCorpus(): Promise<void> {
  submitError.value = ''
  submitMessage.value = ''
  if (!form.tagIds.length) {
    submitError.value = '请选择知识标签'
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
      title: form.corpusType === 'VOCABULARY' ? '' : form.title,
      tagIds: [...form.tagIds],
      vocabularyExamples: form.corpusType === 'VOCABULARY' ? filledVocabularyExamples() : [],
      syncExamplesToSentences: form.corpusType === 'VOCABULARY' && Boolean(form.syncExamplesToSentences),
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

onMounted(() => {
  if (!isAiImportPage.value) void loadCorpus()
  void loadCreateTags()
})

watch(isAiImportPage, (nextValue) => {
  if (!nextValue) void loadCorpus()
})

onBeforeUnmount(() => {
  aiImages.value.forEach((item) => URL.revokeObjectURL(item.url))
})
</script>

<template>
  <section class="tech-english-page content-width">
    <header class="tech-english-hero">
      <div class="tech-english-hero__copy">
        <span>{{ isAiImportPage ? 'AI SCREENSHOT IMPORT' : 'TECHNICAL ENGLISH' }}</span>
        <h1>{{ isAiImportPage ? 'AI 截图识别' : '技术英语语料库' }}</h1>
        <p v-if="isAiImportPage">AI 自行判别生词与经典句子，分别按默认配置生成完整学习信息。</p>
        <p v-else>浏览、搜索与维护已发布的技术英语语料。</p>
      </div>
      <div class="tech-english-heading-actions">
        <RouterLink v-if="isAiImportPage" class="secondary-button" to="/tech-english"><BookOpenText :size="17" />返回语料库</RouterLink>
        <template v-else>
          <div class="tech-english-stat"><strong>{{ result.total }}</strong><span>已发布</span></div>
          <RouterLink class="primary-button" to="/tech-english/import"><Sparkles :size="17" />AI 识图入库</RouterLink>
          <RouterLink class="secondary-button" to="/tech-english/history"><FileSearch :size="17" />识图记录</RouterLink>
        </template>
        <button v-if="!isAiImportPage && authStore.state.user" class="secondary-button" type="button" @click="openCreateForm">
          <Plus :size="18" />添加语料
        </button>
        <RouterLink v-else-if="!isAiImportPage" class="secondary-button" to="/login"><LogIn :size="18" />登录后添加</RouterLink>
      </div>
    </header>

    <section v-if="isAiImportPage" class="tech-english-ai-studio" aria-labelledby="tech-english-ai-title">
      <header class="tech-english-ai-studio__header">
        <div>
          <span><Sparkles :size="14" /> AI SCREENSHOT IMPORT</span>
          <h2 id="tech-english-ai-title">上传与自动分类</h2>
          <p>最多 20 张截图，每组 5 张，自动拆成最多 4 个并发识别任务。每条结果都要单独选标签，确认后才会入库。</p>
        </div>
        <div class="tech-english-ai-source">
          <small>当前来源</small>
          <strong>薄荷阅读</strong>
          <RouterLink class="secondary-button" to="/tech-english/history"><FileSearch :size="16" />识图记录</RouterLink>
        </div>
      </header>

      <form class="tech-english-ai-layout" @submit.prevent="submitAiImport">
        <aside class="tech-english-ai-modes">
          <small>01 · AI 自动判别</small>
          <div class="tech-english-ai-auto-mode">
            <Sparkles :size="20" />
            <span><strong>两套默认配置</strong><small>AI 按截图内容决定使用生词或句子配置</small></span>
            <Check :size="16" />
          </div>
          <div class="tech-english-ai-config-summary">
            <p><Type :size="15" /><span><strong>生词配置</strong>词性、释义、英美音标与例句</span></p>
            <p><Languages :size="15" /><span><strong>句子配置</strong>翻译、重点词汇、经典句式与例句</span></p>
          </div>
          <div class="tech-english-ai-flow">
            <span>1</span><p>上传截图并自动切成 5 张一组</p>
            <span>2</span><p>查看每条识别结果并单独选标签</p>
            <span>3</span><p>一次确认后完成入库</p>
          </div>
        </aside>

        <main class="tech-english-ai-panel">
          <div class="tech-english-ai-panel__heading">
            <div><small>02 · 上传阅读截图</small><strong>{{ aiImages.length }} / 20 张</strong></div>
            <button v-if="aiImages.length" type="button" @click="resetAiImport"><RotateCcw :size="14" />重新选择</button>
          </div>

          <button
            class="tech-english-ai-dropzone"
            :class="{ 'is-dragging': aiDragActive }"
            type="button"
            @click="aiImageInput?.click()"
            @dragenter.prevent="aiDragActive = true"
            @dragover.prevent="aiDragActive = true"
            @dragleave.prevent="aiDragActive = false"
            @drop.prevent="dropAiImages"
          >
            <UploadCloud :size="27" />
            <span><strong>拖入截图，或点击选择图片</strong><small>支持 JPG、PNG、GIF、WebP；还可添加 {{ aiRemainingImages }} 张</small></span>
          </button>
          <input ref="aiImageInput" class="avatar-file-input" type="file" multiple accept="image/jpeg,image/png,image/gif,image/webp" @change="chooseAiImages" />

          <div v-if="aiImages.length" class="tech-english-ai-thumbnails">
            <figure v-for="(imageItem, index) in aiImages" :key="imageItem.id">
              <img :src="imageItem.url" :alt="`待识别截图 ${index + 1}`" />
              <figcaption><span>{{ index + 1 }}</span><small>{{ imageItem.file.name }}</small></figcaption>
              <button type="button" title="移除这张截图" :aria-label="`移除第 ${index + 1 } 张截图`" @click="removeAiImage(imageItem.id)"><X :size="15" /></button>
            </figure>
          </div>

          <section v-if="!aiRecognitionResults.length && !aiImportResults.length" class="tech-english-ai-settings">
            <div class="tech-english-ai-settings__heading"><small>03 · 设置识别规则</small><span>此阶段无需选择标签</span></div>
            <div class="tech-english-ai-settings__grid tech-english-ai-settings__grid--recognition">
              <label>例句场景
                <input v-model.trim="aiScenario" maxlength="80" placeholder="例如：机场维修沟通、软件开发会议" />
                <small>AI 会按照这个场景生成扩展例句；留空则使用通用学习场景。</small>
              </label>
              <label>每条生成例句
                <select v-model.number="aiExampleCount">
                  <option :value="0">不生成例句</option>
                  <option v-for="count in 5" :key="count" :value="count">{{ count }} 句</option>
                </select>
                <small>生词会生成场景例句，经典句子会生成同句式例句。</small>
              </label>
            </div>
          </section>

          <div v-if="aiImportError" class="tech-english-ai-message tech-english-ai-message--error">{{ aiImportError }}</div>
          <div v-if="!authStore.state.user" class="tech-english-ai-login">
            <span>登录后即可识别截图，并在确认结果时选择知识标签。</span>
            <RouterLink class="primary-button" to="/login"><LogIn :size="16" />前往登录</RouterLink>
          </div>
          <footer v-else-if="!aiRecognitionResults.length && !aiImportResults.length" class="tech-english-ai-submit">
            <p><strong>先识别，不入库</strong><span>下一步会展示完整结果，再由你给每条内容选标签并确认。</span></p>
            <button class="primary-button" type="submit" :disabled="aiImporting">
              <Sparkles :size="17" />{{ aiImporting ? '正在识别截图…' : `开始识别 ${aiImages.length || ''} 张截图` }}
            </button>
          </footer>

          <section v-if="aiRecognitionResults.length" class="tech-english-ai-review" aria-live="polite">
            <header>
              <div><Sparkles :size="19" /><span><strong>识别完成，等待确认</strong><small>{{ aiRecognitionCount }} 组任务 · {{ aiRecognizedItemCount }} 条语料</small></span></div>
              <small>会话 {{ aiSessionUuid }}</small>
            </header>

            <section class="tech-english-ai-tag-picker tech-english-ai-tag-picker--batch">
              <label>当前标签
                <div class="tech-english-tag-search">
                  <Search :size="16" />
                  <input v-model="aiTagSearch" maxlength="80" placeholder="搜索并选择一个标签" @input="selectedAiTagId = null" />
                </div>
              </label>
              <p v-if="tagLoading" class="tech-english-tag-state">正在加载标签...</p>
              <p v-else-if="tagError" class="tech-english-tag-state tech-english-tag-state--error">{{ tagError }}</p>
              <div v-else-if="aiTagSearch && !selectedAiTag" class="tech-english-ai-tag-options">
                <button v-for="tag in filteredAiTags" :key="tag.id" type="button" @click="selectAiTag(tag)"><span>{{ tag.path }}</span></button>
              </div>
              <button v-if="selectedAiTag" class="tech-english-ai-selected-tag" type="button" title="重新选择知识标签" @click="selectedAiTagId = null">
                <Check :size="14" /><span>{{ selectedAiTag.path }}</span><X :size="13" />
              </button>
              <footer class="tech-english-ai-tag-picker__actions">
                <p>先选中一个当前标签，再逐条补充；也可以一键填充到所有未标注项。</p>
                <button class="secondary-button" type="button" :disabled="!selectedAiTagId" @click="fillCurrentTagToUnassignedItems">
                  <Check :size="16" />填充到未标注项
                </button>
              </footer>
            </section>

            <div class="tech-english-ai-review__chunks">
              <article v-for="batch in aiRecognitionResults" :key="batch.batchUuid" class="tech-english-ai-chunk">
                <header class="tech-english-ai-chunk__header">
                  <span>分组 {{ batch.chunkIndex }} / {{ batch.chunkCount }}</span>
                  <div>
                    <small>{{ batch.imageCount }} 张截图 · {{ batch.itemCount }} 条语料</small>
                    <button class="secondary-button" type="button" :disabled="aiConfirming || !isBatchTagged(batch)" @click="confirmAiBatch(batch)">
                      <Check :size="15" />{{ aiConfirming ? '入库中…' : '确认本组入库' }}
                    </button>
                  </div>
                </header>
                <div class="tech-english-ai-review__list">
                  <article v-for="item in batch.items" :key="item.itemKey" class="tech-english-ai-item">
                    <header>
                      <span>截图 {{ item.sourceImageIndex }}</span>
                      <small>{{ item.corpusType === 'VOCABULARY' ? '生词' : '经典句子' }}</small>
                    </header>
                    <h3>{{ item.englishText }}</h3>
                    <div v-if="item.partOfSpeech || item.britishPhonetic || item.americanPhonetic" class="tech-english-ai-review__pronunciation">
                      <strong v-if="item.partOfSpeech">{{ item.partOfSpeech }}</strong>
                      <span v-if="item.britishPhonetic">英 {{ item.britishPhonetic }}</span>
                      <span v-if="item.americanPhonetic">美 {{ item.americanPhonetic }}</span>
                    </div>
                    <p v-if="item.translationText">{{ item.translationText }}</p>
                    <section v-if="item.sentencePattern" class="tech-english-ai-review__pattern">
                      <small>经典句式</small><strong>{{ item.sentencePattern }}</strong><p v-if="item.sentencePatternExplanation">{{ item.sentencePatternExplanation }}</p>
                    </section>
                    <div v-if="item.keyVocabulary.length" class="tech-english-ai-review__keywords">
                      <span v-for="word in item.keyVocabulary" :key="`${word.word}-${word.partOfSpeech || ''}`"><strong>{{ word.word }}</strong>{{ word.partOfSpeech ? ` · ${word.partOfSpeech}` : '' }}{{ word.meaning ? ` · ${word.meaning}` : '' }}</span>
                    </div>
                    <details v-if="item.examples.length">
                      <summary>{{ item.examples.length }} 条扩展例句</summary>
                      <div v-for="(example, exampleIndex) in item.examples" :key="exampleIndex"><p>{{ example.englishText }}</p><small v-if="example.translationText">{{ example.translationText }}</small></div>
                    </details>
                    <div class="tech-english-ai-item__tags">
                      <span v-for="tagId in aiItemTagAssignments[item.itemKey] ?? []" :key="tagId">{{ tagPath(tagId) }}</span>
                      <small v-if="!hasItemTags(item.itemKey)">未标注</small>
                    </div>
                    <footer class="tech-english-ai-item__actions">
                      <button class="secondary-button" type="button" :disabled="!selectedAiTagId" @click="assignCurrentTagToAiItem(item.itemKey)">使用当前标签</button>
                      <button class="secondary-button" type="button" :disabled="!hasItemTags(item.itemKey)" @click="clearAiItemAssignment(item.itemKey)">清空</button>
                    </footer>
                  </article>
                </div>
              </article>
            </div>

            <section class="tech-english-ai-confirm">
              <div class="tech-english-ai-confirm__heading"><span>04 · 分批确认入库</span><small>每个批次都可单独入库，已入库批次不会重复创建</small></div>
              <p>为当前批次的每条结果选择标签后，点击该批次右上角的确认按钮即可。</p>
            </section>
          </section>

          <section v-if="aiImportResults.length" class="tech-english-ai-result" aria-live="polite">
            <header>
              <div><Check :size="19" /><span><strong>识别入库完成</strong><small>{{ aiImportResults.reduce((total, batch) => total + batch.imageCount, 0) }} 张截图，共创建 {{ aiImportedCount }} 条语料</small></span></div>
              <small>来源 · {{ aiImportResults[0]?.sourceName ?? '识图结果' }}</small>
            </header>
            <div class="tech-english-ai-result__grid">
              <RouterLink v-for="item in aiImportResults.flatMap((batch) => batch.items)" :key="item.id" :to="`/tech-english/${item.id}`">
                <span>{{ item.corpusType === 'VOCABULARY' ? '生词' : '句子' }}</span>
                <strong>{{ item.title }}</strong>
                <p>{{ item.translationText || item.explanation || '已创建语料' }}</p>
                <small>查看详情 <ArrowRight :size="13" /></small>
              </RouterLink>
            </div>
          </section>
        </main>
      </form>
    </section>

    <Teleport v-if="!isAiImportPage" to="body">
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

            <section class="tech-english-composer__tag-picker">
              <label>知识标签
                <div class="tech-english-tag-search">
                  <Search :size="16" />
                  <input v-model="tagSearch" maxlength="80" placeholder="搜索并选择一个标签" />
                </div>
              </label>
              <p v-if="tagLoading" class="tech-english-tag-state">正在加载标签...</p>
              <p v-else-if="tagError" class="tech-english-tag-state tech-english-tag-state--error">{{ tagError }}</p>
              <div v-else class="tech-english-tag-options">
                <button v-for="tag in filteredCreateTags" :key="tag.id" type="button" :class="{ active: selectedCreateTagId === tag.id }" @click="selectCreateTag(tag)">
                  <Check v-if="selectedCreateTagId === tag.id" :size="14" />
                  <span>{{ tag.path }}</span>
                </button>
              </div>
              <small class="tech-english-composer__tag">{{ selectedCreateTag?.path || '请选择一个知识标签' }}</small>
            </section>

            <template v-if="form.corpusType === 'VOCABULARY'">
              <label>单词或术语
                <input v-model.trim="form.englishText" maxlength="200" required placeholder="例如 idempotent" />
              </label>
              <div class="tech-english-composer__compact">
                <label>发音提示
                  <input v-model.trim="form.phonetic" maxlength="120" placeholder="例如 eye-DEMP-uh-tuhnt" />
                </label>
                <label>难度
                  <select v-model="form.difficulty">
                    <option value="BEGINNER">入门</option>
                    <option value="INTERMEDIATE">中级</option>
                    <option value="ADVANCED">高级</option>
                  </select>
                </label>
              </div>
              <label>中文释义
                <textarea v-model="form.translationText" maxlength="5000" placeholder="例如 幂等；可重复执行且最终结果不变"></textarea>
              </label>
              <label>用法提示
                <textarea v-model="form.explanation" maxlength="1000" placeholder="可选，写一句使用场景或易混点"></textarea>
              </label>
              <section class="tech-english-examples">
                <header>
                  <strong>例句</strong>
                  <button type="button" @click="addVocabularyExample"><Plus :size="15" />添加一组</button>
                </header>
                <div v-for="(example, index) in form.vocabularyExamples" :key="index" class="tech-english-example-row">
                  <textarea v-model="example.englishText" maxlength="2000" placeholder="英文例句，例如 A PUT request should be idempotent."></textarea>
                  <textarea v-model="example.translationText" maxlength="1000" placeholder="例句释义，例如 PUT 请求应当是幂等的。"></textarea>
                  <button type="button" title="删除例句" aria-label="删除例句" @click="removeVocabularyExample(index)">
                    <Trash2 :size="16" />
                  </button>
                </div>
                <label class="tech-english-switch">
                  <input v-model="form.syncExamplesToSentences" type="checkbox" />
                  <span>例句同时加入技术句子语料库</span>
                </label>
              </section>
            </template>

            <label v-if="form.corpusType === 'SENTENCE'">技术语句
              <textarea v-model="form.englishText" required maxlength="20000" placeholder="粘贴一句技术英文表达"></textarea>
            </label>
            <template v-if="form.corpusType === 'ARTICLE'">
              <label>文章标题
                <input v-model.trim="form.title" maxlength="160" placeholder="可选，不填则自动使用来源或正文开头" />
              </label>
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
            <div v-if="form.corpusType !== 'VOCABULARY'" class="tech-english-composer__compact">
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
            <label v-if="form.corpusType !== 'VOCABULARY'">说明
              <textarea v-model="form.explanation" maxlength="1000" placeholder="用法、语境或记忆提示"></textarea>
            </label>
            <label v-if="form.corpusType !== 'VOCABULARY'">中文参考
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

    <p v-if="!isAiImportPage && submitMessage" class="share-message">{{ submitMessage }}</p>

    <section v-if="!isAiImportPage" class="tech-english-command">
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

    <div v-if="!isAiImportPage" class="tech-english-workspace">
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
