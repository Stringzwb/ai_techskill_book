<script setup lang="ts">
import { ref } from 'vue'
import { CornerDownRight, Send } from '@lucide/vue'
import MarkdownContent from './MarkdownContent.vue'
import type { CommunityComment } from '../types'
defineOptions({ name: 'CommunityCommentThread' })
const props=defineProps<{ comments: CommunityComment[]; depth?: number }>()
const emit=defineEmits<{ submit:[markdown:string,parentId:number] }>()
const replying=ref<number|null>(null);const text=ref('')
function send(parentId:number){if(!text.value.trim())return;emit('submit',text.value.trim(),parentId);text.value='';replying.value=null}
function format(value:string){return new Intl.DateTimeFormat('zh-CN',{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(value))}
</script>
<template><div class="comment-thread"><article v-for="comment in props.comments" :key="comment.id" class="comment-item"><header><img :src="comment.author.avatarUrl" alt="" /><strong>{{ comment.author.username }}</strong><time>{{ format(comment.createdAt) }}</time></header><MarkdownContent :markdown="comment.markdown" /><button class="comment-reply" type="button" @click="replying=replying===comment.id?null:comment.id"><CornerDownRight :size="14" />追问</button><form v-if="replying===comment.id" class="comment-compose" @submit.prevent="send(comment.id)"><textarea v-model="text" maxlength="8000" placeholder="继续追问…"></textarea><button class="icon-button" type="submit" title="发布追问"><Send :size="16" /></button></form><CommunityCommentThread v-if="comment.children.length" :comments="comment.children" :depth="(props.depth ?? 0)+1" @submit="(markdown,parentId)=>emit('submit',markdown,parentId)" /></article></div></template>
