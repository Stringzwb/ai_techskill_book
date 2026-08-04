import { computed, ref } from 'vue'

export type Theme = 'light' | 'dark'

/** 读取本机主题偏好。 */
function getInitialTheme(): Theme {
  const saved = localStorage.getItem('theme')
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const theme = ref<Theme>(getInitialTheme())

/** 将主题同步到页面根元素。 */
function applyTheme(value: Theme) {
  document.documentElement.dataset.theme = value
  document.documentElement.style.colorScheme = value
}

/** 在亮色和暗色模式之间切换。 */
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
  applyTheme(theme.value)
}

applyTheme(theme.value)

export const themeStore = {
  theme: computed(() => theme.value),
  toggleTheme,
}
