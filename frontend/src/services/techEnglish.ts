import { apiRequest } from './http'
import type { TechEnglishCorpusCreatePayload, TechEnglishCorpusDetail, TechEnglishCorpusPage, TechEnglishCorpusType } from '../types'

/** 查询已发布技术英语语料。 */
export function fetchTechEnglishCorpus(params: {
  keyword?: string
  corpusType?: TechEnglishCorpusType | ''
  tagId?: number
  page?: number
  size?: number
} = {}): Promise<TechEnglishCorpusPage> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.corpusType) query.set('corpusType', params.corpusType)
  if (params.tagId) query.set('tagId', String(params.tagId))
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
  body.append('title', payload.title)
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
  return apiRequest<TechEnglishCorpusDetail>('/api/tech-english/corpus', {
    method: 'POST',
    body,
  })
}
