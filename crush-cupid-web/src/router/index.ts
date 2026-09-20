import {createRouter, createWebHashHistory, createWebHistory} from 'vue-router'
import Layout from '@/layouts/Layout.vue'

const router = createRouter({
  // history: createWebHistory(),
  // 启用prod
  history:createWebHashHistory(),
  routes: [
    // 登录/注册页：独立于主布局，无 token 时的落脚点
    { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
    {
      path: '/',
      component: Layout,
      redirect: '/chat',
      children: [
        { path: 'chat', name: 'Chat', component: () => import('@/views/ChatView.vue'), meta: { title: '对话' } },
        { path: 'advisor', name: 'Advisor', component: () => import('@/views/AdvisorView.vue'), meta: { title: '军师' } },
        { path: 'crush', name: 'Crush', component: () => import('@/views/CrushListView.vue'), meta: { title: '暗恋对象' } },
        { path: 'skill', name: 'Skill', component: () => import('@/views/SkillCatalogView.vue'), meta: { title: 'Skill 目录' } },
        { path: 'ai-provider', name: 'AiProvider', component: () => import('@/views/AiProviderView.vue'), meta: { title: '大模型 API' } },
         { path: 'user', name: 'User', component: () => import('@/views/UserCenterView.vue'), meta: { title: '用户中心' } },
         { path: 'voice', name: 'Voice', component: () => import('@/views/VoiceConfigView.vue'), meta: { title: '音色配置' } },
         { path: 'report/:id', name: 'Report', component: () => import('@/views/ReportDetailView.vue'), meta: { title: '报告详情' } },
         { path: 'versions', name: 'Versions', component: () => import('@/views/VersionView.vue'), meta: { title: '版本历史' } },
      ],
    },
  ],
})

 router.beforeEach((to) => {
  const token = localStorage.getItem('satoken')
  if (!token) {
    return to.path === '/login' ? true : '/login'
  }
  return to.path === '/login' ? '/chat' : true
})

export default router