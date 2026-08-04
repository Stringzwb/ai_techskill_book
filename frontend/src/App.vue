<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <main class="page-main">
      <RouterView />
    </main>
    <footer class="site-footer">
      <span>技术岗 AI 知识库</span>
      <span>结构化知识 · 工程化实践 · 持续成长</span>
    </footer>
  </div>
</template>
