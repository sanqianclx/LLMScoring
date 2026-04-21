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
          <p class="muted">登录后，你可以管理课程、试卷、评阅以及成绩发布。</p>
        </div>
        <span class="badge">已准备演示账号</span>
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
          <label class="muted"><input v-model="form.remember" type="checkbox" /> 记住本浏览器</label>
          <RouterLink class="text-btn" to="/forgot-password">忘记密码？</RouterLink>
        </div>
        <button class="primary-btn" @click="submit">登录</button>
      </div>
    </div>

    <div class="two-col section-grid">
      <article class="list-card">
        <strong>试卷流程</strong>
        <p class="muted">教师创建试卷并把分享码发给学生。</p>
      </article>
      <article class="list-card">
        <strong>评阅流程</strong>
        <p class="muted">系统先自动评分，随后教师复核并发布最终成绩。</p>
      </article>
    </div>

    <div class="row-between muted">
      <span>还没有教师账号？</span>
      <RouterLink class="secondary-btn" to="/register">去注册</RouterLink>
    </div>
  </section>
</template>