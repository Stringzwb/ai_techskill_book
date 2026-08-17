import { apiRequest } from './http'
import type { KnowledgeTagNode } from '../types'

/** 查询主平台可用的知识标签树。 */
export async function fetchKnowledgeTagTree(): Promise<KnowledgeTagNode[]> {
  return apiRequest<KnowledgeTagNode[]>('/api/knowledge-tags/tree')
}
