<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpenText, FileText, LogIn, LogOut, Menu, Moon, Share2, Sun, TerminalSquare, X } from '@lucide/vue'
import { authStore } from '../stores/auth'
import { themeStore } from '../stores/theme'

const router = useRouter()
const menuOpen = ref(false)
const isDark = computed(() => themeStore.theme.value === 'dark')

/** 导航后关闭移动端菜单。 */
function closeMenu() {
  menuOpen.value = false
}

/** 注销当前用户并返回首页。 */
async function handleLogout() {
  await authStore.signOut()
  closeMenu()
  await router.push('/')
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <RouterLink class="brand" to="/" aria-label="技术岗AI知识库首页" @click="closeMenu">
        <span class="brand-mark"><TerminalSquare :size="22" /></span>
        <span>技术岗 <em>AI</em> 知识库</span>
      </RouterLink>

      <button class="mobile-menu-button" type="button" aria-label="切换导航菜单" @click="menuOpen = !menuOpen">
        <X v-if="menuOpen" :size="21" />
        <Menu v-else :size="21" />
      </button>

      <div class="header-navigation" :class="{ open: menuOpen }">
        <nav class="top-nav" aria-label="主导航">
          <RouterLink to="/" @click="closeMenu">首页</RouterLink>
          <RouterLink to="/library" @click="closeMenu"><Share2 :size="15" />分享库</RouterLink>
          <RouterLink to="/documents" @click="closeMenu"><FileText :size="15" />文档库</RouterLink>
          <RouterLink to="/tech-english" @click="closeMenu"><BookOpenText :size="15" />技术英语</RouterLink>
          <RouterLink to="/paths" @click="closeMenu">成长路径</RouterLink>
          <RouterLink to="/profile" @click="closeMenu">个人中心</RouterLink>
        </nav>

        <div class="header-actions">
          <button class="icon-button" type="button" :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'" @click="themeStore.toggleTheme">
            <Sun v-if="isDark" :size="19" />
            <Moon v-else :size="19" />
          </button>
          <RouterLink v-if="!authStore.state.user" class="header-login" to="/login" @click="closeMenu">
            <LogIn :size="17" /> 登录
          </RouterLink>
          <template v-else>
            <RouterLink class="header-user" to="/profile" @click="closeMenu">
              <span class="mini-avatar"><img :src="authStore.state.user.avatarUrl" alt="" /></span>
              {{ authStore.state.user.username }}
            </RouterLink>
            <button class="icon-button" type="button" aria-label="退出登录" @click="handleLogout">
              <LogOut :size="18" />
            </button>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>
