import type { HomeResponse } from '../types'

const fallbackHome: HomeResponse = {
  productName: '技术岗AI知识库',
  headline: '把复杂技术，变成可执行的成长路径',
  articleCount: 126,
  learningPathCount: 4,
  categoryCount: 6,
  categories: [
    { name: 'Java 后端', code: 'JAVA', description: 'Spring Boot、JVM 与高并发服务', articleCount: 28 },
    { name: '前端工程', code: 'WEB', description: 'Vue、TypeScript 与工程化体系', articleCount: 24 },
    { name: 'AI 应用', code: 'AI', description: '大模型、RAG 与智能体工程', articleCount: 21 },
    { name: '数据工程', code: 'DATA', description: 'MySQL、缓存与实时数据链路', articleCount: 19 },
    { name: '云原生', code: 'CLOUD', description: 'Linux、容器与可观测性', articleCount: 18 },
    { name: '架构进阶', code: 'ARCH', description: '系统设计、稳定性与技术决策', articleCount: 16 },
  ],
  featuredArticles: [
    { id: 1, title: 'Spring Boot 生产级配置清单', summary: '从线程池、连接池到健康检查，建立一套可复用的后端上线基线。', category: 'Java 后端', difficulty: '进阶', readMinutes: 18, tags: ['Spring Boot', 'JVM'] },
    { id: 2, title: 'Vue 3 组合式 API 的工程化边界', summary: '掌握组件拆分、状态管理和请求层设计，让前端项目持续可维护。', category: '前端工程', difficulty: '中级', readMinutes: 14, tags: ['Vue 3', 'TypeScript'] },
    { id: 3, title: '从零搭建企业级 RAG 知识库', summary: '理解文档切分、向量检索、重排与评测，构建可靠的知识增强应用。', category: 'AI 应用', difficulty: '进阶', readMinutes: 24, tags: ['RAG', '大模型'] },
  ],
}

export async function fetchHome(): Promise<{ data: HomeResponse; online: boolean }> {
  try {
    const response = await fetch('/api/home', { headers: { Accept: 'application/json' } })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    return { data: await response.json() as HomeResponse, online: true }
  } catch {
    return { data: fallbackHome, online: false }
  }
}
