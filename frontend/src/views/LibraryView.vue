<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BookOpen, Search } from '@lucide/vue'
import { fetchHome } from '../services/home'
import type { ArticleSummary } from '../types'

const articles = ref<ArticleSummary[]>([])
const query = ref('')

const filteredArticles = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return articles.value
  return articles.value.filter((article) =>
    [article.title, article.summary, article.category, ...article.tags].join(' ').toLowerCase().includes(keyword),
  )
})

/** 加载当前可展示的知识专题。 */
onMounted(async () => {
  const response = await fetchHome()
  articles.value = response.data.featuredArticles
})
</script>

<template>
  <section class="inner-page content-width">
    <div class="page-heading">
      <span>KNOWLEDGE LIBRARY</span>
      <h1>知识库</h1>
      <p>从工程问题出发，查找可直接用于学习和实践的专题。</p>
    </div>
    <label class="library-search"><Search :size="19" /><input v-model="query" placeholder="搜索标题、方向或标签" /></label>
    <div class="article-list">
      <article v-for="article in filteredArticles" :key="article.id" class="library-card">
        <div><span>{{ article.category }}</span><small>{{ article.difficulty }} · {{ article.readMinutes }} 分钟</small></div>
        <h2>{{ article.title }}</h2>
        <p>{{ article.summary }}</p>
        <footer><span v-for="tag in article.tags" :key="tag"># {{ tag }}</span></footer>
      </article>
      <div v-if="!filteredArticles.length" class="empty-state"><BookOpen :size="30" /><p>没有找到匹配的内容</p></div>
    </div>
  </section>
</template>
