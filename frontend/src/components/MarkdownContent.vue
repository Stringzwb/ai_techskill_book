<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'

const props = defineProps<{
  markdown: string
}>()

marked.use({ gfm: true, breaks: true })

// Markdown 先转为 HTML，再移除脚本、事件属性等不可信内容。
const safeHtml = computed(() => DOMPurify.sanitize(marked.parse(props.markdown) as string))
</script>

<template>
  <article class="markdown-body" v-html="safeHtml"></article>
</template>
