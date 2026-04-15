<script setup>
import { computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state, loadStudentPaper, submitStudentPaper } = usePlatform()

const form = reactive({
  studentId: '',
  studentName: '',
  answers: {}
})

const paper = computed(() => state.studentPaper)
const storageKey = computed(() => paper.value ? `draft:${paper.value.shareCode}` : `draft:${route.params.shareCode}`)

onMounted(async () => {
  try {
    const currentPaper = await loadStudentPaper(route.params.shareCode)
    form.studentId = localStorage.getItem(`${storageKey.value}:studentId`) || ''
    form.studentName = localStorage.getItem(`${storageKey.value}:studentName`) || ''
    form.answers = Object.fromEntries((currentPaper.questions || []).map((question) => [question.id, localStorage.getItem(`${storageKey.value}:${question.id}`) || '']))
  } catch (error) {
    pushToast(error.message, 'error')
  }
})

watch(() => form.studentId, (value) => localStorage.setItem(`${storageKey.value}:studentId`, value))
watch(() => form.studentName, (value) => localStorage.setItem(`${storageKey.value}:studentName`, value))
watch(() => form.answers, (value) => {
  Object.entries(value || {}).forEach(([questionId, answer]) => {
    localStorage.setItem(`${storageKey.value}:${questionId}`, answer)
  })
}, { deep: true })

const questionProgress = computed(() => {
  const total = paper.value?.questions?.length || 0
  const completed = (paper.value?.questions || []).filter((question) => (form.answers[question.id] || '').trim()).length
  return { total, completed }
})

async function submitPaper() {
  if (!form.studentId.trim()) {
    pushToast('请先填写学号', 'error')
    return
  }

  const ok = window.confirm('确认现在提交试卷吗？')
  if (!ok) return

  try {
    await submitStudentPaper({
      shareCode: paper.value.shareCode,
      studentId: form.studentId,
      studentName: form.studentName,
      answers: paper.value.questions.map((question) => ({
        questionId: question.id,
        answerText: form.answers[question.id] || ''
      }))
    })
    router.push(`/student/result/${paper.value.shareCode}?studentId=${encodeURIComponent(form.studentId)}`)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid" v-if="paper">
    <section class="panel" style="padding: 26px; border-radius: 28px;">
      <p class="kicker">答题入口</p>
      <div class="row-between">
        <div>
          <h2 class="section-title">{{ paper.title }}</h2>
          <p class="muted">{{ paper.courseName }} / 分享码 {{ paper.shareCode }}</p>
        </div>
        <span class="badge">已完成 {{ questionProgress.completed }}/{{ questionProgress.total }}</span>
      </div>
    </section>

    <section class="two-col section-grid">
      <div class="form-card">
        <h3 class="section-title">学生信息</h3>
        <p class="muted">提交前必须填写学号，答题草稿会自动保存在当前浏览器中。</p>
        <div class="section-grid" style="margin-top: 16px;">
          <label class="label"><span>学号</span><input v-model="form.studentId" class="input" placeholder="必填" /></label>
          <label class="label"><span>姓名</span><input v-model="form.studentName" class="input" placeholder="选填" /></label>
        </div>
      </div>
      <div class="form-card">
        <h3 class="section-title">答题提示</h3>
        <ul class="muted" style="margin: 0; padding-left: 18px; line-height: 1.9;">
          <li>填空题尽量使用准确术语作答。</li>
          <li>简答题建议分点陈述，便于系统识别关键得分点。</li>
          <li>输入内容会自动保存为本地草稿。</li>
        </ul>
      </div>
    </section>

    <article v-for="(question, index) in paper.questions" :key="question.id" class="question-card">
      <div class="row-between">
        <div>
          <p class="kicker">第 {{ index + 1 }} 题</p>
          <h3 class="section-title" style="font-size: 1.12rem;">{{ question.stem }}</h3>
        </div>
        <span class="status-pill info">{{ question.typeLabel }}</span>
      </div>
      <label class="label" style="margin-top: 16px;">
        <span>你的答案</span>
        <textarea v-model="form.answers[question.id]" class="textarea" :rows="question.type === 'FILL_BLANK' ? 3 : 6" :placeholder="question.type === 'FILL_BLANK' ? '请输入简短答案' : '请按要点完整作答'" />
      </label>
    </article>

    <div class="toolbar">
      <button class="primary-btn" @click="submitPaper">提交试卷</button>
    </div>
  </section>
</template>