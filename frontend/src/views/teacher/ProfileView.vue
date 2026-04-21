<script setup>
import { reactive, watch } from 'vue'
import SectionCard from '../../components/SectionCard.vue'
import TagInput from '../../components/TagInput.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const { state, updateTeacherProfile } = usePlatform()
const form = reactive({
  name: '',
  email: '',
  school: '',
  taughtCourses: []
})

watch(() => state.teacher, (teacher) => {
  form.name = teacher.name || ''
  form.email = teacher.email || ''
  form.school = teacher.school || ''
  form.taughtCourses = [...(teacher.taughtCourses || [])]
}, { immediate: true, deep: true })

async function saveProfile() {
  try {
    await updateTeacherProfile(form)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <div class="two-col section-grid">
      <SectionCard title="教师信息" subtitle="维护教师基本信息与授课课程标签。">
        <div class="section-grid">
          <label class="label"><span>姓名</span><input v-model="form.name" class="input" /></label>
          <label class="label"><span>邮箱 / 用户名</span><input v-model="form.email" class="input" disabled /></label>
          <label class="label"><span>学校</span><input v-model="form.school" class="input" /></label>
          <label class="label"><span>授课课程</span><TagInput v-model="form.taughtCourses" /></label>
          <div class="inline-actions"><button class="primary-btn" @click="saveProfile">保存</button></div>
        </div>
      </SectionCard>

      <SectionCard title="使用提示" subtitle="端到端流程的简要提醒。">
        <div class="data-grid">
          <article v-for="item in state.guideItems" :key="item" class="list-card">
            <strong>提醒</strong>
            <p class="muted">{{ item }}</p>
          </article>
        </div>
      </SectionCard>
    </div>
  </section>
</template>