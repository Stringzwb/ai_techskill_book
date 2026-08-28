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

/** 技术英语语料类型。 */
export type TechEnglishCorpusType = 'VOCABULARY' | 'SENTENCE' | 'IMAGE' | 'ARTICLE'

/** 技术英语语料难度。 */
export type TechEnglishDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

/** 技术英语语料列表项。 */
export interface TechEnglishCorpusSummary {
  id: number
  corpusType: TechEnglishCorpusType
  title: string
  englishText: string | null
  phonetic: string | null
  explanation: string | null
  imageUrl: string | null
  imageAlt: string | null
  scenario: string | null
  difficulty: TechEnglishDifficulty
  tags: string | null
  translationText: string | null
  publishedAt: string | null
  knowledgeTags: DocumentTag[]
}

/** 技术英语语料分页响应。 */
export interface TechEnglishCorpusPage {
  total: number
  page: number
  size: number
  totalPages: number
  items: TechEnglishCorpusSummary[]
}

/** 技术英语语料详情。 */
export interface TechEnglishCorpusDetail extends TechEnglishCorpusSummary {
  articleMarkdown: string | null
  sourceName: string | null
  sourceUrl: string | null
  updatedAt: string
}

/** 主站轻量收录技术英语语料。 */
export interface TechEnglishCorpusCreatePayload {
  corpusType: TechEnglishCorpusType
  title: string
  englishText?: string
  phonetic?: string
  explanation?: string
  articleMarkdown?: string
  imageFile?: File | null
  imageAlt?: string
  sourceName?: string
  sourceUrl?: string
  scenario?: string
  difficulty?: TechEnglishDifficulty
  translationText?: string
  tagIds: number[]
}

export type CommunityPostType = 'QUESTION' | 'IMAGE' | 'LINK' | 'FILE' | 'VOTE'
export interface CommunityTag { id: number; name: string; level: 1 | 2 | 3 }
export interface CommunityAuthor { id: number; username: string; avatarUrl: string }
export interface CommunityAttachment { id: number; originalName: string; contentType: string; extension: string; sizeBytes: number; attachmentType: 'IMAGE' | 'FILE'; previewable: boolean }
export interface CommunityVoteOption { id: number; text: string; voteCount: number }
export interface CommunityVote { question: string; allowMultiple: boolean; anonymous: boolean; voteCount: number; voted: boolean; options: CommunityVoteOption[] }
export interface CommunityPost { id:number; postType:CommunityPostType; title:string; markdown:string|null; linkUrl:string|null; linkDomain:string|null; author:CommunityAuthor; tags:CommunityTag[]; attachments:CommunityAttachment[]; vote:CommunityVote|null; commentCount:number; publishedAt:string; canDelete:boolean }
export interface CommunityPostPage { total:number; page:number; size:number; totalPages:number; items:CommunityPost[] }
export interface CommunityComment { id:number; parentId:number|null; markdown:string; author:CommunityAuthor; createdAt:string; children:CommunityComment[] }
export interface CommunityAttachmentPreview { title: string; content: string; truncated: boolean }
