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
