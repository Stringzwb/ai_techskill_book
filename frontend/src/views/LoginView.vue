<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowRight, LockKeyhole } from '@lucide/vue'
import { ApiError } from '../services/http'
import { authStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const account = ref('')
const password = ref('')
const errorMessage = ref('')
const submitting = ref(false)

/** 提交密码登录并跳转到原目标页面。 */
async function submitLogin() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await authStore.signIn({ account: account.value, password: password.value })
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : '/profile'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-page content-width">
    <div class="auth-intro">
      <span class="eyebrow"><LockKeyhole :size="15" /> 用户登录</span>
      <h1>继续你的技术成长计划</h1>
      <p>使用用户名、手机号或邮箱登录。登录状态会安全保存在当前浏览器会话中。</p>
    </div>
    <form class="auth-card" @submit.prevent="submitLogin">
      <div class="form-heading"><h2>欢迎回来</h2><p>请输入你的账号和密码</p></div>
      <label>账号<input v-model.trim="account" name="account" autocomplete="username" maxlength="128" placeholder="用户名 / 手机号 / 邮箱" required /></label>
      <label>密码<input v-model="password" name="password" type="password" autocomplete="current-password" maxlength="64" placeholder="请输入密码" required /></label>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button form-submit" type="submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }} <ArrowRight v-if="!submitting" :size="17" />
      </button>
      <p class="form-switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </form>
  </section>
</template>
