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
export type TechEnglishCorpusType = 'VOCABULARY' | 'PHRASE' | 'PATTERN' | 'SENTENCE' | 'ARTICLE'

/** 技术英语语料难度。 */
export type TechEnglishDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

/** 固定场景标签。 */
export interface TechEnglishScenarioTag {
  code: string
  label: string
  description: string
}

/** 技术英语语料列表项。 */
export interface TechEnglishCorpusSummary {
  id: number
  corpusType: TechEnglishCorpusType
  title: string
  englishText: string | null
  phonetic: string | null
  partOfSpeech: string | null
  britishPhonetic: string | null
  americanPhonetic: string | null
  explanation: string | null
  imageUrl: string | null
  imageAlt: string | null
  scenario: string | null
  scenarioTags: TechEnglishScenarioTag[]
  difficulty: TechEnglishDifficulty
  tags: string | null
  translationText: string | null
  sentencePattern: string | null
  sentencePatternExplanation: string | null
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
  importBatchUuid: string | null
  articleMarkdown: string | null
  sourceName: string | null
  sourceUrl: string | null
  keyVocabulary: TechEnglishKeyVocabulary[]
  patternExamples: TechEnglishPatternExample[]
  updatedAt: string
  vocabularyExamples: TechEnglishVocabularyExample[]
}

/** 句子语料中的重点词汇。 */
export interface TechEnglishKeyVocabulary {
  word: string
  partOfSpeech: string | null
  meaning: string | null
}

/** 经典句式的扩展例句。 */
export interface TechEnglishPatternExample {
  englishText: string
  translationText: string | null
}

/** 技术英语词汇例句。 */
export interface TechEnglishVocabularyExample {
  id: number
  sentenceCorpusId: number | null
  englishText: string
  translationText: string | null
}

/** 主站轻量收录词汇例句。 */
export interface TechEnglishVocabularyExampleInput {
  englishText: string
  translationText: string
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
  scenarioTagCodes?: string[]
  difficulty?: TechEnglishDifficulty
  translationText?: string
  tagIds: number[]
  vocabularyExamples?: TechEnglishVocabularyExampleInput[]
  syncExamplesToSentences?: boolean
}

/** AI 截图识别模式，由模型自动判别语料类型。 */
export type TechEnglishAiImportType = 'AUTO'

/** AI 截图导入请求。 */
export interface TechEnglishAiImportPayload {
  sessionUuid: string
  chunkIndex: number
  chunkCount: number
  scenario: string
  exampleCount: number
  images: File[]
}

/** 等待用户确认的单条截图识别结果。 */
export interface TechEnglishAiRecognitionItem {
  itemKey: string
  sourceImageIndex: number
  corpusType: 'VOCABULARY' | 'PHRASE' | 'SENTENCE'
  englishText: string
  partOfSpeech: string | null
  translationText: string | null
  britishPhonetic: string | null
  americanPhonetic: string | null
  sentencePattern: string | null
  sentencePatternExplanation: string | null
  scenarioTags: TechEnglishScenarioTag[]
  keyVocabulary: TechEnglishKeyVocabulary[]
  examples: TechEnglishPatternExample[]
}

/** AI 截图识别草稿，选择标签确认后才正式入库。 */
export interface TechEnglishAiRecognitionResponse {
  sessionUuid: string
  batchUuid: string
  chunkIndex: number
  chunkCount: number
  importType: TechEnglishAiImportType
  sourceName: string
  imageCount: number
  itemCount: number
  expiresAt: string
  items: TechEnglishAiRecognitionItem[]
}

/** 单条识图结果的标签分配。 */
export interface TechEnglishAiItemTagAssignment {
  itemKey: string
  tagIds: number[]
}

/** AI 识别草稿确认入库请求。 */
export interface TechEnglishAiConfirmPayload {
  batchUuid: string
  itemTagAssignments: TechEnglishAiItemTagAssignment[]
  images?: File[]
}

/** AI 截图导入结果。 */
export interface TechEnglishAiImportResponse {
  batchUuid: string
  importType: TechEnglishAiImportType
  sourceName: string
  imageCount: number
  createdCount: number
  items: TechEnglishCorpusDetail[]
}

/** 识图历史分页。 */
export interface TechEnglishRecognitionHistoryPage {
  total: number
  page: number
  size: number
  totalPages: number
  items: TechEnglishRecognitionHistorySummary[]
}

/** 识图历史会话摘要。 */
export interface TechEnglishRecognitionHistorySummary {
  sessionUuid: string
  status: 'PROCESSING' | 'FAILED' | 'RECOGNIZED' | 'PARTIAL' | 'IMPORTED'
  sourceName: string | null
  scenario: string | null
  chunkCount: number
  completedChunkCount: number
  imageCount: number
  itemCount: number
  importedChunkCount: number
  createdAt: string
  updatedAt: string
}

/** 识图历史单个子任务。 */
export interface TechEnglishRecognitionHistoryTask {
  batchUuid: string
  status: 'PROCESSING' | 'FAILED' | 'RECOGNIZED' | 'IMPORTED'
  chunkIndex: number
  chunkCount: number
  imageCount: number
  itemCount: number
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
  completedAt: string | null
  importedAt: string | null
  items: TechEnglishAiRecognitionItem[]
}

/** 识图历史详情。 */
export interface TechEnglishRecognitionHistoryDetail {
  sessionUuid: string
  status: 'PROCESSING' | 'FAILED' | 'RECOGNIZED' | 'PARTIAL' | 'IMPORTED'
  sourceName: string | null
  scenario: string | null
  imageCount: number
  itemCount: number
  createdAt: string
  updatedAt: string
  tasks: TechEnglishRecognitionHistoryTask[]
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
