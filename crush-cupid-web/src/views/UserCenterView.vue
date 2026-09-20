
<template>
  <PageContainer icon="👤" :title="t('user.title')" subtitle="我的资料 · 配额 · 账号安全">
    <div class="user-page">
      <a-row :gutter="20" class="user-row">
        <a-col :xs="24" :md="8">
          <div class="profile-card">
            <div class="profile-card__avatar">
              {{ user?.username?.charAt(0) || '?' }}
            </div>
            <div class="profile-card__name">{{ user?.username || '未登录' }}</div>
            <div class="profile-card__email">{{ user?.email }}</div>
            <div class="profile-card__status" v-if="user?.emailVerified">
              ✅ 邮箱已验证
            </div>
            <div class="profile-card__status" v-else>
              ⏳ 邮箱未验证
            </div>
            <div class="profile-card__joined">
              {{ t('user.memberSince') }}：{{ user?.createdAt ? user.createdAt.slice(0, 10) : '-' }}
            </div>
            <a-button class="profile-card__btn" @click="handleLogout">{{ t('common.logout') }}</a-button>
          </div>
        </a-col>

        <a-col :xs="24" :md="16">
          <a-card class="quota-card" :title="'📊 ' + t('user.quota')">
            <a-row :gutter="16">
              <a-col :xs="12" :md="6">
                <div class="quota-item">
                  <div class="quota-item__value">{{ quota?.crushCount || 0 }}</div>
                  <div class="quota-item__label">暗恋对象上限</div>
                </div>
              </a-col>
              <a-col :xs="12" :md="6">
                <div class="quota-item">
                  <div class="quota-item__value">{{ quota?.dailyChatLimit || 0 }}</div>
                  <div class="quota-item__label">每日对话上限</div>
                </div>
              </a-col>
              <a-col :xs="12" :md="6">
                <div class="quota-item">
                  <div class="quota-item__value">{{ quota?.todayMessageCount || 0 }}</div>
                  <div class="quota-item__label">{{ t('user.used') }}</div>
                </div>
              </a-col>
              <a-col :xs="12" :md="6">
                <div class="quota-item">
                  <div class="quota-item__value">{{ quota?.plan || '—' }}</div>
                  <div class="quota-item__label">{{ t('user.plan') }}</div>
                </div>
              </a-col>
            </a-row>
          </a-card>

          <a-card class="profile-form-card" :title="'✏️ ' + t('user.editProfile')">
            <a-form :model="profileForm" layout="vertical" @finish="handleUpdateProfile">
              <a-form-item label="用户名">
                <a-input v-model:value="profileForm.username" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" :loading="saving">
                  {{ t('common.save') }}
                </a-button>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { myProfile, myQuota, updateProfile, logout } from '@/api'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()
const user = ref<{ username: string; email: string; emailVerified: boolean; createdAt: string } | null>(null)
const quota = ref<{ plan: string; crushLimit: number; dailyChatLimit: number; todayMessageCount: number; crushCount: number } | null>(null)
const saving = ref(false)

const profileForm = ref({ username: '', email: '' })

async function load() {
  try {
    const u = await myProfile()
    user.value = u
    profileForm.value.username = u.username
    profileForm.value.email = u.email
  } catch { /* ignore */ }
  try {
    const q = await myQuota()
    quota.value = q
  } catch { /* ignore */ }
}

async function handleUpdateProfile() {
  saving.value = true
  try {
    await updateProfile({ username: profileForm.value.username })
    message.success('资料已更新')
    await load()
  } catch (e: any) {
    message.error(e?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

async function handleLogout() {
  try {
    await logout()
  } catch { /* ignore */ }
  localStorage.removeItem('satoken')
  message.success(t('common.logoutSuccess'))
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.user-page {
  padding: 0;
}

.profile-card {
  background: #fff;
  border-radius: var(--cupid-radius-lg);
  padding: 32px 24px;
  text-align: center;
  box-shadow: var(--cupid-shadow);
  border: 1px solid var(--cupid-border);
  position: relative;
  overflow: hidden;
}

.profile-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--cupid-gradient);
}

.profile-card__avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--cupid-gradient);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 4px 16px rgba(255, 90, 122, 0.3);
}

.profile-card__name {
  font-size: 20px;
  font-weight: 700;
  color: var(--cupid-text);
}

.profile-card__email {
  font-size: 13px;
  color: var(--cupid-text-secondary);
  margin-top: 4px;
}

.profile-card__status {
  font-size: 12px;
  margin-top: 8px;
}

.profile-card__joined {
  font-size: 11px;
  color: var(--cupid-text-muted);
  margin-top: 16px;
  border-top: 1px solid var(--cupid-border);
  padding-top: 16px;
}

.profile-card__btn {
  margin-top: 12px;
  background: var(--cupid-gradient);
  border-color: transparent;
  color: #fff;
  border-radius: var(--cupid-radius-sm);
  box-shadow: 0 4px 12px rgba(255, 90, 122, 0.25);
}

.profile-card__btn:hover {
  background: linear-gradient(135deg, #ff7a98 0%, #ffa066 100%) !important;
  border-color: transparent !important;
}

.quota-card {
  margin-bottom: 20px;
  border-radius: var(--cupid-radius-lg) !important;
}

.quota-item {
  text-align: center;
  padding: 16px 8px;
}

.quota-item__value {
  font-size: 28px;
  font-weight: 700;
  background: var(--cupid-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.quota-item__label {
  font-size: 12px;
  color: var(--cupid-text-secondary);
  margin-top: 4px;
}

.profile-form-card {
  margin-top: 0;
  border-radius: var(--cupid-radius-lg) !important;
}
</style>
