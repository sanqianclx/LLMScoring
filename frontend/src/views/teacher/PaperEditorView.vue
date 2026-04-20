<script setup>
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state, findPaper, savePaper, findCourse } = usePlatform()

const editingPaper = computed(() => findPaper(route.params.paperId))
const draft = reactive(editingPaper.value ? structuredClone(editingPaper.value) : {
  id: '',
  title: '新建试卷',
  courseId: state.courses[0]?.id || '',
  description: '请填写试卷说明与答题要求。',
  active: true,
  questions: [
    {
      id: 'question-draft-1',
      type: 'FILL_BLANK',
      stem: '',
      referenceAnswer: '',
      maxScore: 10,
      points: [{ id: 'point-1', label: '关键词', keyword: '关键词', description: '核心得分点', score: 10 }]
    }
  ]
})

const totalScore = computed(() => draft.questions.reduce((sum, question) => sum + Number(question.maxScore || 0), 0))

function addQuestion(type = 'SHORT_ANSWER') {
  draft.questions.push({
    id: `question-${Date.now()}`,
    type,
    stem: '',
    referenceAnswer: '',
    maxScore: 10,
    points: [{ id: `point-${Date.now()}`, label: '', keyword: '', description: '', score: 2 }]
  })
}

function removeQuestion(questionId) {
  draft.questions = draft.questions.filter((question) => question.id !== questionId)
}

function addPoint(question) {
  question.points.push({ id: `point-${Math.random().toString(36).slice(2, 7)}`, label: '', keyword: '', description: '', score: 2 })
}

function removePoint(question, pointId) {
  question.points = question.points.filter((point) => point.id !== pointId)
}

async function persistPaper() {
  try {
    const saved = await savePaper(draft)
    router.push(`/teacher/share/${saved.id}`)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <div class="row-between panel" style="padding: 24px 26px; border-radius: 28px;">
      <div>
        <p class="kicker">试卷编辑</p>
        <h2 class="section-title">试卷创建与配置</h2>
        <p class="muted">左侧维护试卷信息，右侧配置题目、参考答案与评分点。</p>
      </div>
      <div class="toolbar">
        <span class="badge">总分 {{ totalScore }} 分</span>
        <button class="ghost-btn" @click="addQuestion('FILL_BLANK')">添加填空题</button>
        <button class="ghost-btn" @click="addQuestion('SHORT_ANSWER')">添加简答题</button>
      </div>
    </div>

    <div class="split-view">
      <SectionCard title="试卷设置" subtitle="在这里维护所属课程、描述和发布状态。">
        <div class="section-grid">
          <label class="label"><span>试卷标题</span><input v-model="draft.title" class="input" /></label>
          <label class="label"><span>所属课程</span>
            <select v-model="draft.courseId" class="select">
              <option v-for="course in state.courses" :key="course.id" :value="course.id">{{ course.name }}</option>
            </select>
          </label>
          <label class="label"><span>试卷说明</span><textarea v-model="draft.description" class="textarea" /></label>
          <label class="label"><span>学生访问状态</span>
            <select v-model="draft.active" class="select">
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>
          </label>
        </div>

        <div class="list-card" style="margin-top: 18px;">
          <strong>当前摘要</strong>
          <p class="muted">所属课程：{{ findCourse(draft.courseId)?.name || '未选择' }}</p>
          <p class="muted">题目数量：{{ draft.questions.length }} / 试卷总分：{{ totalScore }}</p>
        </div>

        <div class="toolbar" style="margin-top: 18px;">
          <button class="primary-btn" @click="persistPaper">保存试卷</button>
          <button class="ghost-btn">预览试卷</button>
        </div>
      </SectionCard>

      <SectionCard title="题目编辑器" subtitle="每道题可维护题型、参考答案和评分点。">
        <div class="section-grid">
          <article v-for="(question, index) in draft.questions" :key="question.id" class="question-card">
            <div class="row-between">
              <div>
                <p class="kicker">第 {{ index + 1 }} 题</p>
                <h3 class="section-title" style="font-size: 1.1rem;">{{ question.type === 'FILL_BLANK' ? '填空题' : '简答题' }}</h3>
              </div>
              <div class="toolbar">
                <select v-model="question.type" class="select" style="width: 160px;">
                  <option value="FILL_BLANK">填空题</option>
                  <option value="SHORT_ANSWER">简答题</option>
                </select>
                <button class="text-btn" @click="removeQuestion(question.id)">删除题目</button>
              </div>
            </div>

            <div class="section-grid" style="margin-top: 16px;">
              <label class="label"><span>题目内容</span><textarea v-model="question.stem" class="textarea" /></label>
              <div class="two-col section-grid">
                <label class="label"><span>参考答案</span><textarea v-model="question.referenceAnswer" class="textarea" /></label>
                <label class="label"><span>题目分值</span><input v-model="question.maxScore" class="input" type="number" min="1" step="1" /></label>
              </div>

              <div class="section-grid">
                <div class="row-between">
                  <strong>评分点</strong>
                  <button class="ghost-btn" @click="addPoint(question)">添加评分点</button>
                </div>
                <div v-for="point in question.points" :key="point.id" class="list-card" style="padding: 16px;">
                  <div class="two-col section-grid">
                    <label class="label"><span>关键词</span><input v-model="point.keyword" class="input" placeholder="填写关键词或同义表达" /></label>
                    <label class="label"><span>分值</span><input v-model="point.score" class="input" type="number" min="1" step="1" /></label>
                  </div>
                  <label class="label" style="margin-top: 12px;"><span>说明</span><input v-model="point.description" class="input" placeholder="说明该评分点的判分依据" /></label>
                  <div class="inline-actions" style="margin-top: 12px;">
                    <button class="text-btn" @click="removePoint(question, point.id)">删除评分点</button>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>
      </SectionCard>
    </div>
  </section>
</template>