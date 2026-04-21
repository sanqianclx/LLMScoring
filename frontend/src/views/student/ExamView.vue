<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state, loadStudentPaper, submitStudentPaper } = usePlatform()
const submitting = ref(false)

const form = reactive({
  studentId: '',
  studentName: '',
  answers: {}
})

const paper = computed(() => state.studentPaper)
const storageKey = computed(() => paper.value ? `draft:${paper.value.shareCode}` : `draft:${route.params.shareCode}`)

function clearDraft(currentPaper) {
  if (!currentPaper) {
    return
  }
  localStorage.removeItem(`${storageKey.value}:studentId`)
  localStorage.removeItem(`${storageKey.value}:studentName`)
  ;(currentPaper.questions || []).forEach((question) => {
    localStorage.removeItem(`${storageKey.value}:${question.id}`)
  })
}

async function loadPaper() {
  try {
    const currentPaper = await loadStudentPaper(route.params.shareCode)
    form.studentId = localStorage.getItem(`${storageKey.value}:studentId`) || ''
    form.studentName = localStorage.getItem(`${storageKey.value}:studentName`) || ''
    form.answers = Object.fromEntries((currentPaper.questions || []).map((question) => [question.id, localStorage.getItem(`${storageKey.value}:${question.id}`) || '']))
  } catch (error) {
    pushToast(error.message, 'error')
  }
}

onMounted(loadPaper)
watch(() => route.params.shareCode, loadPaper)

watch(() => form.studentId, (value) => localStorage.setItem(`${storageKey.value}:studentId`, value || ''))
watch(() => form.studentName, (value) => localStorage.setItem(`${storageKey.value}:studentName`, value || ''))
watch(() => form.answers, (value) => {
  Object.entries(value || {}).forEach(([questionId, answer]) => {
    localStorage.setItem(`${storageKey.value}:${questionId}`, answer || '')
  })
}, { deep: true })

const progress = computed(() => {
  const total = paper.value?.questions?.length || 0
  const completed = (paper.value?.questions || []).filter((question) => (form.answers[question.id] || '').trim()).length
  return { total, completed }
})

async function submitPaper() {
  if (!paper.value) {
    return
  }
  if (!form.studentId.trim()) {
    pushToast('提交前必须填写学号。', 'error')
    return
  }

  const confirmed = window.confirm('确定现在提交试卷吗？系统会先自动评分，然后进入教师复核队列。')
  if (!confirmed) {
    return
  }

  submitting.value = true
  try {
    await submitStudentPaper({
      shareCode: paper.value.shareCode,
      studentId: form.studentId.trim(),
      studentName: form.studentName.trim(),
      answers: (paper.value.questions || []).map((question) => ({
        questionId: question.id,
        answerText: form.answers[question.id] || ''
      }))
    })
    clearDraft(paper.value)
    router.push(`/student/result/${paper.value.shareCode}?studentId=${encodeURIComponent(form.studentId.trim())}`)
  } catch (error) {
    pushToast(error.message, 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section v-if="paper" class="section-grid">
    <section class="panel" style="padding: 26px; border-radius: 28px;">
      <p class="kicker">答题页面</p>
      <div class="row-between">
        <div>
          <h2 class="section-title">{{ paper.title }}</h2>
          <p class="muted">分享码：{{ paper.shareCode }}</p>
          <p class="muted">{{ paper.description || '请尽量清晰、完整地作答每道题。' }}</p>
        </div>
        <span class="badge">已完成 {{ progress.completed }}/{{ progress.total }}</span>
      </div>
    </section>

    <section class="two-col section-grid">
      <article class="form-card">
        <h3 class="section-title">学生信息</h3>
        <p class="muted">学号为必填项。答案会在本浏览器自动保存，直到你提交为止。</p>
        <div class="section-grid" style="margin-top: 16px;">
          <label class="label">
            <span>学号</span>
            <input v-model="form.studentId" class="input" placeholder="必填" />
          </label>
          <label class="label">
            <span>姓名</span>
            <input v-model="form.studentName" class="input" placeholder="选填" />
          </label>
        </div>
      </article>

      <article class="form-card">
        <h3 class="section-title">提交说明</h3>
        <ul class="muted tips-list">
          <li>提交后，页面会跳转到“等待复核/最终成绩”的结果页。</li>
          <li>系统会先自动评分，随后由教师复核并发布最终成绩。</li>
          <li>你可以稍后使用相同分享码与学号再次查询成绩与评语。</li>
        </ul>
      </article>
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
        <span>你的作答</span>
        <textarea
          v-model="form.answers[question.id]"
          class="textarea"
          :rows="question.type === 'FILL_BLANK' ? 3 : 6"
          :placeholder="question.type === 'FILL_BLANK' ? '请输入简短答案' : '请输入较完整的回答'"
        />
      </label>
    </article>

    <div class="toolbar">
      <button class="primary-btn" :disabled="submitting" @click="submitPaper">{{ submitting ? '提交中...' : '提交试卷' }}</button>
    </div>
  </section>

  <section v-else class="empty-state">
    试卷加载中，或分享码无效。
  </section>
</template>

<style scoped>
.tips-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
}
</style>