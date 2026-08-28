<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, BookOpenText, CalendarDays, ExternalLink } from '@lucide/vue'
import { useRoute } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { fetchTechEnglishCorpusDetail } from '../services/techEnglish'
import type { TechEnglishCorpusDetail, TechEnglishCorpusType, TechEnglishDifficulty } from '../types'

const route = useRoute()
const corpus = ref<TechEnglishCorpusDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

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
          </div>
        </div>
      </header>
      <main class="reader-content tech-english-reader-content">
        <section v-if="corpus.englishText" class="tech-english-detail-block">
          <small>ENGLISH</small>
          <p class="tech-english-detail-english">{{ corpus.englishText }}</p>
          <span v-if="corpus.phonetic">{{ corpus.phonetic }}</span>
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
          </div>
        </section>
        <figure v-if="corpus.corpusType === 'IMAGE' && corpus.imageUrl" class="tech-english-detail-image">
          <img :src="corpus.imageUrl" :alt="corpus.imageAlt || corpus.title" />
          <figcaption v-if="corpus.imageAlt">{{ corpus.imageAlt }}</figcaption>
        </figure>
        <MarkdownContent v-if="corpus.articleMarkdown" :markdown="corpus.articleMarkdown" />
      </main>
    </template>
  </section>
</template>
