import axios from 'axios'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

const NETWORK_TEXT: Record<string, string> = {
  'Network Error': '网络连接失败，请检查网络后重试',
  'timeout of 60000ms exceeded': '请求超时，请稍后再试',
  'Request failed with status code 400': '请求参数有误',
  'Request failed with status code 401': '未登录或登录已过期',
  'Request failed with status code 403': '没有权限执行该操作',
  'Request failed with status code 404': '内容不存在或已被删除',
  'Request failed with status code 429': '操作过于频繁，请稍后再试',
  'Request failed with status code 500': '服务器开小差了，请稍后再试',
  'Request failed with status code 502': '服务器开小差了，请稍后再试',
  'Request failed with status code 503': '服务暂不可用，请稍后再试',
}

function friendlyNetworkMsg(raw: string | undefined): string {
  if (!raw) return '请求失败'
  if (NETWORK_TEXT[raw]) return NETWORK_TEXT[raw]
  const m = raw.match(/^Request failed with status code (\d+)$/)
  if (m) {
    return NETWORK_TEXT[`Request failed with status code ${m[1]}`] || '请求失败，请稍后再试'
  }
  return raw
}

/**
 * 登录失效统一处理：清除本地 token 并跳回登录页。
 * 抽成独立函数，供 axios 拦截器与 SSE(EventSource 拿不到状态码) 探活复用。
 */
export function handleUnauthorized(): void {
  localStorage.removeItem('satoken')
  if (!window.location.hash.includes('/login')) {
    window.location.hash = '#/login'
  }
}

/**
 * 登录态心跳：周期性探活后端 /auth/me，token 过期（401）时强制登出。
 * 用于覆盖「用户停留页面、只有 EventSource 常驻连接静默重连 401、
 * 没有新 axios 请求触发拦截器」的失效场景。静默请求，不弹错误提示。
 */
export function startSessionHeartbeat(intervalMs = 60000): () => void {
  const tick = () => {
    const token = localStorage.getItem('satoken')
    if (!token || window.location.hash.includes('/login')) return
    fetch('/api/auth/me', { headers: { Authorization: token } })
      .then((res) => {
        if (res.status === 401) handleUnauthorized()
      })
      .catch(() => { /* 网络抖动忽略，交由下一次心跳 */ })
  }
  tick()
  const timer = window.setInterval(tick, intervalMs)
  return () => window.clearInterval(timer)
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) {
    config.headers['Authorization'] = token
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    // 登录失效（401）：统一登出，避免卡在当前页反复报错
    if (err?.response?.status === 401) {
      handleUnauthorized()
    }
    const msg = err?.response?.data?.message || friendlyNetworkMsg(err?.message) || '请求失败'
    message.error(msg)
    return Promise.reject(err)
  },
)

export default http
