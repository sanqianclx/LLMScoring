import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { layout: 'auth', title: '教师登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: { layout: 'auth', title: '教师注册' }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('../views/auth/ForgotPasswordView.vue'),
    meta: { layout: 'auth', title: '重置密码' }
  },
  {
    path: '/teacher/dashboard',
    name: 'teacher-dashboard',
    component: () => import('../views/teacher/DashboardView.vue'),
    meta: { layout: 'teacher', title: '仪表盘', section: 'dashboard' }
  },
  {
    path: '/teacher/courses',
    name: 'teacher-courses',
    component: () => import('../views/teacher/CoursesView.vue'),
    meta: { layout: 'teacher', title: '课程管理', section: 'courses' }
  },
  {
    path: '/teacher/papers',
    name: 'paper-list',
    component: () => import('../views/teacher/PaperEditorView.vue'),
    meta: { layout: 'teacher', title: '试卷管理', section: 'papers' }
  },
  {
    path: '/teacher/papers/new',
    name: 'paper-create',
    component: () => import('../views/teacher/PaperEditorView.vue'),
    meta: { layout: 'teacher', title: '新建试卷', section: 'papers' }
  },
  {
    path: '/teacher/papers/:paperId/edit',
    name: 'paper-edit',
    component: () => import('../views/teacher/PaperEditorView.vue'),
    meta: { layout: 'teacher', title: '编辑试卷', section: 'papers' }
  },
  {
    path: '/teacher/share/:paperId',
    name: 'paper-share',
    component: () => import('../views/teacher/ShareView.vue'),
    meta: { layout: 'teacher', title: '分享试卷', section: 'papers' }
  },
  {
    path: '/teacher/review',
    name: 'teacher-review',
    component: () => import('../views/teacher/ReviewView.vue'),
    meta: { layout: 'teacher', title: '成绩复核', section: 'review' }
  },
  {
    path: '/teacher/profile',
    name: 'teacher-profile',
    component: () => import('../views/teacher/ProfileView.vue'),
    meta: { layout: 'teacher', title: '个人信息', section: 'profile' }
  },
  {
    path: '/student',
    name: 'student-entry',
    component: () => import('../views/student/StudentEntryView.vue'),
    meta: { layout: 'student', title: '学生入口' }
  },
  {
    path: '/student/exam/:shareCode',
    name: 'student-exam',
    component: () => import('../views/student/ExamView.vue'),
    meta: { layout: 'student', title: '开始答题' }
  },
  {
    path: '/student/result/:shareCode',
    name: 'student-result',
    component: () => import('../views/student/ResultView.vue'),
    meta: { layout: 'student', title: '查询成绩' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: { layout: 'auth', title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'LLM 智能评分'} - LLM 智能评分`
})

export default router