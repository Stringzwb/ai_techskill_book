<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, BookOpenText, CalendarDays, Check, ExternalLink, Quote, Sparkles, Volume2 } from '@lucide/vue'
import { useRoute } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { fetchTechEnglishCorpusDetail, saveTechEnglishVocabularyExampleAsSentence } from '../services/techEnglish'
import { authStore } from '../stores/auth'
import type { TechEnglishCorpusDetail, TechEnglishCorpusType, TechEnglishDifficulty, TechEnglishVocabularyExample } from '../types'

const route = useRoute()
const corpus = ref<TechEnglishCorpusDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const savingExampleId = ref<number | null>(null)
const savedExampleId = ref<number | null>(null)
const failedExampleId = ref<number | null>(null)
const exampleSaveError = ref('')

const corpusId = computed(() => Number(route.params.id))

/** 转换语料类型展示文案。 */
function typeLabel(value: TechEnglishCorpusType): string {
  const labels: Record<TechEnglishCorpusType, string> = {
    VOCABULARY: '技术词汇',
    SENTENCE: '技术语句',
    IMAGE: '语料图片',
    ARTICLE: '英语文章',
  }
  return labels[value] ?? value
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

onMounted(loadCorpus)
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
          </div>
        </div>
      </header>
      <main class="reader-content tech-english-reader-content">
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
          <small><Quote :size="14" /> CLASSIC PATTERN</small>
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
