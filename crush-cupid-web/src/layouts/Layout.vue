<template>
  <a-layout class="app-layout">
    <a-layout-sider :width="220" class="app-sider">
      <div class="logo">
        <div class="logo__icon">💘</div>
        <div class="logo__text">
          <div class="logo__name">Cupid</div>
          <div class="logo__sub">暗恋模拟器</div>
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
          <span>对话</span>
        </a-menu-item>
        <a-menu-item key="/advisor">
          <span class="menu-icon">🧠</span>
          <span>军师</span>
        </a-menu-item>
        <a-menu-item key="/crush">
          <span class="menu-icon">💞</span>
          <span>暗恋对象</span>
        </a-menu-item>
        <a-menu-item key="/skill">
          <span class="menu-icon">📚</span>
          <span>Skill 目录</span>
        </a-menu-item>
        <a-menu-item key="/ai-provider">
          <span class="menu-icon">⚙️</span>
          <span>大模型 API</span>
        </a-menu-item>
        <a-menu-item key="/user">
          <span class="menu-icon">👤</span>
          <span>用户中心</span>
        </a-menu-item>
        <a-menu-item key="/versions">
          <span class="menu-icon">📜</span>
          <span>版本历史</span>
        </a-menu-item>
      </a-menu>
      <div v-if="user" class="sider-user">
        <div class="sider-user__name">{{ user.username }}</div>
        <div class="sider-user__email">{{ user.email }}</div>
        <a-button size="small" class="sider-user__btn" @click="handleLogout">登出</a-button>
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

async function handleLogout() {
  try {
    await logout()
  } catch { /* ignore */ }
  localStorage.removeItem('satoken')
  user.value = null
  message.success('已登出')
  router.push('/chat')
}
</script>

<style>
.app-layout {
  min-height: 100vh;
}

.app-sider {
  background: linear-gradient(180deg, #2a1f2e 0%, #3a2530 100%) !important;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.app-sider .ant-layout-sider-children {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo__icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--cupid-gradient);
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(255, 90, 122, 0.4);
  flex-shrink: 0;
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
  padding: 12px 8px;
  background: transparent !important;
  border-right: none !important;
}

.app-menu .ant-menu-item {
  border-radius: 10px;
  margin: 4px 0 !important;
  height: 42px;
  line-height: 42px;
  color: rgba(255, 255, 255, 0.75) !important;
  transition: all 0.25s ease;
}

.app-menu .ant-menu-item:hover {
  background: rgba(255, 90, 122, 0.15) !important;
  color: #fff !important;
}

.app-menu .ant-menu-item-selected {
  background: var(--cupid-gradient) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(255, 90, 122, 0.35);
}

.menu-icon {
  display: inline-block;
  margin-right: 8px;
  font-size: 15px;
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
}

.logo__icon {
  transition: box-shadow var(--cupid-transition);
}

.logo:hover .logo__icon {
  box-shadow: 0 4px 20px rgba(255, 90, 122, 0.6);
}
</style>