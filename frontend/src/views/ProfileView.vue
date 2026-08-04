<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { BadgeCheck, CalendarDays, CheckCircle2, Mail, Phone, Save, ShieldCheck, UserRound } from '@lucide/vue'
import { ApiError } from '../services/http'
import { updateProfile } from '../services/user'
import { authStore } from '../stores/auth'

const form = reactive({ username: '', phone: '', email: '' })
const fieldErrors = ref<Record<string, string>>({})
const statusMessage = ref('')
const errorMessage = ref('')
const saving = ref(false)
const user = computed(() => authStore.state.user)

/** 将服务端个人资料同步到编辑表单。 */
watch(user, (value) => {
  if (!value) return
  form.username = value.username
  form.phone = value.phone ?? ''
  form.email = value.email ?? ''
}, { immediate: true })

/** 使用本地语言格式化日期。 */
function formatDate(value: string | null | undefined) {
  if (!value) return '未设置'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

/** 保存允许修改的个人资料。 */
async function saveProfile() {
  statusMessage.value = ''
  errorMessage.value = ''
  fieldErrors.value = {}
  saving.value = true
  try {
    const updated = await updateProfile(form)
    authStore.setUser(updated)
    statusMessage.value = '个人资料已保存'
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = error.message
      fieldErrors.value = error.fieldErrors
    } else {
      errorMessage.value = '保存失败，请稍后重试'
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section v-if="user" class="profile-page content-width">
    <div class="page-heading">
      <span>PERSONAL PROFILE</span>
      <h1>个人信息</h1>
      <p>查看会员状态，并维护用于账号识别和联系的基础信息。</p>
    </div>

    <div class="profile-layout">
      <aside class="profile-summary-card">
        <div class="profile-avatar"><UserRound :size="42" /></div>
        <h2>{{ user.username }}</h2>
        <span class="membership-badge"><BadgeCheck :size="16" /> {{ user.memberLevelLabel }}</span>
        <p>当前使用系统默认头像，头像修改入口将在后续版本开放。</p>
        <dl>
          <div><dt><CalendarDays :size="16" /> 会员到期</dt><dd>{{ formatDate(user.memberExpireTime) }}</dd></div>
          <div><dt><ShieldCheck :size="16" /> 登录方式</dt><dd>{{ user.authProvider === 'PASSWORD' ? '密码登录' : '微信登录' }}</dd></div>
          <div><dt><CheckCircle2 :size="16" /> 最近登录</dt><dd>{{ formatDate(user.lastLoginTime) }}</dd></div>
        </dl>
      </aside>

      <form class="profile-form-card" @submit.prevent="saveProfile">
        <div class="form-heading"><h2>基础资料</h2><p>会员等级、到期时间和头像暂不支持自行修改</p></div>
        <label>用户名<div class="input-with-icon"><UserRound :size="18" /><input v-model.trim="form.username" minlength="3" maxlength="32" required /></div><small v-if="fieldErrors.username">{{ fieldErrors.username }}</small></label>
        <label>手机号<div class="input-with-icon"><Phone :size="18" /><input v-model.trim="form.phone" inputmode="numeric" maxlength="11" required /></div><small v-if="fieldErrors.phone">{{ fieldErrors.phone }}</small></label>
        <label>邮箱<div class="input-with-icon"><Mail :size="18" /><input v-model.trim="form.email" type="email" maxlength="128" required /></div><small v-if="fieldErrors.email">{{ fieldErrors.email }}</small></label>
        <div class="form-status" aria-live="polite">
          <span v-if="statusMessage" class="success-message"><CheckCircle2 :size="16" /> {{ statusMessage }}</span>
          <span v-if="errorMessage" class="form-error">{{ errorMessage }}</span>
        </div>
        <button class="primary-button form-submit" type="submit" :disabled="saving">
          <Save :size="17" /> {{ saving ? '保存中…' : '保存修改' }}
        </button>
      </form>
    </div>
  </section>
</template>
