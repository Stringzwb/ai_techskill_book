<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ArrowRight, UserPlus } from '@lucide/vue'
import { ApiError } from '../services/http'
import { authStore } from '../stores/auth'

const router = useRouter()
const form = reactive({ username: '', phone: '', email: '', password: '', confirmPassword: '' })
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})
const submitting = ref(false)

/** 校验并提交注册信息。 */
async function submitRegister() {
  errorMessage.value = ''
  fieldErrors.value = {}
  if (form.password !== form.confirmPassword) {
    fieldErrors.value.confirmPassword = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  try {
    await authStore.signUp({
      username: form.username,
      phone: form.phone,
      email: form.email,
      password: form.password,
    })
    await router.replace('/profile')
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = error.message
      fieldErrors.value = error.fieldErrors
    } else {
      errorMessage.value = '注册失败，请稍后重试'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-page content-width">
    <div class="auth-intro">
      <span class="eyebrow"><UserPlus :size="15" /> 创建账号</span>
      <h1>建立属于你的学习档案</h1>
      <p>注册后即可访问知识库与成长路径。新账号默认为游客会员，后续可升级会员等级。</p>
    </div>
    <form class="auth-card" @submit.prevent="submitRegister">
      <div class="form-heading"><h2>注册账号</h2><p>手机号和邮箱不可与其他账号重复</p></div>
      <label>用户名<input v-model.trim="form.username" autocomplete="username" minlength="3" maxlength="32" placeholder="3-32位文字、字母或数字" required /><small v-if="fieldErrors.username">{{ fieldErrors.username }}</small></label>
      <div class="form-row">
        <label>手机号<input v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="11位手机号" required /><small v-if="fieldErrors.phone">{{ fieldErrors.phone }}</small></label>
        <label>邮箱<input v-model.trim="form.email" type="email" autocomplete="email" maxlength="128" placeholder="name@example.com" required /><small v-if="fieldErrors.email">{{ fieldErrors.email }}</small></label>
      </div>
      <div class="form-row">
        <label>密码<input v-model="form.password" type="password" autocomplete="new-password" minlength="8" maxlength="64" placeholder="至少8位" required /><small v-if="fieldErrors.password">{{ fieldErrors.password }}</small></label>
        <label>确认密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="64" placeholder="再次输入密码" required /><small v-if="fieldErrors.confirmPassword">{{ fieldErrors.confirmPassword }}</small></label>
      </div>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button form-submit" type="submit" :disabled="submitting">
        {{ submitting ? '注册中…' : '注册并登录' }} <ArrowRight v-if="!submitting" :size="17" />
      </button>
      <p class="form-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
    </form>
  </section>
</template>
