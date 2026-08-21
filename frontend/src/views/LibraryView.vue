<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { BarChart3, FileText, FileUp, ImagePlus, Link2, MessageCircle, Plus, Search, Send, Trash2, X } from '@lucide/vue'
import CommunityCommentThread from '../components/CommunityCommentThread.vue'
import MarkdownContent from '../components/MarkdownContent.vue'
import { attachmentUrl, createComment, createCommunityPost, deleteCommunityPost, fetchAttachmentPreview, fetchComments, fetchCommunityPosts, uploadCommunityAttachment, vote } from '../services/community'
import { fetchKnowledgeTagTree } from '../services/knowledgeTags'
import type { CommunityAttachment, CommunityAttachmentPreview, CommunityComment, CommunityPost, CommunityPostType, KnowledgeTagNode } from '../types'

const page = ref({ total: 0, page: 1, size: 12, totalPages: 0, items: [] as CommunityPost[] })
const tags = ref<KnowledgeTagNode[]>([])
const keyword = ref('')
const activeTag = ref<number | undefined>()
const showComposer = ref(false)
const posting = ref(false)
const error = ref('')
const action = ref('')
const files = ref<File[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const comments = reactive<Record<number, CommunityComment[]>>({})
const commentsOpen = reactive<Record<number, boolean>>({})
const rootComment = reactive<Record<number, string>>({})
const selectedVote = reactive<Record<number, number[]>>({})
const preview = ref<CommunityAttachmentPreview | null>(null)
const previewLoading = ref(false)
const form = reactive({ postType: 'QUESTION' as CommunityPostType, title: '', markdown: '', linkUrl: '', tagIds: [] as number[], voteQuestion: '', voteOptions: ['', ''], allowMultiple: false, anonymous: false })

const typeMeta: Record<CommunityPostType, string> = { QUESTION: '技术问答', IMAGE: '图片', LINK: '链接分享', FILE: '文件分享', VOTE: '投票' }
const flatTags = computed(() => {
  const output: { id: number; name: string; level: number }[] = []
  const walk = (nodes: KnowledgeTagNode[]) => nodes.forEach(node => { output.push({ id: node.id, name: node.name, level: node.level }); walk(node.children) })
  walk(tags.value)
  return output
})

function formatDate(value: string) { return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value)) }
function fileSize(size: number) { return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(size / 1024)} KB` }
function setType(type: CommunityPostType) { form.postType = type; files.value = []; if (fileInput.value) fileInput.value.value = '' }
function resetForm() { Object.assign(form, { postType: 'QUESTION', title: '', markdown: '', linkUrl: '', tagIds: [], voteQuestion: '', voteOptions: ['', ''], allowMultiple: false, anonymous: false }); files.value = []; if (fileInput.value) fileInput.value.value = '' }
function addOption() { if (form.voteOptions.length < 12) form.voteOptions.push('') }
function onFiles(event: Event) {
  const picked = Array.from((event.target as HTMLInputElement).files ?? [])
  if (form.postType === 'IMAGE' && picked.length > 9) { error.value = '图片最多选择 9 张'; return }
  if (form.postType === 'FILE' && picked.length > 1) { error.value = '文件分享只能选择一个附件'; return }
  files.value = picked
}
async function load(next = 1) {
  error.value = ''
  try { page.value = await fetchCommunityPosts({ keyword: keyword.value.trim(), tagId: activeTag.value, page: next, size: 12 }) }
  catch (exception) { error.value = exception instanceof Error ? exception.message : '分享加载失败' }
}
async function publish() {
  error.value = ''; action.value = ''
  if ((form.postType === 'IMAGE' || form.postType === 'FILE') && !files.value.length) { error.value = '请选择需要分享的附件'; return }
  posting.value = true
  try {
    let post = await createCommunityPost({ ...form, tagIds: [...form.tagIds], voteOptions: form.voteOptions.filter(Boolean) })
    for (const file of files.value) post = await uploadCommunityAttachment(post.id, file)
    resetForm(); showComposer.value = false; action.value = '分享已发布'; await load(1)
  } catch (exception) { error.value = exception instanceof Error ? exception.message : '发布失败' }
  finally { posting.value = false }
}
async function toggleComments(post: CommunityPost) {
  if (!commentsOpen[post.id]) {
    try { comments[post.id] = await fetchComments(post.id) }
    catch (exception) { error.value = exception instanceof Error ? exception.message : '评论加载失败' }
  }
  commentsOpen[post.id] = !commentsOpen[post.id]
}
async function sendComment(postId: number, markdown: string, parentId?: number) {
  if (!markdown.trim()) return
  try {
    await createComment(postId, markdown, parentId)
    comments[postId] = await fetchComments(postId)
    const post = page.value.items.find(item => item.id === postId)
    if (post) post.commentCount += 1
    rootComment[postId] = ''
  } catch (exception) { error.value = exception instanceof Error ? exception.message : '评论发布失败' }
}
async function castVote(post: CommunityPost) {
  const choices = selectedVote[post.id] ?? []
  if (!choices.length) { error.value = '请选择投票选项'; return }
  try {
    const changed = await vote(post.id, choices)
    const index = page.value.items.findIndex(item => item.id === post.id)
    if (index >= 0) page.value.items[index] = changed
    action.value = '投票已提交'
  } catch (exception) { error.value = exception instanceof Error ? exception.message : '投票失败' }
}
function toggleOption(post: CommunityPost, id: number) {
  const current = selectedVote[post.id] ?? []
  selectedVote[post.id] = post.vote?.allowMultiple ? (current.includes(id) ? current.filter(item => item !== id) : [...current, id]) : [id]
}
async function remove(post: CommunityPost) {
  if (!window.confirm(`确认删除“${post.title}”？`)) return
  try { await deleteCommunityPost(post.id); action.value = '分享已删除'; await load(page.value.page) }
  catch (exception) { error.value = exception instanceof Error ? exception.message : '删除失败' }
}
async function openPreview(attachment: CommunityAttachment) {
  if (['pdf', 'jpg', 'jpeg', 'png', 'gif', 'webp'].includes(attachment.extension)) { window.open(attachmentUrl(attachment.id), '_blank', 'noopener'); return }
  previewLoading.value = true; preview.value = { title: attachment.originalName, content: '', truncated: false }
  try { preview.value = await fetchAttachmentPreview(attachment.id) }
  catch (exception) { preview.value = { title: attachment.originalName, content: exception instanceof Error ? exception.message : '预览加载失败', truncated: false } }
  finally { previewLoading.value = false }
}
onMounted(async () => { try { tags.value = await fetchKnowledgeTagTree() } catch { /* tag filtering remains unavailable */ } await load() })
</script>

<template>
  <section class="share-library content-width">
    <header class="share-heading">
      <div><span>SHARE LIBRARY</span><h1>分享库</h1><p>工程实践的提问、资料、图片、网盘链接与投票讨论。</p></div>
      <button class="primary-button" type="button" @click="showComposer = !showComposer"><Plus :size="18" />发布分享</button>
    </header>
    <section v-if="showComposer" class="share-composer">
      <header><div><span>NEW SHARE</span><h2>发布内容</h2></div><button class="close-text" type="button" @click="showComposer = false">收起</button></header>
      <form @submit.prevent="publish">
        <div class="share-type-tabs"><button v-for="(_, type) in typeMeta" :key="type" type="button" :class="{ active: form.postType === type }" @click="setType(type as CommunityPostType)"><MessageCircle v-if="type === 'QUESTION'" :size="16" /><ImagePlus v-else-if="type === 'IMAGE'" :size="16" /><Link2 v-else-if="type === 'LINK'" :size="16" /><FileUp v-else-if="type === 'FILE'" :size="16" /><BarChart3 v-else :size="16" />{{ typeMeta[type as CommunityPostType] }}</button></div>
        <div class="share-form-grid"><label>标题<input v-model.trim="form.title" maxlength="160" required placeholder="用一句话说明要分享的内容" /></label><label>关联知识标签<select v-model="form.tagIds" multiple><option v-for="tag in flatTags" :key="tag.id" :value="tag.id">{{ '　'.repeat(tag.level - 1) }}{{ tag.name }}</option></select><small>最多选择 5 个标签</small></label></div>
        <label v-if="form.postType === 'QUESTION'">问题描述（支持 Markdown）<textarea v-model="form.markdown" required maxlength="50000" placeholder="描述问题、已尝试的方法和期望结果"></textarea></label>
        <label v-if="form.postType === 'LINK'">分享链接<input v-model.trim="form.linkUrl" type="url" required maxlength="2048" placeholder="https://pan.example.com/share" /><small>可分享公开网盘链接；平台不会主动访问该链接。</small></label>
        <template v-if="form.postType === 'IMAGE' || form.postType === 'FILE'"><label>{{ form.postType === 'IMAGE' ? '图片（最多 9 张）' : '文件（仅 1 个，最大 300MB）' }}<input ref="fileInput" type="file" :multiple="form.postType === 'IMAGE'" :accept="form.postType === 'IMAGE' ? 'image/jpeg,image/png,image/gif,image/webp' : '.txt,.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.md,.markdown'" @change="onFiles" /><small v-if="form.postType === 'FILE'">不支持压缩包，压缩资料请使用网盘链接分享。</small></label><div v-if="files.length" class="selected-files"><span v-for="file in files" :key="file.name">{{ file.name }} · {{ fileSize(file.size) }}</span></div></template>
        <template v-if="form.postType === 'VOTE'"><label>投票问题<input v-model.trim="form.voteQuestion" required maxlength="300" /></label><div class="vote-options"><label v-for="(_, index) in form.voteOptions" :key="index">选项 {{ index + 1 }}<input v-model.trim="form.voteOptions[index]" required maxlength="160" /></label><button class="secondary-button" type="button" @click="addOption">增加选项</button></div><div class="vote-settings"><label><input v-model="form.allowMultiple" type="checkbox" />允许多选</label><label><input v-model="form.anonymous" type="checkbox" />匿名投票</label></div></template>
        <footer><span v-if="error" class="form-error">{{ error }}</span><button class="primary-button" :disabled="posting" type="submit"><Send :size="16" />{{ posting ? '发布中...' : '发布分享' }}</button></footer>
      </form>
    </section>
    <p v-if="action" class="share-message">{{ action }}</p>
    <div class="share-layout">
      <aside class="share-filter"><label class="library-search"><Search :size="18" /><input v-model="keyword" placeholder="搜索分享" @keyup.enter="load(1)" /></label><button :class="{ active: !activeTag }" type="button" @click="activeTag = undefined; load(1)">全部分享 <small>{{ page.total }}</small></button><div class="share-filter__label">知识标签</div><button v-for="tag in flatTags" :key="tag.id" :class="{ active: activeTag === tag.id }" type="button" @click="activeTag = tag.id; load(1)">{{ '　'.repeat(tag.level - 1) }}{{ tag.name }}</button></aside>
      <main class="share-feed"><div v-if="error" class="share-error">{{ error }}</div><article v-for="post in page.items" :key="post.id" class="share-card"><header><img :src="post.author.avatarUrl" alt="" /><div><strong>{{ post.author.username }}</strong><small>{{ typeMeta[post.postType] }} · {{ formatDate(post.publishedAt) }}</small></div><button v-if="post.canDelete" class="delete-button" type="button" title="删除分享" @click="remove(post)"><Trash2 :size="16" /></button></header><div class="share-card__body"><div v-if="post.tags.length" class="share-card__tags"><span v-for="tag in post.tags" :key="tag.id">{{ tag.name }}</span></div><h2>{{ post.title }}</h2><MarkdownContent v-if="post.markdown" :markdown="post.markdown" /><a v-if="post.linkUrl" class="shared-link" :href="post.linkUrl" target="_blank" rel="noopener noreferrer"><Link2 :size="17" /><span>{{ post.linkDomain }}</span><small>{{ post.linkUrl }}</small></a><div v-if="post.attachments.some(item => item.attachmentType === 'IMAGE')" class="share-images"><a v-for="attachment in post.attachments" :key="attachment.id" :href="attachmentUrl(attachment.id)" target="_blank"><img :src="attachmentUrl(attachment.id)" :alt="attachment.originalName" /></a></div><div v-if="post.attachments.some(item => item.attachmentType === 'FILE')" class="share-files"><div v-for="attachment in post.attachments" :key="attachment.id"><FileText :size="20" /><span><strong>{{ attachment.originalName }}</strong><small>{{ attachment.extension.toUpperCase() }} · {{ fileSize(attachment.sizeBytes) }}</small></span><button v-if="attachment.previewable" type="button" @click="openPreview(attachment)">预览</button><a :href="attachmentUrl(attachment.id, true)">下载</a></div></div><section v-if="post.vote" class="share-vote"><header><strong>{{ post.vote.question }}</strong><small>{{ post.vote.anonymous ? '匿名投票' : '实名投票' }} · {{ post.vote.allowMultiple ? '可多选' : '单选' }}</small></header><label v-for="option in post.vote.options" :key="option.id" :class="{ chosen: (selectedVote[post.id] ?? []).includes(option.id) }"><input :type="post.vote.allowMultiple ? 'checkbox' : 'radio'" :checked="(selectedVote[post.id] ?? []).includes(option.id)" :disabled="post.vote.voted" @change="toggleOption(post, option.id)" /><span>{{ option.text }}</span><b v-if="post.vote.voted">{{ option.voteCount }}</b></label><footer v-if="!post.vote.voted"><button class="secondary-button" type="button" @click="castVote(post)">提交投票</button></footer><p v-else>已投票 · 共 {{ post.vote.voteCount }} 人参与</p></section></div><footer class="share-card__footer"><button type="button" @click="toggleComments(post)"><MessageCircle :size="16" />讨论 {{ post.commentCount }}</button></footer><section v-if="commentsOpen[post.id]" class="share-comments"><form class="comment-compose" @submit.prevent="sendComment(post.id, rootComment[post.id] || '')"><textarea v-model="rootComment[post.id]" maxlength="8000" placeholder="登录后可参与讨论，支持 Markdown"></textarea><button class="icon-button" type="submit" title="发布评论"><Send :size="16" /></button></form><CommunityCommentThread :comments="comments[post.id] ?? []" @submit="(markdown, parentId) => sendComment(post.id, markdown, parentId)" /></section></article><div v-if="!page.items.length" class="empty-state"><MessageCircle :size="30" /><p>还没有匹配的分享内容</p></div></main>
    </div>
    <div v-if="preview" class="preview-backdrop" role="dialog" aria-modal="true" @click.self="preview = null"><section class="preview-panel"><header><div><small>文档预览</small><h2>{{ preview.title }}</h2></div><button class="icon-button" type="button" title="关闭预览" @click="preview = null"><X :size="18" /></button></header><p v-if="previewLoading" class="preview-state">正在解析文档...</p><pre v-else>{{ preview.content }}</pre><footer v-if="preview.truncated">内容过长，仅展示前半部分。</footer></section></div>
  </section>
</template>
