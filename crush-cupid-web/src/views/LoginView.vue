
<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">💘</div>
        <div class="login-title">Cupid</div>
        <div class="login-sub">暗恋模拟器</div>
      </div>

      <a-tabs v-model:activeKey="activeKey" class="login-tabs">
        <a-tab-pane key="login" tab="登录">
          <a-form :model="loginForm" layout="vertical" @finish="handleLogin">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="loginForm.email" placeholder="请输入邮箱">
                <template #suffix>
                  <a-button size="small" type="primary" @click="sendCode" :disabled="!loginForm.email || sendingCode || countdown > 0" style="margin-left: 4px;">
                    {{ countdown > 0 ? `${countdown}s 后重发` : (sendingCode ? '发送中…' : '验证码') }}
                  </a-button>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item v-if="showCode" label="验证码" name="code">
              <a-input v-model:value="loginForm.code" placeholder="请输入验证码" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="loginForm.password" placeholder="请输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block :loading="loading">
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form :model="registerForm" layout="vertical" @finish="handleRegister">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="registerForm.username" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="registerForm.email" placeholder="请输入邮箱">
                <template #suffix>
                  <a-button size="small" type="primary" @click="sendRegisterCode" :disabled="!registerForm.email || sendingRegCode || regCountdown > 0" style="margin-left: 4px;">
                    {{ regCountdown > 0 ? `${regCountdown}s 后重发` : (sendingRegCode ? '发送中…' : '验证码') }}
                  </a-button>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item v-if="showRegCode" label="验证码" name="code">
              <a-input v-model:value="registerForm.code" placeholder="请输入验证码" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="registerForm.password" placeholder="请输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block :loading="loading">
                注册
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>

      <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login, register, sendEmailCode } from '@/api'

const router = useRouter()
const activeKey = ref('login')
const loading = ref(false)
const errorMsg = ref('')
const sendingCode = ref(false)
const countdown = ref(0)
const showCode = ref(false)
const sendingRegCode = ref(false)
const regCountdown = ref(0)
const showRegCode = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let regCountdownTimer: ReturnType<typeof setInterval> | null = null

const loginForm = reactive({ email: '', password: '', code: '' })
const registerForm = reactive({ username: '', email: '', password: '', code: '' })

async function sendCode() {
  if (!loginForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  sendingCode.value = true
  try {
    await sendEmailCode(loginForm.email, 'LOGIN')
    message.success('验证码已发送')
    showCode.value = true
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
        sendingCode.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || '发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function sendRegisterCode() {
  if (!registerForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  sendingRegCode.value = true
  try {
    await sendEmailCode(registerForm.email, 'REGISTER')
    message.success('验证码已发送')
    showRegCode.value = true
    regCountdown.value = 60
    regCountdownTimer = setInterval(() => {
      regCountdown.value--
      if (regCountdown.value <= 0) {
        clearInterval(regCountdownTimer)
        regCountdownTimer = null
        sendingRegCode.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || '发送失败')
  } finally {
    sendingRegCode.value = false
  }
}

onMounted(() => {
  if (localStorage.getItem('satoken')) {
    router.push('/chat')
  }
})

onUnmounted(() => {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  if (regCountdownTimer) { clearInterval(regCountdownTimer); regCountdownTimer = null }
})

async function handleLogin() {
  if (!loginForm.email || !loginForm.password) {
    message.warning('请填写邮箱和密码')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const vo = await login({ email: loginForm.email, password: loginForm.password, code: loginForm.code })
    localStorage.setItem('satoken', vo.tokenValue)
    message.success('登录成功')
    router.push('/chat')
  } catch (e: any) {
    errorMsg.value = e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.email || !registerForm.password) {
    message.warning('请填写邮箱和密码')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const vo = await register({
      email: registerForm.email,
      password: registerForm.password,
      username: registerForm.username,
      code: registerForm.code,
    })
    localStorage.setItem('satoken', vo.tokenValue)
    message.success('注册成功')
    router.push('/chat')
  } catch (e: any) {
    errorMsg.value = e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a0f1a 0%, #2a1f2e 50%, #1a0f1a 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: var(--cupid-radius-xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 28px 0 8px;
}

.login-logo {
  font-size: 40px;
  margin-bottom: 8px;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  background: var(--cupid-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-sub {
  font-size: 13px;
  color: var(--cupid-text-secondary);
  margin-top: 4px;
}

.login-tabs {
  padding: 0 24px;
}

.login-error {
  color: #ff4d4f;
  font-size: 13px;
  text-align: center;
  padding: 0 24px 16px;
}
</style>
