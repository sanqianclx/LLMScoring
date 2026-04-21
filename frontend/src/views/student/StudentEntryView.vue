<script setup>
import { reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state } = usePlatform()

const startForm = reactive({
  shareCode: route.query.shareCode || ''
})

const resultForm = reactive({
  shareCode: route.query.shareCode || '',
  studentId: route.query.studentId || ''
})

watch(() => state.bootstrap?.demoShareCode, (code) => {
  if (code && !startForm.shareCode) {
    startForm.shareCode = code
  }
  if (code && !resultForm.shareCode) {
    resultForm.shareCode = code
  }
}, { immediate: true })

function startExam() {
  if (!startForm.shareCode.trim()) {
    pushToast('请输入分享码。', 'error')
    return
  }
  router.push(`/student/exam/${encodeURIComponent(startForm.shareCode.trim())}`)
}

function lookupResult() {
  if (!resultForm.shareCode.trim() || !resultForm.studentId.trim()) {
    pushToast('请同时填写分享码与学号。', 'error')
    return
  }
  router.push(`/student/result/${encodeURIComponent(resultForm.shareCode.trim())}?studentId=${encodeURIComponent(resultForm.studentId.trim())}`)
}
</script>

<template>
  <section class="section-grid">
    <div class="two-col section-grid">
      <article class="form-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">开始答题</h3>
            <p class="muted">输入教师提供的分享码打开对应试卷。学号会在答题页填写。</p>
          </div>
        </header>
        <div class="section-grid">
          <label class="label">
            <span>分享码</span>
            <input v-model="startForm.shareCode" class="input" placeholder="请输入分享码" />
          </label>
          <button class="primary-btn" @click="startExam">打开试卷</button>
        </div>
      </article>

      <article class="form-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">查询成绩</h3>
            <p class="muted">教师完成复核后，可使用相同分享码与学号查看最终得分与评语。</p>
          </div>
        </header>
        <div class="section-grid">
          <label class="label">
            <span>分享码</span>
            <input v-model="resultForm.shareCode" class="input" placeholder="请输入分享码" />
          </label>
          <label class="label">
            <span>学号</span>
            <input v-model="resultForm.studentId" class="input" placeholder="请输入学号" />
          </label>
          <button class="secondary-btn" @click="lookupResult">查询</button>
        </div>
      </article>
    </div>

    <article class="list-card">
      <strong>流程说明</strong>
      <p class="muted">1. 输入分享码以打开对应试卷。</p>
      <p class="muted">2. 填写学号，完成作答并提交。</p>
      <p class="muted">3. 系统先自动评分，随后教师复核并发布最终成绩。</p>
      <p class="muted">4. 稍后返回此页，使用相同分享码与学号查看成绩与评语。</p>
    </article>
  </section>
</template>