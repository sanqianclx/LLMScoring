<script setup>
import { reactive } from 'vue'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const { state, createCourse, deleteCourse } = usePlatform()
const form = reactive({
  name: '',
  description: ''
})

async function submitCourse() {
  if (!form.name.trim()) {
    pushToast('课程名称不能为空', 'error')
    return
  }
  try {
    await createCourse({ ...form })
    form.name = ''
    form.description = ''
  } catch (error) {
    pushToast(error.message, 'error')
  }
}

async function removeCourse(courseId) {
  try {
    await deleteCourse(courseId)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <SectionCard title="课程管理" subtitle="先创建课程，再在课程下维护试卷与学生提交。">
      <template #actions>
        <button class="primary-btn" @click="submitCourse">创建课程</button>
      </template>

      <div class="form-card" style="margin-bottom: 20px; background: var(--surface-soft);">
        <div class="two-col section-grid">
          <label class="label">
            <span>课程名称</span>
            <input v-model="form.name" class="input" placeholder="例如：高中生物必修一" />
          </label>
          <label class="label">
            <span>课程描述</span>
            <textarea v-model="form.description" class="textarea" placeholder="填写课程范围、班级信息和测评重点"></textarea>
          </label>
        </div>
      </div>

      <div class="three-col section-grid">
        <article v-for="course in state.courses" :key="course.id" class="list-card">
          <div class="row-between">
            <strong>{{ course.name }}</strong>
            <span class="status-pill info">已创建</span>
          </div>
          <p class="muted">{{ course.description || '暂无课程描述。' }}</p>
          <p class="muted">创建时间：{{ course.createdAt }}</p>
          <div class="inline-actions" style="margin-top: 18px;">
            <button class="text-btn">查看详情</button>
            <button class="text-btn" @click="removeCourse(course.id)">删除</button>
          </div>
        </article>
      </div>
    </SectionCard>
  </section>
</template>