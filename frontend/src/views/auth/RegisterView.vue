<script setup>
import { reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import TagInput from '../../components/TagInput.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const router = useRouter()
const { registerTeacher } = usePlatform()

const form = reactive({
  name: '',
  email: '',
  password: '',
  school: '',
  courses: ['高中生物']
})

async function submit() {
  try {
    await registerTeacher(form)
    router.push('/login')
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
          <h3 class="section-title">教师注册</h3>
          <p class="muted">创建教师账号后即可进入完整的课程、试卷与评分流程。</p>
        </div>
      </header>

      <div class="two-col section-grid">
        <label class="label"><span>姓名</span><input v-model="form.name" class="input" /></label>
        <label class="label"><span>邮箱 / 账号</span><input v-model="form.email" class="input" type="email" /></label>
        <label class="label"><span>密码</span><input v-model="form.password" class="input" type="password" /></label>
        <label class="label"><span>所属学校</span><input v-model="form.school" class="input" /></label>
      </div>

      <label class="label" style="margin-top: 18px;">
        <span>任教课程</span>
        <TagInput v-model="form.courses" placeholder="输入课程后按回车添加" />
      </label>

      <div class="row-between" style="margin-top: 20px;">
        <RouterLink class="text-btn" to="/login">返回登录</RouterLink>
        <button class="primary-btn" @click="submit">注册</button>
      </div>
    </div>
  </section>
</template>