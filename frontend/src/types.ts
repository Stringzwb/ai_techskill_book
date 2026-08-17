export interface CategorySummary {
  name: string
  code: string
  description: string
  articleCount: number
}

export interface ArticleSummary {
  id: number
  title: string
  summary: string
  category: string
  difficulty: string
  readMinutes: number
  tags: string[]
}

export interface HomeResponse {
  productName: string
  headline: string
  articleCount: number
  learningPathCount: number
  categoryCount: number
  categories: CategorySummary[]
  featuredArticles: ArticleSummary[]
}

export interface UserProfile {
  id: number
  username: string
  phone: string | null
  email: string | null
  memberLevel: 'SUPER' | 'NORMAL' | 'GUEST' | 'BANNED'
  memberLevelLabel: string
  userRole: 'USER' | 'SUPER_ADMIN'
  userRoleLabel: string
  memberExpireTime: string | null
  avatarUrl: string
  authProvider: 'PASSWORD' | 'WECHAT'
  lastLoginTime: string | null
  createtime: string
}

export interface AuthResponse {
  user: UserProfile
}

export interface ApiErrorPayload {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

/** 可供文档库选择的知识标签树节点。 */
export interface KnowledgeTagNode {
  id: number
  name: string
  level: 1 | 2 | 3
  sortOrder: number
  description: string | null
  children: KnowledgeTagNode[]
}

/** 当前选中的三级知识标签路径。 */
export interface KnowledgeTagSelection {
  module: KnowledgeTagNode | null
  secondary: KnowledgeTagNode | null
  tertiary: KnowledgeTagNode | null
}

/** 文档关联标签摘要。 */
export interface DocumentTag {
  id: number
  name: string
  level: 1 | 2 | 3
}

/** 文档库列表项。 */
export interface DocumentSummary {
  id: number
  title: string
  summary: string
  readingMinutes: number
  publishedAt: string | null
  tags: DocumentTag[]
}

/** 文档库分页响应。 */
export interface DocumentPage {
  total: number
  page: number
  size: number
  totalPages: number
  items: DocumentSummary[]
}

/** 文档阅读详情。 */
export interface DocumentDetail extends DocumentSummary {
  markdown: string
  updatedAt: string
}
