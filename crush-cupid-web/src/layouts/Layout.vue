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
        <a-menu-item v-for="item in navItems" :key="item.key">
          <span class="menu-icon">{{ item.icon }}</span>
          <span>{{ t(item.i18nKey) }}</span>
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

    <!-- 移动端顶栏 -->
    <div class="app-topbar">
      <div class="app-topbar__left">
        <div class="app-topbar__icon">💘</div>
        <div class="app-topbar__title">Cupid</div>
      </div>
      <div class="app-topbar__right">
        <a-button type="text" size="small" class="app-topbar__lang" @click="switchLang">
          {{ locale === 'zh-CN' ? 'EN' : '中' }}
        </a-button>
        <a-button type="text" class="app-topbar__burger" @click="drawerOpen = true" aria-label="菜单">
          ☰
        </a-button>
      </div>
    </div>

    <!-- 移动端抽屉菜单 -->
    <a-drawer
      v-model:open="drawerOpen"
      placement="left"
      :width="280"
      class="app-drawer-wrap"
    >
      <div class="drawer-inner">
        <div class="drawer-logo">
          <div class="drawer-logo__icon">💘</div>
          <div class="drawer-logo__text">
            <div class="drawer-logo__name">Cupid</div>
            <div class="drawer-logo__sub">{{ t('nav.subtitle') }}</div>
          </div>
        </div>
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="drawer-menu"
          @click="onDrawerMenuClick"
        >
          <a-menu-item v-for="item in navItems" :key="item.key">
            <span class="menu-icon">{{ item.icon }}</span>
            <span>{{ t(item.i18nKey) }}</span>
          </a-menu-item>
        </a-menu>
        <div v-if="user" class="drawer-user">
          <div class="drawer-user__name">{{ user.username }}</div>
          <div class="drawer-user__email">{{ user.email }}</div>
          <a-button block class="drawer-user__btn" @click="handleLogout">{{ t('common.logout') }}</a-button>
        </div>
      </div>
    </a-drawer>

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
const drawerOpen = ref(false)

const navItems = [
  { key: '/chat', icon: '💬', i18nKey: 'nav.chat' },
  { key: '/advisor', icon: '🧠', i18nKey: 'nav.advisor' },
  { key: '/crush', icon: '💞', i18nKey: 'nav.crush' },
  { key: '/skill', icon: '📚', i18nKey: 'nav.skill' },
  { key: '/ai-provider', icon: '⚙️', i18nKey: 'nav.aiProvider' },
  { key: '/user', icon: '👤', i18nKey: 'nav.user' },
  { key: '/voice', icon: '🎙️', i18nKey: 'nav.voice' },
  { key: '/versions', icon: '📜', i18nKey: 'nav.versions' },
]

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

function onDrawerMenuClick({ key }: { key: string }) {
  selectedKeys.value = [key]
  drawerOpen.value = false
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
  router.push('/login')
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

.app-menu.ant-menu-dark .ant-menu-item {
  border-radius: var(--cupid-radius-sm);
  margin: 3px 0 !important;
  height: 42px;
  line-height: 42px;
  color: #fff !important;
  transition: all var(--cupid-transition);
}
.app-menu.ant-menu-dark .ant-menu-item:hover {
  background: rgba(196, 78, 200, 0.18) !important;
  color: #fff !important;
  transform: translateX(2px);
}
.app-menu.ant-menu-dark .ant-menu-item-selected {
  background: var(--cupid-gradient) !important;
  color: #fff !important;
  box-shadow: 0 4px 16px rgba(196, 78, 200, 0.45);
  font-weight: 500;
}

/* 任何 antd 深色主题默认样式都不可能盖掉最亮的白字 */
.app-menu.ant-menu-dark .ant-menu-item .ant-menu-title-content {
  color: #fff !important;
}
.app-menu.ant-menu-dark .ant-menu-item-selected .ant-menu-title-content {
  color: #fff !important;
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
  min-width: 0;
}

.app-content {
  padding: 24px 28px;
  height: 100vh;
  overflow: hidden;
  background-image: var(--cupid-gradient-mesh);
}

/* 移动端顶栏（桌面隐藏） */
.app-topbar {
  display: none;
}

/* 抽屉菜单：antd Drawer 使用 teleport 渲染到 body，.app-drawer-wrap 与 .ant-drawer-content 非 DOM 父子，需用同级选择器 */
.ant-drawer.app-drawer-wrap .ant-drawer-content {
  background: linear-gradient(180deg, #140a2e 0%, #1e1248 40%, #2d1b4e 100%);
}
.ant-drawer.app-drawer-wrap .ant-drawer-content-wrapper {
  background: transparent;
}
.ant-drawer.app-drawer-wrap .ant-drawer-body {
  background: transparent;
  padding: 0;
}
.drawer-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.drawer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 8px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.drawer-logo__icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--cupid-radius-sm);
  background: var(--cupid-gradient);
  font-size: 22px;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(196, 78, 200, 0.5);
}
.drawer-logo__name {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}
.drawer-logo__sub {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  margin-top: 2px;
}
.drawer-menu {
  flex: 1;
  background: transparent !important;
  border-right: none !important;
}
.drawer-menu .ant-menu-item {
  border-radius: var(--cupid-radius-sm);
  margin: 3px 0 !important;
  height: 42px;
  line-height: 42px;
  color: #fff !important;
}
.drawer-menu .ant-menu-item:hover {
  background: rgba(196, 78, 200, 0.18) !important;
  color: #fff !important;
}
.drawer-menu .ant-menu-item-selected {
  background: var(--cupid-gradient) !important;
  color: #fff !important;
  box-shadow: 0 4px 16px rgba(196, 78, 200, 0.45);
}
.drawer-menu .ant-menu-item .ant-menu-title-content {
  color: #fff !important;
}
.drawer-menu .ant-menu-item-selected .ant-menu-title-content {
  color: #fff !important;
}
.drawer-user {
  padding: 16px 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.drawer-user__name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.drawer-user__email {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  margin-top: 2px;
}
.drawer-user__btn {
  margin-top: 12px;
  background: rgba(255, 90, 122, 0.2);
  border-color: rgba(255, 90, 122, 0.4);
  color: #fff;
  border-radius: var(--cupid-radius-sm);
}
.drawer-user__btn:hover {
  background: rgba(255, 90, 122, 0.35);
  border-color: var(--cupid-primary);
  color: #fff;
}

/* 移动端适配 */
@media (max-width: 768px) {
  /* 强制根布局为纵向：覆盖 ant-layout-has-sider 的 row 方向 */
  .app-layout {
    height: 100vh;
    height: 100dvh;
    flex-direction: column !important;
  }
  .app-sider {
    display: none !important;
  }
  .app-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 52px;
    flex-shrink: 0;
    padding: 0 8px 0 14px;
    background: linear-gradient(180deg, #140a2e 0%, #1e1248 60%, #2d1b4e 100%);
    z-index: 100;
  }
  .app-topbar__left {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .app-topbar__icon {
    font-size: 20px;
  }
  .app-topbar__title {
    color: #fff;
    font-size: 16px;
    font-weight: 700;
  }
  .app-topbar__lang {
    color: rgba(255, 255, 255, 0.75) !important;
  }
  .app-topbar__burger {
    color: #fff;
    font-size: 20px;
  }
  .app-main {
    flex: 1;
    min-height: 0;
    width: 100% !important;
    max-width: 100%;
  }
  .app-content {
    height: 100%;
    padding: 12px 12px calc(12px + env(safe-area-inset-bottom));
    overflow: hidden;
  }
}
</style>