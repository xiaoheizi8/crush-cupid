<template>
  <a-layout class="app-layout">
    <a-layout-sider :width="220" class="app-sider">
      <div class="logo">
        <div class="logo__icon">💘</div>
        <div class="logo__text">
          <div class="logo__name">Cupid</div>
          <div class="logo__sub">{{ t('nav.subtitle') }}</div>
        </div>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        class="app-menu"
        @click="onMenuClick"
      >
        <a-menu-item key="/chat">
          <span class="menu-icon">💬</span>
          <span>{{ t('nav.chat') }}</span>
        </a-menu-item>
        <a-menu-item key="/advisor">
          <span class="menu-icon">🧠</span>
          <span>{{ t('nav.advisor') }}</span>
        </a-menu-item>
        <a-menu-item key="/crush">
          <span class="menu-icon">💞</span>
          <span>{{ t('nav.crush') }}</span>
        </a-menu-item>
        <a-menu-item key="/skill">
          <span class="menu-icon">📚</span>
          <span>{{ t('nav.skill') }}</span>
        </a-menu-item>
        <a-menu-item key="/ai-provider">
          <span class="menu-icon">⚙️</span>
          <span>{{ t('nav.aiProvider') }}</span>
        </a-menu-item>
        <a-menu-item key="/user">
          <span class="menu-icon">👤</span>
          <span>{{ t('nav.user') }}</span>
        </a-menu-item>
        <a-menu-item key="/voice">
          <span class="menu-icon">🎙️</span>
          <span>{{ t('nav.voice') }}</span>
        </a-menu-item>
        <a-menu-item key="/versions">
          <span class="menu-icon">📜</span>
          <span>{{ t('nav.versions') }}</span>
        </a-menu-item>
      </a-menu>
      <div class="lang-switcher">
        <a-button type="link" size="small" @click="switchLang">{{ t('common.switchLang') }}</a-button>
      </div>
      <div v-if="user" class="sider-user">
        <div class="sider-user__name">{{ user.username }}</div>
        <div class="sider-user__email">{{ user.email }}</div>
        <a-button size="small" class="sider-user__btn" @click="handleLogout">{{ t('common.logout') }}</a-button>
      </div>
      <div v-else class="sider-footer">made with 💗</div>
    </a-layout-sider>

    <a-layout class="app-main">
      <a-layout-content class="app-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { me, logout } from '@/api'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const user = ref<{ username: string; email: string } | null>(null)
const selectedKeys = ref([route.path])

onMounted(async () => {
  try {
    const vo = await me()
    user.value = { username: vo.username, email: vo.email }
  } catch {
    // 未登录
  }
})

watch(route, () => {
  selectedKeys.value = [route.path]
})

function onMenuClick({ key }: { key: string }) {
  selectedKeys.value = [key]
  router.push(key)
}

function switchLang() {
  locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  localStorage.setItem('cupid-locale', locale.value)
}

async function handleLogout() {
  try {
    await logout()
  } catch { /* ignore */ }
  localStorage.removeItem('satoken')
  user.value = null
  message.success(t('common.logoutSuccess'))
  router.push('/chat')
}
</script>

<style>
.app-layout {
  min-height: 100vh;
}

.app-sider {
  background: linear-gradient(180deg, #140a2e 0%, #1e1248 40%, #2d1b4e 100%) !important;
  box-shadow: 4px 0 24px rgba(20, 10, 46, 0.2);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
.app-sider::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 15%, rgba(196, 78, 200, 0.1), transparent 60%),
              radial-gradient(circle at 70% 80%, rgba(255, 107, 157, 0.08), transparent 50%);
  pointer-events: none;
}
/* 侧边栏流光动画 */
.app-sider::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -100%;
  width: 200%;
  height: 200%;
  background: linear-gradient(115deg, transparent 30%, rgba(196, 78, 200, 0.04) 45%, rgba(255, 107, 157, 0.03) 50%, transparent 60%);
  pointer-events: none;
  animation: siderShimmer 12s linear infinite;
}
@keyframes siderShimmer {
  from { transform: translateX(0); }
  to { transform: translateX(50%); }
}

.app-sider .ant-layout-sider-children {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 16px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
  z-index: 1;
}

.logo__icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--cupid-radius-sm);
  background: var(--cupid-gradient);
  font-size: 22px;
  box-shadow: 0 4px 16px rgba(196, 78, 200, 0.5);
  flex-shrink: 0;
  transition: transform var(--cupid-transition-bounce), box-shadow var(--cupid-transition);
  cursor: pointer;
  animation: logoGlow 3s ease-in-out infinite;
}
@keyframes logoGlow {
  0%, 100% { box-shadow: 0 4px 16px rgba(196, 78, 200, 0.5); }
  50% { box-shadow: 0 4px 24px rgba(255, 107, 157, 0.65); }
}
.logo:hover .logo__icon {
  transform: scale(1.08) rotate(-5deg);
  box-shadow: 0 6px 28px rgba(196, 78, 200, 0.7);
}

.logo__text {
  min-width: 0;
}

.logo__name {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.logo__sub {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  margin-top: 2px;
}

.app-menu {
  flex: 1;
  padding: 14px 8px;
  background: transparent !important;
  border-right: none !important;
  position: relative;
  z-index: 1;
}

.app-menu .ant-menu-item {
  border-radius: var(--cupid-radius-sm);
  margin: 3px 0 !important;
  height: 42px;
  line-height: 42px;
  color: rgba(255, 255, 255, 0.65) !important;
  transition: all var(--cupid-transition);
}

.app-menu .ant-menu-item:hover {
  background: rgba(196, 78, 200, 0.12) !important;
  color: #fff !important;
  transform: translateX(2px);
}

.app-menu .ant-menu-item-selected {
  background: var(--cupid-gradient) !important;
  color: #fff !important;
  box-shadow: 0 4px 16px rgba(196, 78, 200, 0.45);
  font-weight: 500;
}

.menu-icon {
  display: inline-block;
  margin-right: 10px;
  font-size: 16px;
}

.lang-switcher {
  text-align: center;
  padding: 8px 16px;
}

.lang-switcher .ant-btn-link {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.lang-switcher .ant-btn-link:hover {
  color: #fff;
}

.sider-user {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}

.sider-user__name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.sider-user__email {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  margin-top: 2px;
}

.sider-user__btn {
  margin-top: 10px;
  background: rgba(255, 90, 122, 0.2);
  border-color: rgba(255, 90, 122, 0.4);
  color: #fff;
  border-radius: var(--cupid-radius-sm);
  height: 36px;
  transition: all var(--cupid-transition);
}

.sider-user__btn:hover {
  background: rgba(255, 90, 122, 0.35);
  border-color: var(--cupid-primary);
  color: #fff;
  transform: translateY(-1px);
}

.sider-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

.app-main {
  background: var(--cupid-bg-page);
}

.app-content {
  padding: 24px 28px;
  height: 100vh;
  overflow: hidden;
  background-image: var(--cupid-gradient-mesh);
}
</style>