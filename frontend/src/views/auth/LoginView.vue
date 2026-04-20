<script setup>
import { reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const router = useRouter()
const { login } = usePlatform()

const form = reactive({
  account: 'teacher',
  password: 'teacher123',
  remember: true
})

async function submit() {
  try {
    await login(form)
    router.push('/teacher/dashboard')
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <div class="form-card">
      <header class="card-header">
        <div>
          <h3 class="section-title">教师登录</h3>
          <p class="muted">登录后可管理课程、试卷、评分审核与个人信息。</p>
        </div>
        <span class="badge">已预填本地演示账号</span>
      </header>

      <div class="section-grid">
        <label class="label">
          <span>账号</span>
          <input v-model="form.account" class="input" placeholder="teacher" />
        </label>
        <label class="label">
          <span>密码</span>
          <input v-model="form.password" class="input" type="password" placeholder="请输入密码" />
        </label>
        <div class="row-between">
          <label class="muted"><input v-model="form.remember" type="checkbox" /> 记住当前浏览器登录状态</label>
          <RouterLink class="text-btn" to="/forgot-password">忘记密码？</RouterLink>
        </div>
        <button class="primary-btn" @click="submit">登录</button>
      </div>
    </div>

    <div class="two-col section-grid">
      <article class="list-card">
        <strong>教师工作台</strong>
        <p class="muted">集中查看课程数量、试卷情况、审核队列与最近动态。</p>
      </article>
      <article class="list-card">
        <strong>学生入口</strong>
        <p class="muted">学生通过分享码进入答题页面，审核完成后查看最终成绩与评语。</p>
      </article>
    </div>

    <div class="row-between muted">
      <span>还没有本地测试账号？</span>
      <RouterLink class="secondary-btn" to="/register">创建教师账号</RouterLink>
    </div>
  </section>
</template>