<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ArrowLeft, Check, Download, FileSearch, RefreshCcw, Search, Sparkles, Trash2, X } from '@lucide/vue'
import { useRoute } from 'vue-router'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import { confirmTechEnglishScreenshotImport, deleteTechEnglishRecognitionHistory, downloadTechEnglishRecognitionBatchHistory, downloadTechEnglishRecognitionHistory, fetchTechEnglishRecognitionHistory, fetchTechEnglishRecognitionHistoryDetail, retryTechEnglishScreenshotImport } from '../services/techEnglish'
import type { KnowledgeTagNode, TechEnglishRecognitionHistoryDetail, TechEnglishRecognitionHistorySummary, TechEnglishRecognitionHistoryTask } from '../types'

const route = useRoute()
const history = ref({ total: 0, page: 1, size: 10, totalPages: 0, items: [] as TechEnglishRecognitionHistorySummary[] })
const loading = ref(true)
const errorMessage = ref('')
const selectedSessionUuid = ref('')
const detail = ref<TechEnglishRecognitionHistoryDetail | null>(null)
const detailLoading = ref(false)
const detailError = ref('')
const tagTree = ref<KnowledgeTagNode[]>([])
const tagSearch = ref('')
const selectedTagId = ref<number | null>(null)
const tagPickerOpen = ref(false)
const batchImporting = ref('')
const batchRetrying = ref('')
const deletingSession = ref('')
const itemTagAssignments = reactive<Record<string, number[]>>({})
let tagPickerCloseTimer: ReturnType<typeof setTimeout> | undefined

interface FlatTagOption {
  id: number
  name: string
  path: string
}

/** 将知识标签树展开为可搜索路径。 */
function flattenTags(nodes: KnowledgeTagNode[], parents: string[] = []): FlatTagOption[] {
  return nodes.flatMap((node) => {
    const path = [...parents, node.name]
    return [{ id: node.id, name: node.name, path: path.join(' / ') }, ...flattenTags(node.children, path)]
  })
}

const flatTags = ref<FlatTagOption[]>([])
const filteredTags = ref<FlatTagOption[]>([])

/** 转换识图结果类型文案。 */
function corpusTypeLabel(value: string): string {
  if (value === 'PHRASE') return '短语'
  if (value === 'PATTERN') return '句式'
  if (value === 'SENTENCE') return '句子'
  return '词汇'
}

/** 将句式槽位收敛为阅读时更轻的省略号。 */
function readablePattern(value: string | null | undefined): string {
  return value ? value.replace(/\[[^\]]*\]/g, '...').replace(/(?:\.\.\.){2,}/g, '...') : ''
}

/** 根据搜索词刷新历史入库标签候选。 */
function refreshTagOptions(): void {
  const query = tagSearch.value.trim().toLowerCase()
  const source = query ? flatTags.value.filter((tag) => `${tag.name} ${tag.path}`.toLowerCase().includes(query)) : flatTags.value
  filteredTags.value = source.slice(0, 20)
}

/** 选择历史批次入库使用的当前标签。 */
function selectTag(tag: FlatTagOption): void {
  selectedTagId.value = tag.id
  tagSearch.value = tag.path
  tagPickerOpen.value = false
}

/** 打开历史批次标签候选列表并准备新的搜索。 */
function openTagPicker(): void {
  if (tagPickerCloseTimer) clearTimeout(tagPickerCloseTimer)
  tagPickerOpen.value = true
  if (selectedTagId.value) {
    selectedTagId.value = null
    tagSearch.value = ''
  }
}

/** 延迟关闭历史批次标签候选列表，保证鼠标可以点击候选项。 */
function closeTagPicker(): void {
  tagPickerCloseTimer = setTimeout(() => { tagPickerOpen.value = false }, 120)
}

/** 清空当前历史批次标签选择。 */
function clearTagSelection(): void {
  selectedTagId.value = null
  tagSearch.value = ''
  tagPickerOpen.value = false
  refreshTagOptions()
}

/** 返回标签完整路径。 */
function tagPath(tagId: number): string {
  return flatTags.value.find((tag) => tag.id === tagId)?.path ?? `#${tagId}`
}

/** 给历史识图结果追加当前标签。 */
function assignTag(itemKey: string): void {
  if (!selectedTagId.value) return
  addTag(itemKey, selectedTagId.value)
}

/** 为一条历史识图结果追加指定标签。 */
function addTag(itemKey: string, tagId: number): void {
  const values = new Set(itemTagAssignments[itemKey] ?? [])
  values.add(tagId)
  itemTagAssignments[itemKey] = [...values]
}

/** 使用卡片内选择器为单条历史识图结果追加标签。 */
function addTagFromSelect(itemKey: string, event: Event): void {
  const select = event.target as HTMLSelectElement
  const tagId = Number(select.value)
  if (Number.isSafeInteger(tagId) && tagId > 0) addTag(itemKey, tagId)
  select.value = ''
}

/** 清空历史识图结果的标签。 */
function clearTags(itemKey: string): void {
  delete itemTagAssignments[itemKey]
}

/** 从历史识图结果中移除指定标签。 */
function removeTag(itemKey: string, tagId: number): void {
  const remaining = (itemTagAssignments[itemKey] ?? []).filter((value) => value !== tagId)
  if (remaining.length) itemTagAssignments[itemKey] = remaining
  else delete itemTagAssignments[itemKey]
}

/** 将当前标签批量追加或替换到历史识图分组。 */
function applyTagToBatch(task: TechEnglishRecognitionHistoryTask, mode: 'append' | 'replace'): void {
  if (!selectedTagId.value) return
  task.items.forEach((item) => {
    if (mode === 'replace') {
      itemTagAssignments[item.itemKey] = [selectedTagId.value as number]
      return
    }
    addTag(item.itemKey, selectedTagId.value as number)
  })
}

/** 清空一个历史识图分组的标签。 */
function clearBatchTags(task: TechEnglishRecognitionHistoryTask): void {
  task.items.forEach((item) => clearTags(item.itemKey))
}

/** 判断一个批次是否存在可入库的识别结果。 */
function batchReady(task: TechEnglishRecognitionHistoryTask): boolean {
  return task.status === 'RECOGNIZED' && task.items.length > 0
}

/** 从永久记录确认一个批次，服务端会复用识别时保存的原图。 */
async function importBatch(task: TechEnglishRecognitionHistoryTask): Promise<void> {
  if (!batchReady(task)) {
    detailError.value = '该批次没有可入库的识别结果'
    return
  }
  batchImporting.value = task.batchUuid
  detailError.value = ''
  try {
    await confirmTechEnglishScreenshotImport({
      batchUuid: task.batchUuid,
      itemTagAssignments: task.items.map((item) => ({ itemKey: item.itemKey, tagIds: itemTagAssignments[item.itemKey] ?? [] })),
    })
    await openDetail(selectedSessionUuid.value)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '批次入库失败，请稍后重试'
  } finally {
    batchImporting.value = ''
  }
}

/** 使用已保存来源截图重试历史中的失败分组。 */
async function retryBatch(task: TechEnglishRecognitionHistoryTask): Promise<void> {
  if (task.status !== 'FAILED' || batchRetrying.value) return
  batchRetrying.value = task.batchUuid
  detailError.value = ''
  try {
    await retryTechEnglishScreenshotImport(task.batchUuid)
    await openDetail(selectedSessionUuid.value)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '批次重试失败，请稍后重试'
  } finally {
    batchRetrying.value = ''
  }
}

/** 将状态翻译成更适合页面展示的文字。 */
function statusLabel(status: TechEnglishRecognitionHistorySummary['status']): string {
  const labels: Record<TechEnglishRecognitionHistorySummary['status'], string> = {
    PROCESSING: '处理中',
    FAILED: '失败',
    RECOGNIZED: '已识别',
    PARTIAL: '部分入库',
    IMPORTED: '已入库',
  }
  return labels[status] ?? status
}

/** 给历史状态加上简短描述。 */
function statusNote(status: TechEnglishRecognitionHistorySummary['status']): string {
  const notes: Record<TechEnglishRecognitionHistorySummary['status'], string> = {
    PROCESSING: '正在等待所有分组完成',
    FAILED: '本次会话没有可用结果',
    RECOGNIZED: '结果已识别，等待标签确认',
    PARTIAL: '部分分组已处理完成',
    IMPORTED: '已完成正式入库',
  }
  return notes[status] ?? ''
}

/** 格式化时间。 */
function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

/** 读取识图历史列表。 */
async function loadHistory(page = history.value.page): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    history.value = await fetchTechEnglishRecognitionHistory({ page, size: 10 })
    if (!selectedSessionUuid.value && history.value.items.length) {
      await openDetail(history.value.items[0].sessionUuid)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '识图记录加载失败'
  } finally {
    loading.value = false
  }
}

/** 读取单次识图会话详情。 */
async function openDetail(sessionUuid: string): Promise<void> {
  if (!sessionUuid) return
  selectedSessionUuid.value = sessionUuid
  detailLoading.value = true
  detailError.value = ''
  try {
    detail.value = await fetchTechEnglishRecognitionHistoryDetail(sessionUuid)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '识图详情加载失败'
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

/** 删除当前用户的一次识图会话。 */
async function deleteSession(sessionUuid: string, event?: Event): Promise<void> {
  event?.stopPropagation()
  if (!sessionUuid || deletingSession.value) return
  if (!window.confirm('删除这次识图任务？未入库的识别结果和来源截图会一起删除。')) return
  deletingSession.value = sessionUuid
  errorMessage.value = ''
  try {
    await deleteTechEnglishRecognitionHistory(sessionUuid)
    if (selectedSessionUuid.value === sessionUuid) {
      selectedSessionUuid.value = ''
      detail.value = null
    }
    await loadHistory()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '识图任务删除失败，请稍后重试'
  } finally {
    deletingSession.value = ''
  }
}

/** 下载当前会话的 Markdown 或 HTML 导出文件。 */
function exportHistory(format: 'markdown' | 'html'): void {
  if (!selectedSessionUuid.value) return
  window.location.href = downloadTechEnglishRecognitionHistory(selectedSessionUuid.value, format)
}

/** 导出当前会话中的单个批次。 */
function exportBatch(batchUuid: string, format: 'markdown' | 'html'): void {
  if (!selectedSessionUuid.value) return
  window.location.href = downloadTechEnglishRecognitionBatchHistory(selectedSessionUuid.value, batchUuid, format)
}

onMounted(async () => {
  try {
    tagTree.value = await fetchKnowledgeTagTree()
    flatTags.value = flattenTags(tagTree.value)
    refreshTagOptions()
  } catch {
    detailError.value = '知识标签加载失败，暂时不能进行批次入库'
  }
  await loadHistory()
  const session = typeof route.query.session === 'string' ? route.query.session : ''
  if (session) {
    await openDetail(session)
  }
})

onBeforeUnmount(() => {
  if (tagPickerCloseTimer) clearTimeout(tagPickerCloseTimer)
})

watch(
  () => route.query.session,
  async (session) => {
    if (typeof session === 'string' && session) {
      await openDetail(session)
    }
  },
)
</script>

<template>
  <section class="tech-english-page content-width tech-english-history-page">
    <header class="tech-english-hero">
      <div class="tech-english-hero__copy">
        <span>TECH ENGLISH HISTORY</span>
        <h1>识图记录</h1>
        <p>这里记录每次截图识别的会话、分组和结果。失败分组可直接重试，已识别结果可查看、打标签并导出 Markdown 或 HTML。</p>
      </div>
      <div class="tech-english-heading-actions">
        <RouterLink class="secondary-button" to="/tech-english"><ArrowLeft :size="17" />返回语料库</RouterLink>
        <button class="secondary-button" type="button" @click="loadHistory()"><RefreshCcw :size="17" />刷新</button>
      </div>
    </header>

    <div v-if="loading && !history.items.length" class="document-result-state"><FileSearch :size="25" />正在加载识图记录…</div>
    <div v-else-if="errorMessage" class="document-result-state document-result-state--error">
      <span>{{ errorMessage }}</span><button type="button" @click="loadHistory()">重新加载</button>
    </div>
    <div v-else class="tech-english-history-layout">
      <aside class="tech-english-history-list">
        <header>
          <span><Sparkles :size="14" /> 识图会话</span>
          <small>{{ history.total }} 条</small>
        </header>
        <div v-for="item in history.items" :key="item.sessionUuid" class="tech-english-history-entry">
          <button
            type="button"
            class="tech-english-history-card"
            :class="{ active: selectedSessionUuid === item.sessionUuid }"
            @click="openDetail(item.sessionUuid)"
          >
            <div class="tech-english-history-card__top">
              <strong>{{ item.sourceName || '未命名来源' }}</strong>
              <span>{{ statusLabel(item.status) }}</span>
            </div>
            <p>{{ item.scenario || '通用例句语境' }}</p>
            <small>{{ item.chunkCount }} 组 · {{ item.completedChunkCount }} 已完成 · {{ item.imageCount }} 张截图 · {{ item.itemCount }} 条语料</small>
            <time>{{ formatTime(item.createdAt) }}</time>
          </button>
          <button class="tech-english-history-delete" type="button" :disabled="deletingSession === item.sessionUuid || item.status === 'PROCESSING'" :title="item.status === 'PROCESSING' ? '任务处理中，暂不能删除' : '删除识图任务'" :aria-label="item.status === 'PROCESSING' ? '任务处理中，暂不能删除' : '删除识图任务'" @click="deleteSession(item.sessionUuid, $event)">
            <Trash2 :size="15" />
          </button>
        </div>
        <div v-if="!history.items.length" class="tech-english-history-empty">
          <strong>暂无识图记录</strong>
          <span>完成一次截图识别后，这里就会出现会话摘要。</span>
        </div>
      </aside>

      <main class="tech-english-history-detail">
        <div v-if="detailLoading" class="document-result-state"><FileSearch :size="25" />正在加载详情…</div>
        <div v-else-if="detailError" class="document-result-state document-result-state--error">
          <span>{{ detailError }}</span><button type="button" @click="openDetail(selectedSessionUuid)">重新加载</button>
        </div>
        <template v-else-if="detail">
          <header class="tech-english-history-detail__header">
            <div>
              <span>SESSION DETAIL</span>
              <h2>{{ detail.sourceName || '识图会话' }}</h2>
              <p>{{ detail.scenario || '通用例句语境' }}</p>
            </div>
            <div class="tech-english-history-detail__actions">
              <button class="secondary-button" type="button" @click="exportHistory('markdown')"><Download :size="16" />导出 Markdown</button>
              <button class="secondary-button" type="button" @click="exportHistory('html')"><Download :size="16" />导出 HTML</button>
              <button class="secondary-button tech-english-history-delete-detail" type="button" :disabled="deletingSession === detail.sessionUuid || detail.status === 'PROCESSING'" :title="detail.status === 'PROCESSING' ? '任务处理中，暂不能删除' : '删除识图任务'" @click="deleteSession(detail.sessionUuid)"><Trash2 :size="16" />删除任务</button>
            </div>
          </header>

          <section class="tech-english-history-meta">
            <div><strong>{{ statusLabel(detail.status) }}</strong><span>{{ statusNote(detail.status) }}</span></div>
            <div><strong>{{ detail.imageCount }}</strong><span>截图</span></div>
            <div><strong>{{ detail.itemCount }}</strong><span>语料</span></div>
            <div><strong>{{ detail.tasks.length }}</strong><span>分组</span></div>
          </section>

          <section class="tech-english-ai-tag-picker tech-english-ai-tag-picker--batch">
            <label>知识标签树
              <div class="tech-english-tag-search">
                <Search :size="16" />
                <input v-model="tagSearch" maxlength="80" placeholder="搜索并选择一个标签" @focus="openTagPicker" @blur="closeTagPicker" @input="refreshTagOptions; selectedTagId = null; tagPickerOpen = true" />
                <button v-if="selectedTagId" type="button" title="清除当前标签" aria-label="清除当前标签" @click="clearTagSelection"><X :size="14" /></button>
              </div>
            </label>
            <div v-if="tagPickerOpen" class="tech-english-ai-tag-options">
              <button v-for="tag in filteredTags" :key="tag.id" type="button" @click="selectTag(tag)"><span>{{ tag.path }}</span></button>
            </div>
            <button v-if="selectedTagId" class="tech-english-ai-selected-tag" type="button" @click="openTagPicker">
              <Check :size="14" /><span>{{ tagPath(selectedTagId) }}</span><X :size="13" />
            </button>
            <small>标签可不选；选择当前标签后，可逐条追加、移除，也可对当前分组批量追加或替换。</small>
          </section>

          <div class="tech-english-history-timeline">
            <article v-for="task in detail.tasks" :key="task.batchUuid" class="tech-english-history-task">
              <header>
                <div>
                  <span>分组 {{ task.chunkIndex }} / {{ task.chunkCount }}</span>
                  <strong>{{ task.status }}</strong>
                </div>
                  <div class="tech-english-history-task__actions">
                    <small>{{ task.imageCount }} 张截图 · {{ task.itemCount }} 条语料</small>
                    <button type="button" class="secondary-button" @click="exportBatch(task.batchUuid, 'markdown')"><Download :size="14" />MD</button>
                    <button type="button" class="secondary-button" @click="exportBatch(task.batchUuid, 'html')"><Download :size="14" />HTML</button>
                    <button v-if="task.status === 'RECOGNIZED'" type="button" class="primary-button" :disabled="batchImporting === task.batchUuid" @click="importBatch(task)">
                      <Check :size="14" />{{ batchImporting === task.batchUuid ? '入库中…' : '入库本批次' }}
                    </button>
                    <button v-if="task.status === 'FAILED'" type="button" class="secondary-button" :disabled="Boolean(batchRetrying)" @click="retryBatch(task)">
                      <RefreshCcw :size="14" />{{ batchRetrying === task.batchUuid ? '重试中…' : '重试本组' }}
                    </button>
                    <template v-if="task.status === 'RECOGNIZED'">
                      <button type="button" class="secondary-button" :disabled="!selectedTagId" @click="applyTagToBatch(task, 'append')">批量追加</button>
                      <button type="button" class="secondary-button" :disabled="!selectedTagId" @click="applyTagToBatch(task, 'replace')">批量替换</button>
                      <button type="button" class="secondary-button" @click="clearBatchTags(task)">清空标签</button>
                    </template>
                  </div>
              </header>
              <p v-if="task.errorMessage" class="tech-english-history-task__error">{{ task.errorMessage }}</p>
              <div class="tech-english-history-task__items">
                <article v-for="item in task.items" :key="item.itemKey" class="tech-english-history-task__item">
                  <div class="tech-english-history-task__item-top">
                    <span>{{ corpusTypeLabel(item.corpusType) }}</span>
                    <small>截图 {{ item.sourceImageIndex }}</small>
                  </div>
                  <h3>{{ item.englishText }}</h3>
                  <p v-if="item.translationText">{{ item.translationText }}</p>
                  <section v-if="item.sentencePattern" class="tech-english-history-item__pattern">
                    <small>所属句式</small><strong>{{ readablePattern(item.sentencePattern) }}</strong><p v-if="item.sentencePatternExplanation">{{ item.sentencePatternExplanation }}</p>
                  </section>
                  <div v-if="item.partOfSpeech || item.britishPhonetic || item.americanPhonetic" class="tech-english-history-item__chips">
                    <span v-if="item.partOfSpeech">{{ item.partOfSpeech }}</span>
                    <span v-if="item.britishPhonetic">英 {{ item.britishPhonetic }}</span>
                    <span v-if="item.americanPhonetic">美 {{ item.americanPhonetic }}</span>
                  </div>
                  <div v-if="item.keyVocabulary?.length" class="tech-english-history-item__list">
                    <span v-for="word in item.keyVocabulary ?? []" :key="`${word.word}-${word.partOfSpeech || ''}`">{{ word.word }}</span>
                  </div>
                  <div v-if="item.scenarioTags?.length" class="tech-english-history-item__list">
                    <span v-for="tag in item.scenarioTags ?? []" :key="tag.code">{{ tag.label }}</span>
                  </div>
                      <div v-if="item.examples?.length" class="tech-english-history-item__examples">
                    <div v-for="(example, index) in item.examples ?? []" :key="index">
                      <p>{{ example.englishText }}</p>
                      <small v-if="example.translationText">{{ example.translationText }}</small>
                    </div>
                      </div>
                      <div v-if="task.status === 'RECOGNIZED'" class="tech-english-ai-item__tags">
                        <button v-for="tagId in itemTagAssignments[item.itemKey] ?? []" :key="tagId" type="button" :title="`移除 ${tagPath(tagId)}`" @click="removeTag(item.itemKey, tagId)">
                          <span>{{ tagPath(tagId) }}</span><X :size="12" />
                        </button>
                        <small v-if="!itemTagAssignments[item.itemKey]?.length">未标注</small>
                      </div>
                      <footer v-if="task.status === 'RECOGNIZED'" class="tech-english-ai-item__actions">
                        <select :aria-label="`为 ${item.englishText} 添加知识标签`" @change="addTagFromSelect(item.itemKey, $event)">
                          <option value="">添加标签</option>
                          <option v-for="tag in flatTags" :key="tag.id" :value="tag.id" :disabled="itemTagAssignments[item.itemKey]?.includes(tag.id)">{{ tag.path }}</option>
                        </select>
                        <button class="secondary-button" type="button" :disabled="!selectedTagId" @click="assignTag(item.itemKey)"><Check :size="14" />使用当前标签</button>
                        <button class="secondary-button" type="button" :disabled="!itemTagAssignments[item.itemKey]?.length" @click="clearTags(item.itemKey)"><X :size="14" />清空</button>
                      </footer>
                    </article>
              </div>
            </article>
          </div>
        </template>
      </main>
    </div>
  </section>
</template>
