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
      <SectionCard title="教师信息" subtitle="维护教师资料和任教课程标签，并与后端保持同步。">
        <div class="section-grid">
          <label class="label"><span>姓名</span><input v-model="form.name" class="input" /></label>
          <label class="label"><span>邮箱 / 账号</span><input v-model="form.email" class="input" disabled /></label>
          <label class="label"><span>所属学校</span><input v-model="form.school" class="input" /></label>
          <label class="label"><span>任教课程</span><TagInput v-model="form.taughtCourses" /></label>
          <div class="inline-actions"><button class="primary-btn" @click="saveProfile">保存资料</button></div>
        </div>
      </SectionCard>

      <SectionCard title="使用提示" subtitle="以下提醒面向当前真实教学流程。">
        <div class="data-grid">
          <article v-for="item in state.guideItems" :key="item" class="list-card">
            <strong>系统提示</strong>
            <p class="muted">{{ item }}</p>
          </article>
        </div>
      </SectionCard>
    </div>

    <SectionCard title="操作记录" subtitle="当前后端仍为内存存储，因此这里展示的是轻量级活动摘要。">
      <div class="data-grid">
        <article v-for="activity in state.activities" :key="activity.id" class="list-card">
          <div class="row-between">
            <strong>{{ activity.title }}</strong>
            <span class="badge">{{ activity.time }}</span>
          </div>
        </article>
      </div>
    </SectionCard>
  </section>
</template>