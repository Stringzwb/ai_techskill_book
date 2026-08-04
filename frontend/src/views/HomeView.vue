<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, BookOpen, Code2, Compass, Sparkles } from '@lucide/vue'
import { fetchHome } from '../services/home'
import type { HomeResponse } from '../types'

const home = ref<HomeResponse | null>(null)

/** 加载首页公开内容，接口不可用时服务层会返回备用数据。 */
onMounted(async () => {
  const response = await fetchHome()
  home.value = response.data
})
</script>

<template>
  <div class="home-page">
    <section class="home-hero content-width">
      <div class="hero-copy">
        <span class="eyebrow"><Sparkles :size="15" /> 面向技术岗位的知识库</span>
        <h1>少一点信息噪音，<br /><span>多一点可执行的成长。</span></h1>
        <p>把后端、前端、AI、数据与架构知识整理成清晰的专题和学习路径，帮助你更高效地理解、练习和复盘。</p>
        <div class="hero-actions">
          <RouterLink class="primary-button" to="/library">进入知识库 <ArrowRight :size="17" /></RouterLink>
          <RouterLink class="secondary-button" to="/paths">查看成长路径</RouterLink>
        </div>
      </div>
      <div class="hero-summary">
        <div class="summary-icon"><BookOpen :size="27" /></div>
        <strong>{{ home?.articleCount ?? '—' }}</strong>
        <span>篇工程实践专题</span>
        <div class="summary-divider"></div>
        <p>首页无需登录即可浏览概览；进入知识库、成长路径和个人中心后需要登录。</p>
      </div>
    </section>

    <section class="home-section content-width">
      <div class="section-title">
        <div><span>知识方向</span><h2>围绕真实工作建立能力</h2></div>
        <RouterLink to="/library">查看全部 <ArrowRight :size="16" /></RouterLink>
      </div>
      <div class="simple-category-grid">
        <article v-for="category in home?.categories ?? []" :key="category.code" class="simple-card">
          <span class="card-code">{{ category.code }}</span>
          <h3>{{ category.name }}</h3>
          <p>{{ category.description }}</p>
          <small>{{ category.articleCount }} 篇内容</small>
        </article>
      </div>
    </section>

    <section class="home-callout content-width">
      <span class="callout-icon"><Code2 :size="24" /></span>
      <div><small>从一个明确方向开始</small><h2>用路径组织学习，用专题解决问题。</h2></div>
      <RouterLink class="primary-button" to="/register">免费注册 <Compass :size="17" /></RouterLink>
    </section>
  </div>
</template>
