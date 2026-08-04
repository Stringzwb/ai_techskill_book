<script setup lang="ts">
import { ArrowRight, Bot, Braces, Database, ServerCog } from '@lucide/vue'

const paths = [
  { title: 'Java 后端工程', description: '从语言基础、Spring Boot 到服务稳定性与性能优化。', icon: ServerCog, steps: 18 },
  { title: '现代前端工程', description: '掌握 Vue、TypeScript、组件设计和前端工程化。', icon: Braces, steps: 15 },
  { title: 'AI 应用开发', description: '理解模型调用、RAG、评测与智能体应用落地。', icon: Bot, steps: 16 },
  { title: '数据与架构', description: '建立数据库、缓存、高并发和系统设计能力。', icon: Database, steps: 20 },
]
</script>

<template>
  <section class="inner-page content-width">
    <div class="page-heading">
      <span>LEARNING PATHS</span>
      <h1>成长路径</h1>
      <p>按目标组织专题，减少选择成本。学习进度功能将在内容模块完善后接入。</p>
    </div>
    <div class="path-grid">
      <article v-for="path in paths" :key="path.title" class="path-card">
        <span class="path-icon"><component :is="path.icon" :size="24" /></span>
        <small>{{ path.steps }} 个学习节点</small>
        <h2>{{ path.title }}</h2>
        <p>{{ path.description }}</p>
        <button type="button" disabled>即将开放 <ArrowRight :size="16" /></button>
      </article>
    </div>
  </section>
</template>
