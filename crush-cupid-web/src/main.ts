import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'
import i18n from './locales'
import { startSessionHeartbeat } from './api/http'

// 登录态心跳：周期探活，token 过期自动登出（覆盖停留页面 / 仅 SSE 常驻连接的失效场景）
startSessionHeartbeat(60000)

const app = createApp(App)
app.use(i18n)
app.use(Antd)
app.use(router)
app.mount('#app')
