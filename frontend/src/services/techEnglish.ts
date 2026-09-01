import { apiRequest } from './http'
import type {
  TechEnglishAiConfirmPayload,
  TechEnglishAiImportPayload,
  TechEnglishAiImportResponse,
  TechEnglishAiRecognitionResponse,
  TechEnglishCorpusCreatePayload,
  TechEnglishCorpusDetail,
  TechEnglishCorpusPage,
  TechEnglishCorpusType,
  TechEnglishRecognitionHistoryDetail,
  TechEnglishRecognitionHistoryPage,
} from '../types'

/** 查询已发布技术英语语料。 */
export function fetchTechEnglishCorpus(params: {
  keyword?: string
  corpusType?: TechEnglishCorpusType | ''
  tagId?: number
  tagIds?: number[]
  page?: number
  size?: number
} = {}): Promise<TechEnglishCorpusPage> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.corpusType) query.set('corpusType', params.corpusType)
  if (params.tagId) query.set('tagId', String(params.tagId))
  params.tagIds?.filter((tagId) => tagId > 0).forEach((tagId) => query.append('tagIds', String(tagId)))
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 12))
  return apiRequest<TechEnglishCorpusPage>(`/api/tech-english/corpus?${query.toString()}`)
}

/** 读取一条已发布技术英语语料。 */
export function fetchTechEnglishCorpusDetail(id: number): Promise<TechEnglishCorpusDetail> {
  return apiRequest<TechEnglishCorpusDetail>(`/api/tech-english/corpus/${id}`)
}

/** 从主站轻量收录并发布技术英语语料。 */
export function createTechEnglishCorpus(payload: TechEnglishCorpusCreatePayload): Promise<TechEnglishCorpusDetail> {
  const body = new FormData()
  body.append('corpusType', payload.corpusType)
  if (payload.title) body.append('title', payload.title)
  if (payload.englishText) body.append('englishText', payload.englishText)
  if (payload.phonetic) body.append('phonetic', payload.phonetic)
  if (payload.explanation) body.append('explanation', payload.explanation)
  if (payload.articleMarkdown) body.append('articleMarkdown', payload.articleMarkdown)
  if (payload.imageFile) body.append('imageFile', payload.imageFile)
  if (payload.imageAlt) body.append('imageAlt', payload.imageAlt)
  if (payload.sourceName) body.append('sourceName', payload.sourceName)
  if (payload.sourceUrl) body.append('sourceUrl', payload.sourceUrl)
  if (payload.scenario) body.append('scenario', payload.scenario)
  if (payload.difficulty) body.append('difficulty', payload.difficulty)
  if (payload.translationText) body.append('translationText', payload.translationText)
  payload.tagIds.forEach((tagId) => body.append('tagIds', String(tagId)))
  payload.vocabularyExamples?.forEach((example) => {
    body.append('exampleEnglishTexts', example.englishText)
    body.append('exampleTranslationTexts', example.translationText)
  })
  if (payload.syncExamplesToSentences) body.append('syncExamplesToSentences', 'true')
  return apiRequest<TechEnglishCorpusDetail>('/api/tech-english/corpus', {
    method: 'POST',
    body,
  })
}

/** 上传单个分组截图，由 AI 识别并返回等待确认的草稿。 */
export function importTechEnglishScreenshots(payload: TechEnglishAiImportPayload): Promise<TechEnglishAiRecognitionResponse> {
  const body = new FormData()
  body.append('sessionUuid', payload.sessionUuid)
  body.append('chunkIndex', String(payload.chunkIndex))
  body.append('chunkCount', String(payload.chunkCount))
  body.append('scenario', payload.scenario)
  body.append('exampleCount', String(payload.exampleCount))
  payload.images?.forEach((image) => body.append('images', image))
  return apiRequest<TechEnglishAiRecognitionResponse>('/api/tech-english/imports/screenshots', {
    method: 'POST',
    body,
  })
}

/** 选择知识标签后确认识别草稿，正式保存截图和语料。 */
export function confirmTechEnglishScreenshotImport(payload: TechEnglishAiConfirmPayload): Promise<TechEnglishAiImportResponse> {
  const body = new FormData()
  body.append('itemTagAssignments', JSON.stringify(payload.itemTagAssignments))
  payload.images?.forEach((image) => body.append('images', image))
  return apiRequest<TechEnglishAiImportResponse>(`/api/tech-english/imports/screenshots/${payload.batchUuid}/confirm`, {
    method: 'POST',
    body,
  })
}

/** 查询当前用户的识图记录列表。 */
export function fetchTechEnglishRecognitionHistory(params: { page?: number; size?: number } = {}): Promise<TechEnglishRecognitionHistoryPage> {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 10))
  return apiRequest<TechEnglishRecognitionHistoryPage>(`/api/tech-english/imports/history?${query.toString()}`)
}

/** 查询当前用户的一次识图会话详情。 */
export function fetchTechEnglishRecognitionHistoryDetail(sessionUuid: string): Promise<TechEnglishRecognitionHistoryDetail> {
  return apiRequest<TechEnglishRecognitionHistoryDetail>(`/api/tech-english/imports/history/${sessionUuid}`)
}

/** 导出当前用户的一次识图会话。 */
export function downloadTechEnglishRecognitionHistory(sessionUuid: string, format: 'markdown' | 'html') {
  return `/api/tech-english/imports/history/${sessionUuid}/export?format=${format}`
}

/** 导出识图会话中的单个批次。 */
export function downloadTechEnglishRecognitionBatchHistory(sessionUuid: string, batchUuid: string, format: 'markdown' | 'html') {
  return `/api/tech-english/imports/history/${sessionUuid}/batches/${batchUuid}/export?format=${format}`
}
