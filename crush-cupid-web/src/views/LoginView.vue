<template>
  <div class="letter-page">
    <!-- 静谧夜空：极少量静态星点（不做无限闪烁，保持克制） -->
    <div class="letter-stars" aria-hidden="true">
      <span v-for="i in 14" :key="i" :style="starStyle(i)" />
    </div>

    <!-- 信笺卡片：一封信在灯下摊开 -->
    <main class="letter-card" :class="{ 'letter-enter': entered }">
      <header class="letter-head">
        <p class="letter-kicker">CUPID · 暗恋模拟器</p>
        <h1 class="letter-title">把没说出口的话，<br />写给今晚的月光。</h1>
        <div class="letter-rule" />
      </header>

      <div class="letter-body">
        <div class="letter-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="activeKey === 'login'"
            :class="{ active: activeKey === 'login' }"
            @click="activeKey = 'login'"
          >
            {{ t('login.tabLogin') }}
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="activeKey === 'register'"
            :class="{ active: activeKey === 'register' }"
            @click="activeKey = 'register'"
          >
            {{ t('login.tabRegister') }}
          </button>
        </div>

        <!-- 登录：邮箱 + 密码 + 可刷新图形验证码 -->
        <form v-show="activeKey === 'login'" class="letter-form" @submit.prevent="handleLogin">
          <label class="field">
            <span class="field-label">{{ t('login.email') }}</span>
            <input
              v-model.trim="loginForm.email"
              type="email"
              autocomplete="email"
              :placeholder="t('login.emailPlaceholder')"
            />
          </label>

          <label class="field">
            <span class="field-label">{{ t('login.password') }}</span>
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              :placeholder="t('login.passwordPlaceholder')"
            />
          </label>

          <div class="field">
            <span class="field-label">{{ t('login.captcha') }}</span>
            <div class="captcha-row">
              <input
                v-model.trim="loginForm.captcha"
                class="captcha-input"
                maxlength="4"
                autocomplete="off"
                :placeholder="t('login.captchaPlaceholder')"
                @keydown.enter.prevent="handleLogin"
              />
              <button
                type="button"
                class="captcha-img-btn"
                :disabled="!captchaImage"
                :title="t('login.captchaRefresh')"
                @click="refreshCaptcha"
              >
                <span v-if="captchaImage" class="captcha-img-wrap" :class="{ spinning: spinning }">
                  <img :src="captchaImage" alt="captcha" />
                </span>
                <span v-else class="captcha-fallback">{{ t('login.captchaRefresh') }}</span>
              </button>
            </div>
            <span class="captcha-hint">{{ t('login.captchaRefresh') }}</span>
          </div>

          <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

          <button type="submit" class="seal-btn" :disabled="loading">
            {{ loading ? '……' : t('login.loginBtn') }}
          </button>
        </form>

        <!-- 注册：邮箱验证码仍然保留 -->
        <form v-show="activeKey === 'register'" class="letter-form" @submit.prevent="handleRegister">
          <label class="field">
            <span class="field-label">{{ t('login.username') }}</span>
            <input
              v-model.trim="registerForm.username"
              autocomplete="nickname"
              :placeholder="t('login.usernamePlaceholder')"
            />
          </label>

          <label class="field">
            <span class="field-label">{{ t('login.email') }}</span>
            <div class="code-send-row">
              <input
                v-model.trim="registerForm.email"
                type="email"
                autocomplete="email"
                class="code-send-input"
                :placeholder="t('login.emailPlaceholder')"
              />
              <button
                type="button"
                class="code-send-btn"
                :disabled="!registerForm.email || sendingRegCode || regCountdown > 0"
                @click="sendRegisterCode"
              >
                {{ regCountdown > 0 ? `${regCountdown}s` : (sendingRegCode ? t('login.sending') : t('login.sendCode')) }}
              </button>
            </div>
          </label>

          <label class="field">
            <span class="field-label">{{ t('login.code') }}</span>
            <input
              v-model.trim="registerForm.code"
              class="captcha-input"
              maxlength="8"
              autocomplete="off"
              :placeholder="t('login.codePlaceholder')"
            />
          </label>

          <label class="field">
            <span class="field-label">{{ t('login.password') }}</span>
            <input
              v-model="registerForm.password"
              type="password"
              autocomplete="new-password"
              :placeholder="t('login.passwordPlaceholder')"
            />
          </label>

          <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

          <button type="submit" class="seal-btn" :disabled="loading">
            {{ loading ? '……' : t('login.registerBtn') }}
          </button>
        </form>
      </div>

      <!-- 火漆印章：唯一被记住的装饰 -->
      <div class="wax-seal" aria-hidden="true" :style="{ opacity: entered ? 1 : 0 }"></div>
    </main>

    <footer class="letter-foot">
      <p>by cupid · 深夜写下的每个字，都算数</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { login, register, sendEmailCode, getCaptcha } from '@/api'
import type { CaptchaVO } from '@/types'

const { t } = useI18n()
const router = useRouter()

const activeKey = ref('login')
const loading = ref(false)
const errorMsg = ref('')
const entered = ref(false)

const captchaId = ref('')
const captchaImage = ref('')
const spinning = ref(false)

const sendingRegCode = ref(false)
const regCountdown = ref(0)
let regCountdownTimer: ReturnType<typeof setInterval> | null = null

const loginForm = reactive({ email: '', password: '', captchaId: '', captcha: '' })
const registerForm = reactive({ username: '', email: '', password: '', code: '' })

function starStyle(i: number): Record<string, string> {
  return {
    left: `${(i * 61) % 100}%`,
    top: `${(i * 37) % 100}%`,
    width: `${1 + (i % 3)}px`,
    height: `${1 + (i % 3)}px`,
    opacity: String(0.2 + (i % 4) * 0.1),
  }
}

async function refreshCaptcha() {
  const minIdle = 300
  if (spinning.value) return
  spinning.value = true
  try {
    const vo: CaptchaVO = await getCaptcha()
    captchaId.value = vo.captchaId
    captchaImage.value = vo.image
    loginForm.captchaId = vo.captchaId
  } catch (e: any) {
    message.error(e?.message || t('login.sendFailed'))
  } finally {
    await new Promise((r) => setTimeout(r, minIdle))
    spinning.value = false
  }
}

async function sendRegisterCode() {
  if (!registerForm.email) {
    message.warning(t('login.needEmail'))
    return
  }
  sendingRegCode.value = true
  try {
    await sendEmailCode(registerForm.email, 'REGISTER')
    message.success(t('login.codeSent'))
    regCountdown.value = 60
    if (regCountdownTimer) clearInterval(regCountdownTimer)
    regCountdownTimer = setInterval(() => {
      regCountdown.value--
      if (regCountdown.value <= 0 && regCountdownTimer) {
        clearInterval(regCountdownTimer)
        regCountdownTimer = null
        sendingRegCode.value = false
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || t('login.sendFailed'))
  } finally {
    sendingRegCode.value = false
  }
}

async function handleLogin() {
  if (!loginForm.email || !loginForm.password) {
    message.warning(t('login.needEmailPassword'))
    return
  }
  if (!loginForm.captcha) {
    message.warning(t('login.needCaptcha'))
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const vo = await login({
      email: loginForm.email,
      password: loginForm.password,
      captchaId: captchaId.value,
      captcha: loginForm.captcha,
    })
    localStorage.setItem('satoken', vo.tokenValue)
    message.success(t('login.loginSuccess'))
    router.push('/chat')
  } catch (e: any) {
    errorMsg.value = e?.message || t('login.loginFailed')
    loginForm.captcha = ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.email || !registerForm.password) {
    message.warning(t('login.needEmailPassword'))
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
    message.success(t('login.registerSuccess'))
    router.push('/chat')
  } catch (e: any) {
    errorMsg.value = e?.message || t('login.registerFailed')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (localStorage.getItem('satoken')) {
    router.push('/chat')
    return
  }
  await refreshCaptcha()
  requestAnimationFrame(() => {
    entered.value = true
  })
})

onUnmounted(() => {
  if (regCountdownTimer) {
    clearInterval(regCountdownTimer)
    regCountdownTimer = null
  }
})
</script>

<style scoped>
.letter-page {
  --night: #120e26;
  --paper: #f8f0de;
  --paper-edge: #efe1c4;
  --ink: #3a2f44;
  --ink-muted: #93869c;
  --wax: #b93445;
  --line: #e4d6b8;
  --focus: #7a5068;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px 56px;
  overflow: hidden;
  background:
    radial-gradient(1200px 640px at 50% 118%, rgba(251, 209, 141, 0.16), transparent 60%),
    radial-gradient(900px 520px at 50% -12%, rgba(126, 87, 166, 0.22), transparent 55%),
    var(--night);
}

/* 星空：静态星点，不做无限闪烁 */
.letter-stars {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.letter-stars span {
  position: absolute;
  border-radius: 50%;
  background: #fff;
}

/* 信笺卡片 */
.letter-card {
  position: relative;
  z-index: 1;
  width: 432px;
  max-width: 100%;
  background: var(--paper);
  border: 1px solid var(--paper-edge);
  border-radius: 10px;
  box-shadow:
    0 34px 80px -28px rgba(4, 2, 16, 0.85),
    0 12px 32px -18px rgba(4, 2, 16, 0.7),
    inset 0 1px 0 rgba(255, 255, 255, 0.55);
  transform: translateY(26px);
  opacity: 0;
}
.letter-enter {
  animation: letterIn 0.8s cubic-bezier(0.22, 0.9, 0.32, 1) forwards;
}
@keyframes letterIn {
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.letter-head {
  padding: 40px 40px 0;
  text-align: left;
}
.letter-kicker {
  margin: 0 0 14px;
  font-size: 11px;
  letter-spacing: 0.28em;
  color: var(--ink-muted);
}
.letter-title {
  margin: 0;
  font-family: Georgia, 'Songti SC', 'Noto Serif SC', 'STSong', serif;
  font-weight: 600;
  font-size: 27px;
  line-height: 1.6;
  letter-spacing: 0.02em;
  color: var(--ink);
}
.letter-rule {
  margin: 26px 0 0;
  height: 1px;
  background: linear-gradient(90deg, var(--wax), var(--paper-edge) 56%, transparent);
}

.letter-body {
  padding: 26px 40px 8px;
}

/* 页内 tab：像信纸上的起行，克制不上划线 */
.letter-tabs {
  display: flex;
  gap: 28px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line);
}
.letter-tabs button {
  appearance: none;
  border: 0;
  background: none;
  padding: 0 0 10px;
  font-size: 14px;
  color: var(--ink-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.2s ease, border-color 0.2s ease;
}
.letter-tabs button.active {
  color: var(--ink);
  border-bottom-color: var(--wax);
  font-weight: 600;
}

/* 表单 */
.letter-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 0;
}
.field-label {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
}
.field input {
  height: 46px;
  padding: 0 14px;
  font-size: 14px;
  color: var(--ink);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--line);
  border-radius: 7px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.field input::placeholder {
  color: var(--ink-muted);
  opacity: 0.75;
}
.field input:focus {
  border-color: var(--focus);
  box-shadow: 0 0 0 3px rgba(122, 80, 104, 0.14);
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 10px;
}
.captcha-input {
  flex: 1;
  min-width: 0;
}
.captcha-img-btn {
  flex: 0 0 112px;
  height: 46px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 7px;
  overflow: hidden;
  background: #fffdf6;
  cursor: pointer;
  transition: border-color 0.2s ease;
}
.captcha-img-btn:hover {
  border-color: var(--focus);
}
.captcha-img-btn:disabled {
  cursor: default;
}
.captcha-img-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.captcha-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.captcha-img-wrap.spinning img {
  animation: spinIn 0.45s ease;
}
@keyframes spinIn {
  from {
    transform: rotate(14deg) scale(0.92);
    opacity: 0.5;
  }
  to {
    transform: none;
    opacity: 1;
  }
}
.captcha-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 6px;
  font-size: 11px;
  line-height: 1.4;
  color: var(--ink-muted);
  text-align: center;
}
.captcha-hint {
  font-size: 11px;
  color: var(--ink-muted);
  opacity: 0.8;
}

/* 注册：发送验证码 */
.code-send-row {
  display: flex;
  gap: 10px;
}
.code-send-input {
  flex: 1;
  min-width: 0;
}
.code-send-btn {
  flex: 0 0 auto;
  height: 46px;
  padding: 0 16px;
  font-size: 13px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: var(--wax);
  background: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.code-send-btn:hover:not(:disabled) {
  border-color: var(--wax);
}
.code-send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 火漆按钮 */
.seal-btn {
  margin-top: 4px;
  height: 48px;
  width: 100%;
  font-size: 15px;
  letter-spacing: 0.22em;
  color: #fff7ec;
  background: var(--wax);
  border: 0;
  border-radius: 24px;
  box-shadow: 0 10px 24px -12px rgba(185, 52, 69, 0.75);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease, background 0.2s ease;
}
.seal-btn:hover:not(:disabled) {
  background: #c94457;
  box-shadow: 0 14px 30px -12px rgba(185, 52, 69, 0.85);
  transform: translateY(-1px);
}
.seal-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.99);
}
.seal-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-error {
  margin: 0;
  font-size: 13px;
  color: var(--wax);
}

/* 火漆印章 */
.wax-seal {
  position: absolute;
  right: 38px;
  bottom: -17px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 34% 30%, #d85a68, transparent 42%),
    var(--wax);
  box-shadow: 0 6px 14px -6px rgba(185, 52, 69, 0.7);
  transition: opacity 0.7s ease 0.5s;
}
.wax-seal::after {
  content: '';
  position: absolute;
  inset: 9px;
  border-radius: 50%;
  border: 1.6px solid rgba(255, 247, 236, 0.85);
}

.letter-foot {
  position: absolute;
  z-index: 1;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
}
.letter-foot p {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.14em;
  color: rgba(236, 224, 255, 0.34);
}

/* 动效克制：尊重减少动态偏好 */
@media (prefers-reduced-motion: reduce) {
  .letter-enter {
    animation: none;
    transform: none;
    opacity: 1;
  }
  .captcha-img-wrap.spinning img {
    animation: none;
  }
}

/* 窄屏 */
@media (max-width: 480px) {
  .letter-head {
    padding: 30px 26px 0;
  }
  .letter-body {
    padding: 22px 26px 8px;
  }
  .wax-seal {
    right: 24px;
  }
}
</style>