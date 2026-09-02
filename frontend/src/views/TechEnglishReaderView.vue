<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, BookOpenText, CalendarDays, Check, ExternalLink, Pencil, Quote, Save, Sparkles, Volume2, X } from '@lucide/vue'
import { useRoute } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import { fetchTechEnglishCorpusDetail, fetchTechEnglishScenarioTags, saveTechEnglishVocabularyExampleAsSentence, updateTechEnglishCorpusMetadata } from '../services/techEnglish'
import { authStore } from '../stores/auth'
import type { KnowledgeTagNode, TechEnglishCorpusDetail, TechEnglishCorpusType, TechEnglishDifficulty, TechEnglishScenarioTag, TechEnglishVocabularyExample } from '../types'

interface FlatTagOption {
  id: number
  name: string
  level: 1 | 2 | 3
  path: string
}

const route = useRoute()
const corpus = ref<TechEnglishCorpusDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const savingExampleId = ref<number | null>(null)
const savedExampleId = ref<number | null>(null)
const failedExampleId = ref<number | null>(null)
const exampleSaveError = ref('')
const metadataEditing = ref(false)
const metadataSaving = ref(false)
const metadataError = ref('')
const tagTree = ref<KnowledgeTagNode[]>([])
const scenarioTagOptions = ref<TechEnglishScenarioTag[]>([])
const selectedMetadataTagIds = ref<number[]>([])
const selectedScenarioTagCodes = ref<string[]>([])

const corpusId = computed(() => Number(route.params.id))

/** 转换语料类型展示文案。 */
function typeLabel(value: TechEnglishCorpusType): string {
  const labels: Record<TechEnglishCorpusType, string> = {
    VOCABULARY: '词汇',
    PHRASE: '短语',
    SENTENCE: '句式',
    ARTICLE: '英语文章',
  }
  return labels[value] ?? value
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
  walk(tagTree.value)
  return options
})

/** 进入详情标签编辑态。 */
function startMetadataEdit(): void {
  if (!corpus.value) return
  selectedMetadataTagIds.value = corpus.value.knowledgeTags.map((tag) => tag.id)
  selectedScenarioTagCodes.value = corpus.value.scenarioTags.map((tag) => tag.code)
  metadataError.value = ''
  metadataEditing.value = true
}

/** 放弃当前详情标签编辑。 */
function cancelMetadataEdit(): void {
  metadataEditing.value = false
  metadataError.value = ''
}

/** 保存详情页标签。 */
async function saveMetadata(): Promise<void> {
  if (!corpus.value || metadataSaving.value) return
  metadataSaving.value = true
  metadataError.value = ''
  try {
    corpus.value = await updateTechEnglishCorpusMetadata(corpus.value.id, {
      tagIds: selectedMetadataTagIds.value.map(Number).filter((id) => Number.isSafeInteger(id) && id > 0),
      scenarioTagCodes: selectedScenarioTagCodes.value,
    })
    metadataEditing.value = false
  } catch (error) {
    metadataError.value = error instanceof Error ? error.message : '标签保存失败'
  } finally {
    metadataSaving.value = false
  }
}

/** 加载详情页标签编辑选项。 */
async function loadMetadataOptions(): Promise<void> {
  try {
    const [tags, scenarios] = await Promise.all([
      fetchKnowledgeTagTree(),
      fetchTechEnglishScenarioTags(),
    ])
    tagTree.value = tags
    scenarioTagOptions.value = scenarios
  } catch {
    tagTree.value = []
    scenarioTagOptions.value = []
  }
}

/** 转换难度展示文案。 */
function difficultyLabel(value: TechEnglishDifficulty): string {
  const labels: Record<TechEnglishDifficulty, string> = { BEGINNER: '入门', INTERMEDIATE: '中级', ADVANCED: '高级' }
  return labels[value] ?? value
}

/** 格式化发布时间。 */
function formatDate(value: string | null): string {
  if (!value) return '尚未发布'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(new Date(value))
}

/** 加载路由指定的技术英语语料。 */
async function loadCorpus(): Promise<void> {
  if (!Number.isInteger(corpusId.value) || corpusId.value <= 0) {
    errorMessage.value = '语料地址不正确'
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    corpus.value = await fetchTechEnglishCorpusDetail(corpusId.value)
    selectedMetadataTagIds.value = corpus.value.knowledgeTags.map((tag) => tag.id)
    selectedScenarioTagCodes.value = corpus.value.scenarioTags.map((tag) => tag.code)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '语料加载失败'
  } finally {
    loading.value = false
  }
}

/** 按用户操作将一条例句保存为独立的技术句子语料。 */
async function saveExampleAsSentence(example: TechEnglishVocabularyExample): Promise<void> {
  if (!corpus.value || savingExampleId.value !== null) return
  savingExampleId.value = example.id
  savedExampleId.value = null
  failedExampleId.value = null
  exampleSaveError.value = ''
  try {
    const sentence = await saveTechEnglishVocabularyExampleAsSentence(corpus.value.id, example.id)
    example.sentenceCorpusId = sentence.id
    savedExampleId.value = example.id
  } catch (error) {
    failedExampleId.value = example.id
    exampleSaveError.value = error instanceof Error ? error.message : '保存句子语料失败，请稍后重试'
  } finally {
    savingExampleId.value = null
  }
}

onMounted(() => {
  void loadCorpus()
  void loadMetadataOptions()
})
</script>

<template>
  <section class="document-reader-page">
    <div v-if="loading" class="reader-state"><BookOpenText :size="25" />正在读取语料…</div>
    <div v-else-if="errorMessage || !corpus" class="reader-state reader-state--error">
      <strong>{{ errorMessage || '语料不存在' }}</strong>
      <RouterLink class="secondary-button" to="/tech-english"><ArrowLeft :size="16" />返回技术英语</RouterLink>
    </div>
    <template v-else>
      <header class="reader-header tech-english-reader-header">
        <div class="reader-header__inner">
          <RouterLink class="reader-back" to="/tech-english"><ArrowLeft :size="16" />技术英语</RouterLink>
          <div class="reader-tags">
            <span>{{ typeLabel(corpus.corpusType) }}</span>
            <span>{{ difficultyLabel(corpus.difficulty) }}</span>
            <span v-for="tag in corpus.scenarioTags" :key="tag.code" class="scene-tag">{{ tag.label }}</span>
            <span v-for="tag in corpus.knowledgeTags" :key="tag.id">{{ tag.name }}</span>
          </div>
          <h1>{{ corpus.title }}</h1>
          <p>{{ corpus.explanation || corpus.translationText || corpus.imageAlt || '技术英语语料详情' }}</p>
          <div class="reader-meta">
            <span><CalendarDays :size="15" />{{ formatDate(corpus.publishedAt) }}</span>
            <span v-if="corpus.scenario">{{ corpus.scenario }}</span>
            <a v-if="corpus.sourceUrl" :href="corpus.sourceUrl" target="_blank" rel="noopener noreferrer">
              <ExternalLink :size="15" />{{ corpus.sourceName || '来源' }}
            </a>
            <span v-else-if="corpus.sourceName">来源 · {{ corpus.sourceName }}</span>
            <button v-if="authStore.state.user && !metadataEditing" class="share-trigger" type="button" @click="startMetadataEdit">
              <Pencil :size="14" />编辑标签
            </button>
          </div>
        </div>
      </header>
      <main class="reader-content tech-english-reader-content">
        <section v-if="metadataEditing" class="tech-english-detail-block tech-english-metadata-editor">
          <header>
            <div><small>METADATA</small><h2>标签编辑</h2></div>
            <button type="button" class="icon-button" title="取消编辑" aria-label="取消编辑" @click="cancelMetadataEdit"><X :size="16" /></button>
          </header>
          <div class="tech-english-metadata-editor__grid">
            <label>场景标签
              <select v-model="selectedScenarioTagCodes" multiple size="8">
                <option v-for="tag in scenarioTagOptions" :key="tag.code" :value="tag.code">{{ tag.label }}</option>
              </select>
            </label>
            <label>知识标签
              <select v-model="selectedMetadataTagIds" multiple size="8">
                <option v-for="tag in flatTags" :key="tag.id" :value="tag.id">{{ `${'  '.repeat(tag.level - 1)}${tag.name}` }}</option>
              </select>
            </label>
          </div>
          <p v-if="metadataError" class="form-error">{{ metadataError }}</p>
          <footer>
            <button class="secondary-button" type="button" @click="cancelMetadataEdit">取消</button>
            <button class="primary-button" type="button" :disabled="metadataSaving" @click="saveMetadata">
              <Save :size="15" />{{ metadataSaving ? '保存中...' : '保存标签' }}
            </button>
          </footer>
        </section>
        <section v-if="corpus.englishText" class="tech-english-detail-block">
          <small>ENGLISH</small>
          <p class="tech-english-detail-english">{{ corpus.englishText }}</p>
          <div v-if="corpus.partOfSpeech || corpus.britishPhonetic || corpus.americanPhonetic" class="tech-english-pronunciations">
            <strong v-if="corpus.partOfSpeech">{{ corpus.partOfSpeech }}</strong>
            <span v-if="corpus.britishPhonetic"><Volume2 :size="14" />英 {{ corpus.britishPhonetic }}</span>
            <span v-if="corpus.americanPhonetic"><Volume2 :size="14" />美 {{ corpus.americanPhonetic }}</span>
          </div>
          <span v-else-if="corpus.phonetic">{{ corpus.phonetic }}</span>
        </section>
        <section v-if="corpus.sentencePattern" class="tech-english-detail-block tech-english-pattern-card">
          <small><Quote :size="14" /> SENTENCE FRAMEWORK</small>
          <h2>{{ corpus.sentencePattern }}</h2>
          <p v-if="corpus.sentencePatternExplanation">{{ corpus.sentencePatternExplanation }}</p>
        </section>
        <section v-if="corpus.keyVocabulary.length" class="tech-english-detail-block">
          <small><Sparkles :size="14" /> KEY VOCABULARY</small>
          <div class="tech-english-keyword-grid">
            <article v-for="word in corpus.keyVocabulary" :key="`${word.word}-${word.partOfSpeech || ''}`">
              <header><strong>{{ word.word }}</strong><span v-if="word.partOfSpeech">{{ word.partOfSpeech }}</span></header>
              <p v-if="word.meaning">{{ word.meaning }}</p>
            </article>
          </div>
        </section>
        <section v-if="corpus.patternExamples.length" class="tech-english-detail-block tech-english-example-detail">
          <small>PATTERN EXAMPLES</small>
          <div v-for="(example, index) in corpus.patternExamples" :key="index" class="tech-english-example-detail__item">
            <p class="tech-english-detail-english">{{ example.englishText }}</p>
            <span v-if="example.translationText">{{ example.translationText }}</span>
          </div>
        </section>
        <section v-if="corpus.translationText" class="tech-english-detail-block">
          <small>TRANSLATION</small>
          <p>{{ corpus.translationText }}</p>
        </section>
        <section v-if="corpus.vocabularyExamples.length" class="tech-english-detail-block tech-english-example-detail">
          <small>EXAMPLES</small>
          <div v-for="example in corpus.vocabularyExamples" :key="example.id" class="tech-english-example-detail__item">
            <p class="tech-english-detail-english">{{ example.englishText }}</p>
            <span v-if="example.translationText">{{ example.translationText }}</span>
            <RouterLink v-if="example.sentenceCorpusId" :to="`/tech-english/${example.sentenceCorpusId}`">已同步到句子语料</RouterLink>
            <button v-else-if="authStore.state.user" class="tech-english-example-save" type="button" :disabled="savingExampleId !== null" @click="saveExampleAsSentence(example)">
              <Check :size="14" />{{ savingExampleId === example.id ? '保存中…' : '保存为句子语料' }}
            </button>
            <RouterLink v-else class="tech-english-example-save" to="/login">登录后保存为句子语料</RouterLink>
            <small v-if="savedExampleId === example.id" class="tech-english-example-save__message">已保存，可打开句子语料查看。</small>
            <small v-if="failedExampleId === example.id" class="tech-english-example-save__error">{{ exampleSaveError }}</small>
          </div>
        </section>
        <figure v-if="corpus.imageUrl && !corpus.importBatchUuid" class="tech-english-detail-image">
          <img :src="corpus.imageUrl" :alt="corpus.imageAlt || corpus.title" />
          <figcaption>{{ corpus.imageAlt || '语料来源截图' }}</figcaption>
        </figure>
        <MarkdownContent v-if="corpus.articleMarkdown" :markdown="corpus.articleMarkdown" />
      </main>
    </template>
  </section>
</template>
