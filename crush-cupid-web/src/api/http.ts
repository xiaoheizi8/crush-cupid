import axios from 'axios'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

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
    const msg = err?.response?.data?.message || err?.message || 'ÇëÇóÊ§°Ü'
    message.error(msg)
    return Promise.reject(err)
  },
)

export default http
