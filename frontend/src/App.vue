<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  BookOpen,
  Bot,
  Braces,
  ChevronRight,
  CloudCog,
  Code2,
  Database,
  Layers3,
  Search,
  ShieldCheck,
  Sparkles,
  TerminalSquare,
} from '@lucide/vue'
import { fetchHome } from './services/home'
import type { HomeResponse } from './types'

const home = ref<HomeResponse | null>(null)
const query = ref('')
const apiOnline = ref(false)

const categoryIcons = [Code2, Braces, Bot, Database, CloudCog, Layers3]

const filteredArticles = computed(() => {
  const value = query.value.trim().toLowerCase()
  const articles = home.value?.featuredArticles ?? []
  if (!value) return articles
  return articles.filter((article) =>
    [article.title, article.summary, article.category, ...article.tags]
      .join(' ')
      .toLowerCase()
      .includes(value),
  )
})

const scrollToKnowledge = () => {
  document.querySelector('#featured')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(async () => {
  const response = await fetchHome()
  home.value = response.data
  apiOnline.value = response.online
})
</script>

<template>
  <div class="site-shell">
    <div class="ambient ambient-one"></div>
    <div class="ambient ambient-two"></div>

    <header class="topbar">
      <a class="brand" href="#" aria-label="技术岗AI知识库首页">
        <span class="brand-mark"><TerminalSquare :size="23" /></span>
        <span>技术岗<span class="brand-accent">AI</span>知识库</span>
      </a>
      <nav class="desktop-nav" aria-label="主导航">
        <a class="active" href="#categories">知识领域</a>
        <a href="#featured">精选专题</a>
        <a href="#roadmap">成长路径</a>
      </nav>
      <button class="nav-action" type="button" @click="scrollToKnowledge">
        开始学习 <ArrowRight :size="16" />
      </button>
    </header>

    <main>
      <section class="hero">
        <div class="hero-copy">
          <div class="eyebrow"><Sparkles :size="15" /> AI 驱动的技术成长系统</div>
          <h1>
            把复杂技术，变成<br />
            <span>可执行的成长路径</span>
          </h1>
          <p class="hero-description">
            聚合后端、前端、AI、数据与云原生核心知识。用结构化内容和清晰路线，帮你把每一次学习都转化为工程能力。
          </p>

          <label class="search-box">
            <Search :size="21" />
            <input v-model="query" type="search" placeholder="搜索技术主题、框架或实践…" />
            <kbd>⌘ K</kbd>
          </label>

          <div v-if="home" class="hero-stats">
            <div><strong>{{ home.articleCount }}+</strong><span>知识条目</span></div>
            <i></i>
            <div><strong>{{ home.categoryCount }}</strong><span>技术领域</span></div>
            <i></i>
            <div><strong>{{ home.learningPathCount }}</strong><span>成长路径</span></div>
            <span class="api-state" :class="{ online: apiOnline }">
              {{ apiOnline ? 'API 已连接' : '预览数据' }}
            </span>
          </div>
        </div>

        <aside id="roadmap" class="learning-card">
          <div class="card-glow"></div>
          <div class="learning-header">
            <span><BookOpen :size="19" /> 今日学习路线</span>
            <span class="progress-label">进度 68%</span>
          </div>
          <div class="progress-track"><span></span></div>
          <div class="learning-list">
            <article class="learning-item completed">
              <span class="step-icon"><ShieldCheck :size="18" /></span>
              <div><small>01 · 基础巩固</small><h3>Java 并发核心模型</h3></div>
              <span class="step-time">已完成</span>
            </article>
            <article class="learning-item current">
              <span class="step-icon"><Bot :size="18" /></span>
              <div><small>02 · 工程实践</small><h3>Spring Boot + AI</h3></div>
              <span class="step-time">32 分钟</span>
            </article>
            <article class="learning-item">
              <span class="step-icon"><Database :size="18" /></span>
              <div><small>03 · 系统进阶</small><h3>数据与检索架构</h3></div>
              <span class="step-time">45 分钟</span>
            </article>
          </div>
          <button type="button" class="continue-button" @click="scrollToKnowledge">
            继续今日学习 <ChevronRight :size="18" />
          </button>
        </aside>
      </section>

      <section id="categories" class="section categories-section">
        <div class="section-heading">
          <div><span class="section-kicker">KNOWLEDGE MAP</span><h2>构建完整的技术能力图谱</h2></div>
          <p>从单点知识到系统能力，覆盖技术岗核心成长方向。</p>
        </div>
        <div v-if="home" class="category-grid">
          <article v-for="(category, index) in home.categories" :key="category.code" class="category-card">
            <div class="category-icon"><component :is="categoryIcons[index]" :size="23" /></div>
            <div class="category-meta"><span>{{ category.code }}</span><span>{{ category.articleCount }} 篇</span></div>
            <h3>{{ category.name }}</h3>
            <p>{{ category.description }}</p>
            <a href="#featured">探索该领域 <ArrowRight :size="15" /></a>
          </article>
        </div>
      </section>

      <section id="featured" class="section featured-section">
        <div class="section-heading">
          <div><span class="section-kicker">EDITOR'S PICK</span><h2>{{ query ? '搜索结果' : '本周精选专题' }}</h2></div>
          <p>由工程实践提炼出的高价值内容，强调方法与落地。</p>
        </div>
        <div class="article-grid">
          <article v-for="article in filteredArticles" :key="article.id" class="article-card">
            <div class="article-topline">
              <span>{{ article.category }}</span>
              <span>{{ article.difficulty }} · {{ article.readMinutes }} min</span>
            </div>
            <h3>{{ article.title }}</h3>
            <p>{{ article.summary }}</p>
            <div class="tag-row"><span v-for="tag in article.tags" :key="tag"># {{ tag }}</span></div>
            <button type="button">阅读专题 <ArrowRight :size="16" /></button>
          </article>
          <div v-if="!filteredArticles.length" class="empty-state">
            <Search :size="28" />
            <h3>没有匹配的专题</h3>
            <p>换一个关键词，或清空搜索查看全部内容。</p>
          </div>
        </div>
      </section>

      <section class="cta-section">
        <div class="cta-icon"><Sparkles :size="26" /></div>
        <div><span>从今天开始积累复利</span><h2>让 AI 成为你的技术成长搭档</h2></div>
        <button type="button" @click="scrollToKnowledge">进入知识库 <ArrowRight :size="17" /></button>
      </section>
    </main>

    <footer>
      <div class="brand footer-brand"><span class="brand-mark"><TerminalSquare :size="20" /></span>技术岗AI知识库</div>
      <p>结构化知识 · 工程化实践 · 持续成长</p>
      <span>Built for builders.</span>
    </footer>
  </div>
</template>
