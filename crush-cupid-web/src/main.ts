import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'
import i18n from './locales'

const app = createApp(App)
app.use(i18n)
app.use(Antd)
app.use(router)
app.mount('#app')
