import { apiRequest } from './http'
import type { TechEnglishCorpusDetail, TechEnglishCorpusPage, TechEnglishCorpusType } from '../types'

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
