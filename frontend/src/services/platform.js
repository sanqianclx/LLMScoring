import { computed, reactive } from 'vue'
import { pushToast } from '../composables/useToast'

const teacherIdStorageKey = 'llm-scoring-teacher-id'

const defaultTeacher = () => ({
  id: '',
  name: '',
  username: '',
  school: '',
  taughtCourse: '',
  email: '',
  taughtCourses: [],
  role: '教师',
  lastLogin: ''
})

const state = reactive({
  initialized: false,
  bootstrap: null,
  teacher: defaultTeacher(),
  courses: [],
  papers: [],
  submissions: [],
  metrics: {
    courseCount: 0,
    paperCount: 0,
    submissionCount: 0,
    reviewedCount: 0,
    pendingCount: 0
  },
  activities: [],
  guideItems: [
    '请先创建课程，再创建试卷。',
    '学生通过分享码进入对应的试卷。',
    '系统先自动评分，教师再进行复核并发布最终成绩。',
    '学生可使用分享码与学号再次查询最终成绩。'
  ],
  studentPaper: null,
  studentResult: null
})

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    let message = '请求失败'
    try {
      const body = await response.json()
      message = body.message || body.error || message
    } catch {
      const text = await response.text()
      message = text || message
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  return text ? JSON.parse(text) : null
}

function toLabelStatus(status) {
  return status === 'REVIEWED' ? '已发布' : '待复核'
}

function questionTypeLabel(type) {
  return type === 'FILL_BLANK' ? '填空题' : '简答题'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function normalizeTeacher(raw = {}) {
  return {
    ...raw,
    email: raw.username || '',
    taughtCourses: raw.taughtCourse ? raw.taughtCourse.split(',').map((item) => item.trim()).filter(Boolean) : [],
    role: '教师',
    lastLogin: formatDateTime(new Date().toISOString())
  }
}

function normalizePaper(raw, submissionCount = 0) {
  return {
    ...raw,
    status: raw.active ? '启用' : '停用',
    submittedCount: submissionCount,
    questions: (raw.questions || []).map((question) => ({
      ...question,
      stem: question.text,
      typeLabel: questionTypeLabel(question.type),
      points: (question.scoringPoints || []).map((point) => ({
        ...point,
        label: point.keyword,
        keyword: point.keyword
      }))
    }))
  }
}

function normalizeSubmission(raw, paperMap) {
  const paper = paperMap.get(raw.paperId)
  const answerMap = new Map((raw.answers || []).map((answer) => [answer.questionId, answer.answerText]))
  const autoMap = new Map((raw.autoScores || []).map((score) => [score.questionId, score]))
  const finalMap = new Map((raw.finalScores || []).map((score) => [score.questionId, score]))

  const answers = (paper?.questions || []).map((question) => {
    const autoScore = autoMap.get(question.id)
    const finalScore = finalMap.get(question.id) || autoScore
    return {
      questionId: question.id,
      stem: question.text,
      type: questionTypeLabel(question.type),
      typeLabel: questionTypeLabel(question.type),
      answerText: answerMap.get(question.id) || '',
      autoScore: autoScore?.score ?? 0,
      finalScore: finalScore?.score ?? 0,
      maxScore: finalScore?.maxScore ?? question.maxScore,
      comment: finalScore?.comment || '',
      rationale: finalScore?.rationale || '',
      matchedPoints: finalScore?.matchedPoints || [],
      missingPoints: finalScore?.missingPoints || [],
      overridden: finalScore?.overridden || false
    }
  })

  return {
    ...raw,
    statusLabel: toLabelStatus(raw.status),
    submittedAtLabel: formatDateTime(raw.submittedAt),
    reviewedAtLabel: formatDateTime(raw.reviewedAt),
    paperTitle: paper?.title || '未命名试卷',
    answers,
    overallFeedback: raw.overallFeedback || ''
  }
}

function buildActivities(papers, submissions) {
  const recentSubmissions = submissions.slice(0, 3).map((submission) => ({
    id: `submission-${submission.id}`,
    title: `${submission.studentName}提交了试卷`,
    time: submission.submittedAtLabel,
    kind: '提交记录'
  }))

  const recentPapers = papers.slice(0, 3).map((paper) => ({
    id: `paper-${paper.id}`,
    title: `试卷已更新：${paper.title}`,
    time: formatDateTime(paper.updatedAt),
    kind: '试卷'
  }))

  return [...recentSubmissions, ...recentPapers]
}

function applyDashboard(rawDashboard) {
  const paperSubmissionCount = new Map()
  for (const submission of rawDashboard.submissions || []) {
    paperSubmissionCount.set(submission.paperId, (paperSubmissionCount.get(submission.paperId) || 0) + 1)
  }

  state.teacher = normalizeTeacher(rawDashboard.teacher)
  state.courses = rawDashboard.courses || []
  state.papers = (rawDashboard.papers || []).map((paper) => normalizePaper(paper, paperSubmissionCount.get(paper.id) || 0))

  const paperMap = new Map(state.papers.map((paper) => [paper.id, paper]))
  state.submissions = (rawDashboard.submissions || []).map((submission) => normalizeSubmission(submission, paperMap))
  state.metrics = {
    courseCount: Number(rawDashboard.stats?.courseCount || 0),
    paperCount: Number(rawDashboard.stats?.paperCount || 0),
    submissionCount: Number(rawDashboard.stats?.submissionCount || 0),
    reviewedCount: Number(rawDashboard.stats?.reviewedCount || 0),
    pendingCount: Number(rawDashboard.stats?.pendingCount || 0)
  }
  state.activities = buildActivities(state.papers, state.submissions)
}

async function initializeApp() {
  if (state.initialized) {
    return
  }

  try {
    state.bootstrap = await api('/api/bootstrap')
    const savedTeacherId = localStorage.getItem(teacherIdStorageKey)
    if (savedTeacherId) {
      await refreshDashboard(savedTeacherId, false)
    }
  } catch (error) {
    pushToast(error.message, 'error')
  } finally {
    state.initialized = true
  }
}

async function refreshDashboard(teacherId = state.teacher.id, showToast = false) {
  if (!teacherId) {
    return null
  }
  const dashboard = await api(`/api/teachers/${teacherId}/dashboard`)
  applyDashboard(dashboard)
  localStorage.setItem(teacherIdStorageKey, teacherId)
  if (showToast) {
    pushToast('仪表盘已刷新', 'success')
  }
  return dashboard
}

async function login(form) {
  const dashboard = await api('/api/teachers/login', {
    method: 'POST',
    body: JSON.stringify({
      username: form.account,
      password: form.password
    })
  })
  applyDashboard(dashboard)
  localStorage.setItem(teacherIdStorageKey, dashboard.teacher.id)
  pushToast('登录成功', 'success')
  return true
}

async function registerTeacher(form) {
  await api('/api/teachers/register', {
    method: 'POST',
    body: JSON.stringify({
      name: form.name,
      username: form.email,
      password: form.password,
      school: form.school,
      taughtCourse: (form.courses || []).join(', ') || '通用课程'
    })
  })
  pushToast('注册成功', 'success')
  return true
}

async function updateTeacherProfile(form) {
  const teacherId = state.teacher.id
  await api(`/api/teachers/${teacherId}`, {
    method: 'PUT',
    body: JSON.stringify({
      name: form.name,
      school: form.school,
      taughtCourse: (form.taughtCourses || []).join(', ')
    })
  })
  await refreshDashboard(teacherId)
  pushToast('个人信息已更新', 'success')
}

async function createCourse(course) {
  await api(`/api/teachers/${state.teacher.id}/courses`, {
    method: 'POST',
    body: JSON.stringify({
      name: course.name,
      description: course.description
    })
  })
  await refreshDashboard(state.teacher.id)
  pushToast(`课程已创建：${course.name}`, 'success')
}

async function deleteCourse(courseId) {
  await api(`/api/teachers/${state.teacher.id}/courses/${courseId}`, {
    method: 'DELETE'
  })
  await refreshDashboard(state.teacher.id)
  pushToast('课程已删除', 'success')
}

function toPaperPayload(draft) {
  return {
    courseId: draft.courseId,
    title: draft.title,
    description: draft.description,
    active: draft.active ?? draft.status !== '停用',
    questions: (draft.questions || []).map((question) => ({
      type: question.type,
      text: question.stem,
      referenceAnswer: question.referenceAnswer || question.standardAnswer || '',
      maxScore: Number(question.maxScore || (question.points || []).reduce((sum, point) => sum + Number(point.score || 0), 0)),
      scoringPoints: (question.points || []).map((point) => ({
        keyword: point.keyword || point.label,
        score: Number(point.score || 0),
        description: point.description || point.label || point.keyword
      }))
    }))
  }
}

function validatePaperPayload(payload) {
  if (!payload.title?.trim()) {
    throw new Error('试卷标题不能为空')
  }
  if (!payload.courseId) {
    throw new Error('请先选择课程')
  }
  if (!payload.questions?.length) {
    throw new Error('至少需要添加一道题目')
  }

  payload.questions.forEach((question, index) => {
    if (!question.text?.trim()) {
      throw new Error(`第 ${index + 1} 题题干不能为空`)
    }
    if (Number(question.maxScore) <= 0) {
      throw new Error(`第 ${index + 1} 题分值必须大于 0`)
    }
  })
}

async function savePaper(draft) {
  const teacherId = state.teacher.id
  const payload = toPaperPayload(draft)
  validatePaperPayload(payload)

  const saved = draft.id
    ? await api(`/api/teachers/${teacherId}/papers/${draft.id}`, { method: 'PUT', body: JSON.stringify(payload) })
    : await api(`/api/teachers/${teacherId}/papers`, { method: 'POST', body: JSON.stringify(payload) })

  await refreshDashboard(teacherId)
  pushToast('试卷已保存', 'success')
  return state.papers.find((paper) => paper.id === saved.id) || normalizePaper(saved)
}

async function deletePaper(paperId) {
  await api(`/api/teachers/${state.teacher.id}/papers/${paperId}`, {
    method: 'DELETE'
  })
  await refreshDashboard(state.teacher.id)
  pushToast('试卷已删除', 'success')
}

async function loadStudentPaper(shareCode) {
  const raw = await api(`/api/student/papers/${encodeURIComponent(shareCode)}`)
  state.studentPaper = {
    ...raw,
    questions: (raw.questions || []).map((question) => ({
      ...question,
      stem: question.text,
      typeLabel: questionTypeLabel(question.type)
    }))
  }
  return state.studentPaper
}

async function submitStudentPaper(payload) {
  const response = await api(`/api/student/papers/${encodeURIComponent(payload.shareCode)}/submissions`, {
    method: 'POST',
    body: JSON.stringify({
      studentId: payload.studentId,
      studentName: payload.studentName,
      answers: payload.answers.map((answer) => ({
        questionId: answer.questionId,
        answerText: answer.answerText
      }))
    })
  })
  pushToast('提交已保存，等待教师复核。', 'success')
  return response
}

async function loadStudentResult(shareCode, studentId) {
  const raw = await api(`/api/student/results/${encodeURIComponent(shareCode)}?studentId=${encodeURIComponent(studentId)}`)
  const questionMap = new Map((raw.paper?.questions || []).map((question) => [question.id, question]))
  const isReviewed = raw.status === 'REVIEWED'
  state.studentResult = {
    ...raw,
    totalScore: isReviewed ? raw.totalScore : '',
    overallFeedback: isReviewed ? raw.overallFeedback : '',
    statusLabel: toLabelStatus(raw.status),
    isReviewed,
    isPending: !isReviewed,
    paper: {
      ...raw.paper,
      questions: (raw.paper?.questions || []).map((question) => ({
        ...question,
        stem: question.text,
        typeLabel: questionTypeLabel(question.type)
      }))
    },
    scores: (raw.scores || []).map((score) => ({
      ...score,
      stem: questionMap.get(score.questionId)?.text || score.questionId,
      typeLabel: questionTypeLabel(questionMap.get(score.questionId)?.type)
    }))
  }
  return state.studentResult
}

async function reviewSubmission(payload) {
  const response = await api(`/api/teachers/${state.teacher.id}/submissions/${payload.id}/review`, {
    method: 'PUT',
    body: JSON.stringify({
      questionReviews: payload.answers.map((answer) => ({
        questionId: answer.questionId,
        score: Number(answer.finalScore),
        comment: answer.comment,
        rationale: answer.rationale
      })),
      overallFeedback: payload.overallFeedback || ''
    })
  })
  await refreshDashboard(state.teacher.id)
  pushToast('评阅已发布', 'success')
  return response
}

function clearAuth() {
  localStorage.removeItem(teacherIdStorageKey)
  state.teacher = defaultTeacher()
  state.courses = []
  state.papers = []
  state.submissions = []
  state.studentPaper = null
  state.studentResult = null
}

export function usePlatform() {
  const dashboardCards = computed(() => [
    { label: '课程', value: state.metrics.courseCount, foot: '当前教师课程', tone: 'primary' },
    { label: '试卷', value: state.metrics.paperCount, foot: '已创建的试卷', tone: 'info' },
    { label: '待评阅', value: state.metrics.pendingCount, foot: '等待复核', tone: 'warning' },
    { label: '已评阅', value: state.metrics.reviewedCount, foot: '已发布的结果', tone: 'success' }
  ])

  const pendingSubmissions = computed(() => state.submissions.filter((item) => item.status === 'PENDING_REVIEW'))

  function findCourse(courseId) {
    return state.courses.find((course) => course.id === courseId)
  }

  function findPaper(paperId) {
    return state.papers.find((paper) => paper.id === paperId)
  }

  function findSubmission(submissionId) {
    return state.submissions.find((submission) => submission.id === submissionId)
  }

  return {
    state,
    dashboardCards,
    pendingSubmissions,
    initializeApp,
    refreshDashboard,
    login,
    registerTeacher,
    updateTeacherProfile,
    createCourse,
    deleteCourse,
    savePaper,
    deletePaper,
    loadStudentPaper,
    submitStudentPaper,
    loadStudentResult,
    reviewSubmission,
    clearAuth,
    findCourse,
    findPaper,
    findSubmission,
    questionTypeLabel,
    formatDateTime,
    toLabelStatus
  }
}