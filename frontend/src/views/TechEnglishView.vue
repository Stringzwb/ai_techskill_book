<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ArrowRight, BookOpenText, Check, Download, FileDown, FileSearch, FileText, Languages, LogIn, Plus, Quote, RotateCcw, Search, Send, SlidersHorizontal, Sparkles, Trash2, Type, UploadCloud, X } from '@lucide/vue'
import { useRoute } from 'vue-router'
import KnowledgeTagMultiSelector from '../components/KnowledgeTagMultiSelector.vue'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import { confirmTechEnglishScreenshotImport, createTechEnglishCorpus, downloadTechEnglishCorpusReport, fetchTechEnglishCorpus, importTechEnglishScreenshots, retryTechEnglishScreenshotImport } from '../services/techEnglish'
import { authStore } from '../stores/auth'
import type { KnowledgeTagNode, TechEnglishAiConfirmPayload, TechEnglishAiImportResponse, TechEnglishAiItemTagAssignment, TechEnglishAiRecognitionResponse, TechEnglishCorpusCreatePayload, TechEnglishCorpusPage, TechEnglishCorpusType, TechEnglishDifficulty, TechEnglishVocabularyExampleInput } from '../types'

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

interface AiRecognitionChunk extends TechEnglishAiRecognitionResponse {
  failed?: boolean
  errorMessage?: string
}

const AI_MAX_IMAGES = 20
const AI_CHUNK_SIZE = 5
const CORPUS_PAGE_SIZE = 20

type CorpusFilterType = TechEnglishCorpusType | 'PHRASE_PATTERN' | ''

const keyword = ref('')
const route = useRoute()
const isAiImportPage = computed(() => route.name === 'tech-english-import')
const submittedKeyword = ref('')
const corpusType = ref<CorpusFilterType>('')
const filterTagIds = ref<number[]>([])
const result = ref<TechEnglishCorpusPage>({ total: 0, page: 1, size: CORPUS_PAGE_SIZE, totalPages: 0, items: [] })
const selectedReportIds = ref<number[]>([])
const reportSelectMode = ref(false)
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
const createTagPickerOpen = ref(false)
const aiImageInput = ref<HTMLInputElement | null>(null)
const aiScenario = ref('')
const aiExampleCount = ref(2)
const aiTagSearch = ref('')
const selectedAiTagId = ref<number | null>(null)
const aiTagPickerOpen = ref(false)
const aiImages = ref<AiImagePreview[]>([])
const aiDragActive = ref(false)
const aiImporting = ref(false)
const aiConfirming = ref(false)
const aiRetryingChunk = ref<number | null>(null)
const aiImportError = ref('')
const aiSessionUuid = ref('')
const aiRecognitionResults = ref<AiRecognitionChunk[]>([])
const aiImportResults = ref<TechEnglishAiImportResponse[]>([])
const aiItemTagAssignments = reactive<Record<string, number[]>>({})
let createTagPickerCloseTimer: ReturnType<typeof setTimeout> | undefined
let aiTagPickerCloseTimer: ReturnType<typeof setTimeout> | undefined
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

const corpusCreateTypes: Array<{ value: TechEnglishCorpusType; label: string; icon: typeof Type }> = [
  { value: 'VOCABULARY', label: '单词', icon: Type },
  { value: 'PHRASE', label: '短语', icon: BookOpenText },
  { value: 'PATTERN', label: '句式', icon: Languages },
  { value: 'SENTENCE', label: '句子', icon: Quote },
  { value: 'ARTICLE', label: '文章', icon: FileText },
]

const corpusFilterTypes: Array<{ value: CorpusFilterType; label: string; icon: typeof Type; types: TechEnglishCorpusType[] }> = [
  { value: 'VOCABULARY', label: '单词', icon: Type, types: ['VOCABULARY'] },
  { value: 'PHRASE_PATTERN', label: '短语与句式', icon: BookOpenText, types: ['PHRASE', 'PATTERN'] },
  { value: 'SENTENCE', label: '句子', icon: Quote, types: ['SENTENCE'] },
  { value: 'ARTICLE', label: '文章', icon: FileText, types: ['ARTICLE'] },
]

const selectionLabel = computed(() => filterTagIds.value.length ? `已选择 ${filterTagIds.value.length} 个标签` : '')
const selectedReportCount = computed(() => selectedReportIds.value.length)
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

/** 清空某条识图结果的全部标签。 */
function clearAiItemAssignment(itemKey: string): void {
  delete aiItemTagAssignments[itemKey]
}

/** 为某条识图结果追加当前选中的标签。 */
function assignCurrentTagToAiItem(itemKey: string): void {
  if (!selectedAiTagId.value) return
  addAiItemTag(itemKey, selectedAiTagId.value)
}

/** 为一条识图结果追加指定标签。 */
function addAiItemTag(itemKey: string, tagId: number): void {
  const next = new Set(aiItemTagAssignments[itemKey] ?? [])
  next.add(tagId)
  aiItemTagAssignments[itemKey] = Array.from(next)
}

/** 使用卡片内选择器为单条识图结果追加标签。 */
function addAiItemTagFromSelect(itemKey: string, event: Event): void {
  const select = event.target as HTMLSelectElement
  const tagId = Number(select.value)
  if (Number.isSafeInteger(tagId) && tagId > 0) addAiItemTag(itemKey, tagId)
  select.value = ''
}

/** 从一条识图结果中移除指定标签。 */
function removeAiItemTag(itemKey: string, tagId: number): void {
  const remaining = (aiItemTagAssignments[itemKey] ?? []).filter((value) => value !== tagId)
  if (remaining.length) aiItemTagAssignments[itemKey] = remaining
  else delete aiItemTagAssignments[itemKey]
}

/** 将当前标签批量追加或替换到一个识图分组。 */
function applyCurrentTagToAiBatch(batch: TechEnglishAiRecognitionResponse, mode: 'append' | 'replace'): void {
  if (!selectedAiTagId.value) return
  batch.items.forEach((item) => {
    if (mode === 'replace') {
      aiItemTagAssignments[item.itemKey] = [selectedAiTagId.value as number]
      return
    }
    addAiItemTag(item.itemKey, selectedAiTagId.value as number)
  })
}

/** 清空一个识图分组中所有结果的标签。 */
function clearAiBatchAssignments(batch: TechEnglishAiRecognitionResponse): void {
  batch.items.forEach((item) => clearAiItemAssignment(item.itemKey))
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

/** 判断单个识图批次是否包含可确认入库的结果。 */
function hasBatchItems(batch: TechEnglishAiRecognitionResponse): boolean {
  return batch.items.length > 0
}

/** 转换语料类型展示文案。 */
function typeLabel(value: TechEnglishCorpusType): string {
  return corpusCreateTypes.find((item) => item.value === value)?.label ?? value
}

/** 转换识图结果类型展示文案。 */
function recognitionTypeLabel(value: TechEnglishCorpusType): string {
  return typeLabel(value)
}

/** 判断是否为词汇或短语语料。 */
function isLexicalType(value: TechEnglishCorpusType): boolean {
  return value === 'VOCABULARY' || value === 'PHRASE'
}

/** 转换难度展示文案。 */
function difficultyLabel(value: TechEnglishDifficulty): string {
  const labels: Record<TechEnglishDifficulty, string> = { BEGINNER: '入门', INTERMEDIATE: '中级', ADVANCED: '高级' }
  return labels[value] ?? value
}

/** 检查语料是否已选入报告。 */
function isReportSelected(id: number): boolean {
  return selectedReportIds.value.includes(id)
}

/** 切换报告语料选择，最多 100 条。 */
function toggleReportSelection(id: number, event?: Event): void {
  event?.stopPropagation()
  if (isReportSelected(id)) {
    selectedReportIds.value = selectedReportIds.value.filter((value) => value !== id)
    if (!selectedReportIds.value.length) reportSelectMode.value = false
    return
  }
  if (selectedReportIds.value.length >= 100) return
  reportSelectMode.value = true
  selectedReportIds.value = [...selectedReportIds.value, id]
}

/** 清空报告选择。 */
function clearReportSelection(): void {
  selectedReportIds.value = []
  reportSelectMode.value = false
}

/** 打开或关闭报告选择模式。 */
function toggleReportSelectMode(): void {
  reportSelectMode.value = !reportSelectMode.value
  if (!reportSelectMode.value && !selectedReportIds.value.length) return
}

/** 下载选中语料报告。 */
function exportCorpusReport(format: 'html' | 'pdf'): void {
  if (!selectedReportIds.value.length) return
  window.open(downloadTechEnglishCorpusReport(selectedReportIds.value, format), '_blank', 'noopener,noreferrer')
}

/** 加载技术英语语料列表。 */
async function loadCorpus(page = 1): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const filter = corpusFilterTypes.find((item) => item.value === corpusType.value)
    result.value = await fetchTechEnglishCorpus({
      keyword: submittedKeyword.value,
      corpusType: filter?.types.length === 1 ? filter.types[0] : undefined,
      corpusTypes: filter?.types.length && filter.types.length > 1 ? filter.types : undefined,
      tagIds: filterTagIds.value,
      page,
      size: CORPUS_PAGE_SIZE,
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

/** 多选标签变化后立即应用筛选。 */
function updateTagFilter(tagIds: number[]): void {
  filterTagIds.value = [...tagIds]
  void loadCorpus(1)
}

/** 切换语料类型筛选。 */
function toggleType(nextType: CorpusFilterType): void {
  corpusType.value = corpusType.value === nextType ? '' : nextType
  void loadCorpus(1)
}

/** 清空全部筛选条件。 */
function resetFilters(): void {
  keyword.value = ''
  submittedKeyword.value = ''
  corpusType.value = ''
  filterTagIds.value = []
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
  if (!isLexicalType(nextType)) {
    form.vocabularyExamples = [{ englishText: '', translationText: '' }]
    form.syncExamplesToSentences = false
  } else {
    form.syncExamplesToSentences = true
  }
}

/** 选择收录语料要绑定的知识标签。 */
function selectCreateTag(tag: FlatTagOption): void {
  selectedCreateTagId.value = tag.id
  form.tagIds = [tag.id]
  tagSearch.value = tag.path
  createTagPickerOpen.value = false
}

/** 选择截图识别结果要绑定的知识标签。 */
function selectAiTag(tag: FlatTagOption): void {
  selectedAiTagId.value = tag.id
  aiTagSearch.value = tag.path
  aiTagPickerOpen.value = false
}

/** 打开添加语料标签候选列表。 */
function openCreateTagPicker(): void {
  if (createTagPickerCloseTimer) clearTimeout(createTagPickerCloseTimer)
  createTagPickerOpen.value = true
  if (selectedCreateTagId.value) {
    selectedCreateTagId.value = null
    tagSearch.value = ''
  }
}

/** 延迟关闭添加语料标签候选列表，保证鼠标可以点击候选项。 */
function closeCreateTagPicker(): void {
  createTagPickerCloseTimer = setTimeout(() => { createTagPickerOpen.value = false }, 120)
}

/** 打开识图标签候选列表并准备新的搜索。 */
function openAiTagPicker(): void {
  if (aiTagPickerCloseTimer) clearTimeout(aiTagPickerCloseTimer)
  aiTagPickerOpen.value = true
  if (selectedAiTagId.value) {
    selectedAiTagId.value = null
    aiTagSearch.value = ''
  }
}

/** 延迟关闭识图标签候选列表，保证鼠标可以点击候选项。 */
function closeAiTagPicker(): void {
  aiTagPickerCloseTimer = setTimeout(() => { aiTagPickerOpen.value = false }, 120)
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
  aiTagPickerOpen.value = false
  aiImportError.value = ''
  aiRecognitionResults.value = []
  aiImportResults.value = []
  Object.keys(aiItemTagAssignments).forEach((key) => delete aiItemTagAssignments[key])
  aiSessionUuid.value = ''
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
    const failures = settled
      .map((item, index) => (item.status === 'rejected' ? `第 ${index + 1} 组：${item.reason instanceof Error ? item.reason.message : '识别失败'}` : ''))
      .filter(Boolean)
    const results: AiRecognitionChunk[] = settled.map((item, index) => item.status === 'fulfilled'
      ? { ...item.value, failed: false }
      : {
          sessionUuid,
          batchUuid: `retry-${sessionUuid}-${index + 1}`,
          chunkIndex: index + 1,
          chunkCount: chunks.length,
          importType: 'AUTO',
          sourceName: '技术英语识图',
          imageCount: chunks[index].length,
          itemCount: 0,
          expiresAt: '',
          items: [],
          failed: true,
          errorMessage: item.reason instanceof Error ? item.reason.message : '识别失败，请重试',
        })
    aiRecognitionResults.value = results
    results.filter((batch) => !batch.failed).forEach((batch) => {
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

/** 重试当前会话中失败的单个识别分组。 */
async function retryAiBatch(batch: AiRecognitionChunk): Promise<void> {
  if (!batch.failed || !aiSessionUuid.value || aiRetryingChunk.value !== null) return
  aiImportError.value = ''
  aiRetryingChunk.value = batch.chunkIndex
  try {
    const localRetryBatch = batch.batchUuid.startsWith(`retry-${aiSessionUuid.value}-`)
    const chunk = splitAiImages(aiImages.value, AI_CHUNK_SIZE)[batch.chunkIndex - 1]
    if (localRetryBatch && !chunk?.length) {
      throw new Error(`第 ${batch.chunkIndex} 组原始图片已不存在，无法重试`)
    }
    let result: TechEnglishAiRecognitionResponse
    if (localRetryBatch) {
      result = await importTechEnglishScreenshots({
        sessionUuid: aiSessionUuid.value,
        chunkIndex: batch.chunkIndex,
        chunkCount: batch.chunkCount,
        scenario: aiScenario.value.trim(),
        exampleCount: aiExampleCount.value,
        images: chunk.map((item) => item.file),
      })
    } else {
      result = await retryTechEnglishScreenshotImport(batch.batchUuid)
    }
    const position = aiRecognitionResults.value.findIndex((item) => item.chunkIndex === batch.chunkIndex && item.failed)
    if (position >= 0) aiRecognitionResults.value[position] = { ...result, failed: false }
    result.items.forEach((item) => {
      if (!aiItemTagAssignments[item.itemKey]) aiItemTagAssignments[item.itemKey] = []
    })
  } catch (error) {
    batch.errorMessage = error instanceof Error ? error.message : '识别失败，请稍后重试'
    aiImportError.value = `第 ${batch.chunkIndex} 组：${batch.errorMessage}`
  } finally {
    aiRetryingChunk.value = null
  }
}

/** 用户为单个识图批次选择标签后，确认保存该批次截图和语料。 */
async function confirmAiBatch(batch: AiRecognitionChunk): Promise<void> {
  aiImportError.value = ''
  if (batch.failed) return
  if (!hasBatchItems(batch)) {
    aiImportError.value = `第 ${batch.chunkIndex} 组没有可入库的识别结果`
    return
  }
  aiConfirming.value = true
  try {
    const result = await confirmTechEnglishScreenshotImport({
      batchUuid: batch.batchUuid,
      itemTagAssignments: buildItemTagAssignments(batch),
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
  if ((isLexicalType(form.corpusType) || form.corpusType === 'PATTERN' || form.corpusType === 'SENTENCE') && !form.englishText?.trim()) {
    submitError.value = '请填写英文内容'
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
      title: isLexicalType(form.corpusType) ? '' : form.title,
      tagIds: [...form.tagIds],
      vocabularyExamples: isLexicalType(form.corpusType) ? filledVocabularyExamples() : [],
      syncExamplesToSentences: isLexicalType(form.corpusType) && Boolean(form.syncExamplesToSentences),
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
  if (createTagPickerCloseTimer) clearTimeout(createTagPickerCloseTimer)
  if (aiTagPickerCloseTimer) clearTimeout(aiTagPickerCloseTimer)
})
</script>

<template>
  <section class="tech-english-page content-width">
    <header class="tech-english-hero">
      <div class="tech-english-hero__copy">
        <span>{{ isAiImportPage ? 'AI SCREENSHOT IMPORT' : 'TECHNICAL ENGLISH' }}</span>
        <h1>{{ isAiImportPage ? 'AI 截图识别' : '技术英语语料库' }}</h1>
        <p v-if="isAiImportPage">AI 自行判别词汇、短语与句式，分别按默认配置生成完整学习信息。</p>
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
                <p>最多 20 张图片，每组 5 张，自动拆成最多 4 个并发识别任务。AI 会区分单词、短语、句式和句子，再由你确认入库。</p>
        </div>
        <div class="tech-english-ai-source">
          <small>历史记录</small>
          <strong>查看识别会话</strong>
          <RouterLink class="secondary-button" to="/tech-english/history"><FileSearch :size="16" />识图记录</RouterLink>
        </div>
      </header>

      <form class="tech-english-ai-layout" @submit.prevent="submitAiImport">
        <aside class="tech-english-ai-modes">
          <small>01 · AI 自动判别</small>
          <div class="tech-english-ai-auto-mode">
            <Sparkles :size="20" />
            <span><strong>两套默认配置</strong><small>AI 按截图内容决定使用词汇短语或句式配置</small></span>
            <Check :size="16" />
          </div>
          <div class="tech-english-ai-config-summary">
            <p><Type :size="15" /><span><strong>词汇/短语配置</strong>词性、释义、英美音标与例句</span></p>
              <p><Languages :size="15" /><span><strong>句式提取</strong>只保存可复用的语法骨架，不把完整句子当作句式</span></p>
          </div>
          <div class="tech-english-ai-flow">
            <span>1</span><p>上传图片并自动切成 5 张一组</p>
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
                <small>词汇和短语会生成场景例句，句式会生成同框架例句。</small>
              </label>
            </div>
          </section>

          <div v-if="aiImportError" class="tech-english-ai-message tech-english-ai-message--error">{{ aiImportError }}</div>
          <div v-if="!authStore.state.user" class="tech-english-ai-login">
            <span>登录后即可识别图片，并在确认结果时选择知识标签。</span>
            <RouterLink class="primary-button" to="/login"><LogIn :size="16" />前往登录</RouterLink>
          </div>
          <footer v-else-if="!aiRecognitionResults.length && !aiImportResults.length" class="tech-english-ai-submit">
            <p><strong>先识别，不入库</strong><span>下一步会展示完整结果，再由你给每条内容选标签并确认。</span></p>
            <button class="primary-button" type="submit" :disabled="aiImporting">
              <Sparkles :size="17" />{{ aiImporting ? '正在识别图片…' : `开始识别 ${aiImages.length || ''} 张图片` }}
            </button>
          </footer>

          <section v-if="aiRecognitionResults.length" class="tech-english-ai-review" aria-live="polite">
            <header>
              <div><Sparkles :size="19" /><span><strong>识别完成，等待确认</strong><small>{{ aiRecognitionCount }} 组任务 · {{ aiRecognizedItemCount }} 条语料</small></span></div>
              <small>分组可独立编辑并确认</small>
            </header>

            <section class="tech-english-ai-tag-picker tech-english-ai-tag-picker--batch">
              <label>知识标签树
                <div class="tech-english-tag-search">
                  <Search :size="16" />
                  <input v-model="aiTagSearch" maxlength="80" placeholder="搜索并选择一个标签" @focus="openAiTagPicker" @blur="closeAiTagPicker" @input="selectedAiTagId = null; aiTagPickerOpen = true" />
                </div>
              </label>
              <p v-if="tagLoading" class="tech-english-tag-state">正在加载标签...</p>
              <p v-else-if="tagError" class="tech-english-tag-state tech-english-tag-state--error">{{ tagError }}</p>
              <div v-else-if="aiTagPickerOpen" class="tech-english-ai-tag-options">
                <button v-for="tag in filteredAiTags" :key="tag.id" type="button" @click="selectAiTag(tag)"><span>{{ tag.path }}</span></button>
              </div>
              <button v-if="selectedAiTag" class="tech-english-ai-selected-tag" type="button" title="重新选择知识标签" @click="openAiTagPicker">
                <Check :size="14" /><span>{{ selectedAiTag.path }}</span><X :size="13" />
              </button>
              <p class="tech-english-ai-tag-picker__hint">标签可不选。选择一个当前标签后，可对每条结果追加，也可在分组内批量追加或替换。</p>
            </section>

            <div class="tech-english-ai-review__chunks">
              <article v-for="batch in aiRecognitionResults" :key="batch.batchUuid" class="tech-english-ai-chunk">
                <header class="tech-english-ai-chunk__header">
                  <span>分组 {{ batch.chunkIndex }} / {{ batch.chunkCount }}</span>
                  <div>
                    <small>{{ batch.imageCount }} 张截图 · {{ batch.itemCount }} 条语料</small>
                    <div v-if="!batch.failed" class="tech-english-ai-chunk__batch-actions">
                      <button class="secondary-button" type="button" :disabled="!selectedAiTagId" @click="applyCurrentTagToAiBatch(batch, 'append')">批量追加</button>
                      <button class="secondary-button" type="button" :disabled="!selectedAiTagId" @click="applyCurrentTagToAiBatch(batch, 'replace')">批量替换</button>
                      <button class="secondary-button" type="button" @click="clearAiBatchAssignments(batch)">清空标签</button>
                    </div>
                    <button v-if="batch.failed" class="secondary-button" type="button" :disabled="aiRetryingChunk !== null" @click="retryAiBatch(batch)">
                      <RotateCcw :size="15" />{{ aiRetryingChunk === batch.chunkIndex ? '重试中…' : '重试本组' }}
                    </button>
                    <button v-else class="secondary-button" type="button" :disabled="aiConfirming" @click="confirmAiBatch(batch)">
                      <Check :size="15" />{{ aiConfirming ? '入库中…' : '确认本组入库' }}
                    </button>
                  </div>
                </header>
                <p v-if="batch.failed" class="tech-english-ai-message tech-english-ai-message--error">{{ batch.errorMessage || '本组识别失败，可单独重试。' }}</p>
                <div v-else class="tech-english-ai-review__list">
                  <article v-for="(item, itemIndex) in batch.items" :key="item.itemKey" class="tech-english-ai-item">
                    <header>
                      <span>候选 {{ itemIndex + 1 }}</span>
                      <small>{{ recognitionTypeLabel(item.corpusType) }}</small>
                    </header>
                    <h3>{{ item.englishText }}</h3>
                    <div v-if="item.partOfSpeech || item.britishPhonetic || item.americanPhonetic" class="tech-english-ai-review__pronunciation">
                      <strong v-if="item.partOfSpeech">{{ item.partOfSpeech }}</strong>
                      <span v-if="item.britishPhonetic">英 {{ item.britishPhonetic }}</span>
                      <span v-if="item.americanPhonetic">美 {{ item.americanPhonetic }}</span>
                    </div>
                    <p v-if="item.translationText">{{ item.translationText }}</p>
                    <section v-if="item.sentencePattern" class="tech-english-ai-review__pattern">
                      <small>句式框架</small><strong>{{ item.sentencePattern }}</strong><p v-if="item.sentencePatternExplanation">{{ item.sentencePatternExplanation }}</p>
                    </section>
                    <div v-if="item.keyVocabulary.length" class="tech-english-ai-review__keywords">
                      <span v-for="word in item.keyVocabulary" :key="`${word.word}-${word.partOfSpeech || ''}`"><strong>{{ word.word }}</strong>{{ word.partOfSpeech ? ` · ${word.partOfSpeech}` : '' }}{{ word.meaning ? ` · ${word.meaning}` : '' }}</span>
                    </div>
                    <div v-if="item.scenarioTags.length" class="tech-english-ai-item__scene-tags">
                      <span v-for="tag in item.scenarioTags" :key="tag.code">{{ tag.label }}</span>
                    </div>
                    <details v-if="item.examples.length">
                      <summary>{{ item.examples.length }} 条扩展例句</summary>
                      <div v-for="(example, exampleIndex) in item.examples" :key="exampleIndex"><p>{{ example.englishText }}</p><small v-if="example.translationText">{{ example.translationText }}</small></div>
                    </details>
                    <div class="tech-english-ai-item__tags">
                      <button v-for="tagId in aiItemTagAssignments[item.itemKey] ?? []" :key="tagId" type="button" :title="`移除 ${tagPath(tagId)}`" @click="removeAiItemTag(item.itemKey, tagId)">
                        <span>{{ tagPath(tagId) }}</span><X :size="12" />
                      </button>
                      <small v-if="!hasItemTags(item.itemKey)">未标注</small>
                    </div>
                    <footer class="tech-english-ai-item__actions">
                      <select :aria-label="`为 ${item.englishText} 添加知识标签`" @change="addAiItemTagFromSelect(item.itemKey, $event)">
                        <option value="">添加标签</option>
                        <option v-for="tag in flatTags" :key="tag.id" :value="tag.id" :disabled="aiItemTagAssignments[item.itemKey]?.includes(tag.id)">{{ tag.path }}</option>
                      </select>
                      <button class="secondary-button" type="button" :disabled="!selectedAiTagId" @click="assignCurrentTagToAiItem(item.itemKey)">使用当前标签</button>
                      <button class="secondary-button" type="button" :disabled="!hasItemTags(item.itemKey)" @click="clearAiItemAssignment(item.itemKey)">清空</button>
                    </footer>
                  </article>
                </div>
              </article>
            </div>

            <section class="tech-english-ai-confirm">
              <div class="tech-english-ai-confirm__heading"><span>04 · 分批确认入库</span><small>每个批次都可单独入库，已入库批次不会重复创建</small></div>
              <p>标签是可选的；确认前可逐条编辑，也可使用分组批量操作。确认后按单词、短语、句式和句子分别入库。</p>
            </section>
          </section>

          <section v-if="aiImportResults.length" class="tech-english-ai-result" aria-live="polite">
            <header>
              <div><Check :size="19" /><span><strong>识别入库完成</strong><small>共创建 {{ aiImportedCount }} 条语料</small></span></div>
            </header>
            <div class="tech-english-ai-result__grid">
              <RouterLink v-for="item in aiImportResults.flatMap((batch) => batch.items)" :key="item.id" :to="`/tech-english/${item.id}`">
                <span>{{ recognitionTypeLabel(item.corpusType) }}</span>
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
              <button v-for="item in corpusCreateTypes" :key="item.value" type="button" :class="{ active: form.corpusType === item.value }" @click="setCreateType(item.value)">
                <component :is="item.icon" :size="16" />{{ item.label }}
              </button>
            </div>

            <section class="tech-english-composer__tag-picker">
              <label>知识标签
                <div class="tech-english-tag-search">
                  <Search :size="16" />
                  <input v-model="tagSearch" maxlength="80" placeholder="搜索并选择一个标签" @focus="openCreateTagPicker" @blur="closeCreateTagPicker" @input="createTagPickerOpen = true; selectedCreateTagId = null" />
                </div>
              </label>
              <p v-if="tagLoading" class="tech-english-tag-state">正在加载标签...</p>
              <p v-else-if="tagError" class="tech-english-tag-state tech-english-tag-state--error">{{ tagError }}</p>
              <div v-else-if="createTagPickerOpen" class="tech-english-tag-options">
                <button v-for="tag in filteredCreateTags" :key="tag.id" type="button" :class="{ active: selectedCreateTagId === tag.id }" @click="selectCreateTag(tag)">
                  <Check v-if="selectedCreateTagId === tag.id" :size="14" />
                  <span>{{ tag.path }}</span>
                </button>
              </div>
              <small class="tech-english-composer__tag">{{ selectedCreateTag?.path || '未选择知识标签' }}</small>
            </section>

            <template v-if="isLexicalType(form.corpusType)">
              <label>{{ form.corpusType === 'PHRASE' ? '短语或固定搭配' : '单词或术语' }}
                <input v-model.trim="form.englishText" maxlength="200" required :placeholder="form.corpusType === 'PHRASE' ? '例如 zero-downtime deployment' : '例如 idempotent'" />
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

            <label v-if="form.corpusType === 'SENTENCE'">完整句子
              <textarea v-model="form.englishText" required maxlength="20000" placeholder="粘贴一句技术英文表达"></textarea>
            </label>
            <template v-if="form.corpusType === 'PATTERN'">
              <label>可复用句式框架
                <textarea v-model="form.englishText" required maxlength="500" placeholder="例如 [Subject] should be [adjective]."></textarea>
              </label>
              <label>句式说明
                <textarea v-model="form.explanation" maxlength="1000" placeholder="说明这个骨架的语法作用和适用范围"></textarea>
              </label>
            </template>
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
            <div v-if="!isLexicalType(form.corpusType)" class="tech-english-composer__compact">
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
            <label v-if="!isLexicalType(form.corpusType)">说明
              <textarea v-model="form.explanation" maxlength="1000" placeholder="用法、语境或记忆提示"></textarea>
            </label>
            <label v-if="!isLexicalType(form.corpusType)">中文参考
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
        <button v-for="item in corpusFilterTypes" :key="item.value" type="button" :class="{ active: corpusType === item.value }" @click="toggleType(item.value)">
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
        <KnowledgeTagMultiSelector :key="selectorKey" @change="updateTagFilter" />
        <div class="active-filter">
          <small>当前范围</small>
          <strong>{{ selectionLabel || '全部知识标签' }}</strong>
        </div>
        <button class="filter-reset" type="button" @click="resetFilters"><RotateCcw :size="15" />重置筛选</button>
      </aside>

      <main class="tech-english-results" aria-live="polite">
        <header class="tech-english-results__header">
          <div><span>ENGLISH CORPUS</span><h2>{{ submittedKeyword ? `“${submittedKeyword}”的结果` : '最新语料' }}</h2></div>
          <div class="tech-english-results__tools">
            <small v-if="!loading">{{ corpusFilterTypes.find((item) => item.value === corpusType)?.label || selectionLabel || '全部语料' }}</small>
            <button class="tech-english-report-toggle" type="button" :class="{ active: reportSelectMode || selectedReportCount }" title="报告篮" aria-label="报告篮" @click="toggleReportSelectMode">
              <FileDown :size="15" />
              <span v-if="selectedReportCount">{{ selectedReportCount }}</span>
            </button>
          </div>
        </header>

        <div v-if="loading" class="document-result-state"><FileSearch :size="25" />正在检索语料…</div>
        <div v-else-if="errorMessage" class="document-result-state document-result-state--error">
          <span>{{ errorMessage }}</span><button type="button" @click="loadCorpus(result.page)">重新加载</button>
        </div>
        <div v-else-if="!result.items.length" class="document-result-state">
          <BookOpenText :size="27" /><strong>没有找到匹配语料</strong><span>可以更换关键词、语料类型或知识标签。</span>
        </div>
        <div v-else class="tech-english-result-list">
          <article v-for="item in result.items" :key="item.id" class="tech-english-result-item" :class="[`tech-english-result-item--${item.corpusType.toLowerCase()}`, { 'is-selecting': reportSelectMode, 'is-selected': isReportSelected(item.id) }]">
            <div class="tech-english-result-item__meta">
              <span>{{ typeLabel(item.corpusType) }}</span>
              <small v-if="item.corpusType === 'VOCABULARY'">{{ item.partOfSpeech || difficultyLabel(item.difficulty) }}</small>
              <small v-else-if="item.corpusType === 'PATTERN'">可复用表达</small>
              <small v-else-if="item.corpusType === 'ARTICLE'">阅读材料</small>
              <small v-else>{{ difficultyLabel(item.difficulty) }}</small>
            </div>
            <button class="tech-english-report-pick" type="button" :class="{ active: isReportSelected(item.id) }" :disabled="!isReportSelected(item.id) && selectedReportCount >= 100" title="选择到报告篮" :aria-label="isReportSelected(item.id) ? '从报告篮移除' : '选择到报告篮'" @click="toggleReportSelection(item.id, $event)">
              <Check v-if="isReportSelected(item.id)" :size="14" />
            </button>
            <template v-if="item.corpusType === 'VOCABULARY'">
              <h3>{{ item.title || item.englishText }}</h3>
              <p v-if="item.phonetic || item.americanPhonetic || item.britishPhonetic" class="tech-english-result-item__phonetic">{{ item.phonetic || item.americanPhonetic || item.britishPhonetic }}</p>
              <p v-if="item.translationText" class="tech-english-result-item__translation">{{ item.translationText }}</p>
            </template>
            <template v-else-if="item.corpusType === 'PHRASE'">
              <h3>{{ item.englishText || item.title }}</h3>
              <p v-if="item.translationText" class="tech-english-result-item__translation">{{ item.translationText }}</p>
            </template>
            <template v-else-if="item.corpusType === 'PATTERN'">
              <h3>{{ item.englishText || item.title }}</h3>
              <p v-if="item.explanation" class="tech-english-result-item__translation">{{ item.explanation }}</p>
            </template>
            <template v-else-if="item.corpusType === 'SENTENCE'">
              <h3>{{ item.englishText || item.title }}</h3>
              <p v-if="item.translationText" class="tech-english-result-item__translation">{{ item.translationText }}</p>
            </template>
            <template v-else>
              <h3>{{ item.title || '未命名文章' }}</h3>
              <p v-if="item.explanation || item.translationText" class="tech-english-result-item__translation">{{ item.explanation || item.translationText }}</p>
            </template>
            <footer>
              <RouterLink :to="`/tech-english/${item.id}`">查看<ArrowRight :size="15" /></RouterLink>
            </footer>
          </article>
        </div>

        <nav v-if="result.totalPages > 1" class="document-pagination" aria-label="技术英语语料分页">
          <button type="button" :disabled="result.page <= 1 || loading" @click="loadCorpus(result.page - 1)">上一页</button>
          <span>{{ result.page }} / {{ result.totalPages }}</span>
          <button type="button" :disabled="result.page >= result.totalPages || loading" @click="loadCorpus(result.page + 1)">下一页</button>
        </nav>
      </main>
    </div>

    <Teleport v-if="!isAiImportPage && selectedReportCount" to="body">
      <section class="tech-english-report-dock" aria-label="语料报告篮">
        <div>
          <FileDown :size="17" />
          <span><strong>{{ selectedReportCount }}</strong><small>/ 100</small></span>
        </div>
        <button type="button" title="导出 HTML" aria-label="导出 HTML" @click="exportCorpusReport('html')"><Download :size="15" /><span>HTML</span></button>
        <button type="button" title="导出 PDF" aria-label="导出 PDF" @click="exportCorpusReport('pdf')"><Download :size="15" /><span>PDF</span></button>
        <button type="button" title="清空报告篮" aria-label="清空报告篮" @click="clearReportSelection"><X :size="15" /></button>
      </section>
    </Teleport>
  </section>
</template>
