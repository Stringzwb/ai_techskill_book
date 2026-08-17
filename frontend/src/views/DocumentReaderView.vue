<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, CalendarDays, Clock3, FileText } from '@lucide/vue'
import { useRoute } from 'vue-router'
import MarkdownContent from '../components/MarkdownContent.vue'
import { fetchDocument } from '../services/documents'
import type { DocumentDetail } from '../types'

const route = useRoute()
const document = ref<DocumentDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const documentId = computed(() => Number(route.params.id))

/** 格式化文档发布时间。 */
function formatDate(value: string | null): string {
  if (!value) return '尚未发布'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(new Date(value))
}

/** 加载路由指定的已发布文档。 */
async function loadDocument(): Promise<void> {
  if (!Number.isInteger(documentId.value) || documentId.value <= 0) {
    errorMessage.value = '文档地址不正确'
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    document.value = await fetchDocument(documentId.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDocument)
</script>

<template>
  <section class="document-reader-page">
    <div v-if="loading" class="reader-state"><FileText :size="25" />正在读取文档…</div>
    <div v-else-if="errorMessage || !document" class="reader-state reader-state--error">
      <strong>{{ errorMessage || '文档不存在' }}</strong>
      <RouterLink class="secondary-button" to="/documents"><ArrowLeft :size="16" />返回文档库</RouterLink>
    </div>
    <template v-else>
      <header class="reader-header">
        <div class="reader-header__inner">
          <RouterLink class="reader-back" to="/documents"><ArrowLeft :size="16" />文档库</RouterLink>
          <div class="reader-tags">
            <span v-for="tag in document.tags" :key="tag.id">{{ tag.name }}</span>
          </div>
          <h1>{{ document.title }}</h1>
          <p>{{ document.summary }}</p>
          <div class="reader-meta">
            <span><Clock3 :size="15" />约 {{ document.readingMinutes }} 分钟</span>
            <span><CalendarDays :size="15" />{{ formatDate(document.publishedAt) }}</span>
          </div>
        </div>
      </header>
      <main class="reader-content">
        <MarkdownContent :markdown="document.markdown" />
      </main>
    </template>
  </section>
</template>
