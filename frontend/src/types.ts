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
