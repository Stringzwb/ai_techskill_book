import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from './stores/auth'
import HomeView from './views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { guestOnly: true } },
    { path: '/register', name: 'register', component: () => import('./views/RegisterView.vue'), meta: { guestOnly: true } },
    { path: '/library', name: 'library', component: () => import('./views/LibraryView.vue'), meta: { requiresAuth: true } },
    { path: '/documents', name: 'documents', component: () => import('./views/DocumentLibraryView.vue'), meta: { requiresAuth: true } },
    { path: '/paths', name: 'paths', component: () => import('./views/PathsView.vue'), meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: () => import('./views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

/** 路由切换前恢复会话，并保护需要登录的页面。 */
router.beforeEach(async (to) => {
  try {
    await authStore.initialize()
  } catch {
    // 会话服务异常时按未登录处理，页面请求会展示具体错误。
  }
  if (to.meta.requiresAuth && !authStore.state.user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authStore.state.user) {
    return { name: 'profile' }
  }
  return true
})

export default router
