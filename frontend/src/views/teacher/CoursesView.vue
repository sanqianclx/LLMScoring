<script setup>
import { onMounted, reactive } from 'vue'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const { state, createCourse, deleteCourse, refreshDashboard, formatDateTime } = usePlatform()
const form = reactive({
  name: '',
  description: ''
})

onMounted(() => {
  if (state.teacher.id) {
    refreshDashboard(state.teacher.id).catch(() => {})
  }
})

async function submitCourse() {
  if (!form.name.trim()) {
    pushToast('课程名称不能为空。', 'error')
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
  const confirmed = window.confirm('确定删除该课程吗？请确认该课程下已没有试卷。')
  if (!confirmed) {
    return
  }
  try {
    await deleteCourse(courseId)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <SectionCard title="课程管理" subtitle="建议先创建课程，再在课程下创建试卷与管理学生提交。">
      <template #actions>
        <button class="primary-btn" @click="submitCourse">创建课程</button>
      </template>

      <div class="form-card" style="margin-bottom: 20px; background: var(--surface-soft);">
        <div class="two-col section-grid">
          <label class="label">
            <span>课程名称</span>
            <input v-model="form.name" class="input" placeholder="示例：生物 第二单元" />
          </label>
          <label class="label">
            <span>说明</span>
            <textarea v-model="form.description" class="textarea" placeholder="填写范围、班级/分组信息或评分备注"></textarea>
          </label>
        </div>
      </div>

      <div v-if="state.courses.length" class="three-col section-grid">
        <article v-for="course in state.courses" :key="course.id" class="list-card">
          <div class="row-between">
            <strong>{{ course.name }}</strong>
            <span class="status-pill info">课程</span>
          </div>
          <p class="muted">{{ course.description || '暂无说明。' }}</p>
          <p class="muted">创建时间：{{ formatDateTime(course.createdAt) }}</p>
          <div class="inline-actions" style="margin-top: 18px;">
            <button class="text-btn" @click="removeCourse(course.id)">删除</button>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">还没有课程。请先创建第一门课程，方便组织试卷。</div>
    </SectionCard>
  </section>
</template>