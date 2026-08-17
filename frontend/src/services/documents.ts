import { apiRequest } from './http'
import type { DocumentDetail, DocumentPage } from '../types'

/** 查询已发布文档。 */
export function fetchDocuments(params: {
  keyword?: string
  tagId?: number
  page?: number
  size?: number
} = {}): Promise<DocumentPage> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.tagId) query.set('tagId', String(params.tagId))
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 12))
  return apiRequest<DocumentPage>(`/api/documents?${query.toString()}`)
}

/** 读取一篇已发布文档。 */
export function fetchDocument(id: number): Promise<DocumentDetail> {
  return apiRequest<DocumentDetail>(`/api/documents/${id}`)
}
