
<template>
  <PageContainer icon="📜" :title="t('version.title')" :subtitle="crushName ? crushName + ' 的版本记录' : t('common.loading')">
    <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
    <div v-else-if="versions.length" class="version-page">
      <a-timeline>
        <a-timeline-item v-for="v in versions" :key="v.id">
          <a-card size="small" class="version-card">
            <template #extra>
              <a-tag color="blue">v{{ v.version }}</a-tag>
            </template>
            <div class="version-date">{{ v.createdAt?.slice(0, 10) }}</div>
            <div v-if="v.reason" class="version-reason">{{ v.reason }}</div>
            <div v-if="v.snapshot" class="version-snapshot">
              <a-collapse>
                <a-collapse-panel header="查看快照" key="1">
                  <pre class="snapshot-pre">{{ v.snapshot }}</pre>
                </a-collapse-panel>
              </a-collapse>
            </div>
          </a-card>
        </a-timeline-item>
      </a-timeline>
    </div>
    <div v-else-if="!crushId" class="empty">
      <div>选择一个暗恋对象查看版本记录</div>
      <a-select
        v-model:value="pickedId"
        :options="crushOptions"
        placeholder="选择暗恋对象"
        class="empty-select"
        show-search
        option-filter-prop="label"
        @change="onPick"
      />
    </div>
    <div v-else class="empty">暂无版本记录 · 先在暗恋对象页「构建人格」生成版本</div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listVersions, listCrushes } from '@/api'
import type { Crush, VersionVO } from '@/types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const versions = ref<VersionVO[]>([])
const loading = ref(true)
const crushName = ref('')
const crushes = ref<Crush[]>([])
const pickedId = ref<number | undefined>(undefined)

const crushId = computed(() => Number(route.query.crushId) || 0)
const crushOptions = computed(() => crushes.value.map((c) => ({ label: c.name, value: c.id! })))

/** 从选择器挑人：写入 query 触发加载 */
async function onPick(id: number) {
  if (!id) return
  await router.replace({ query: { ...route.query, crushId: String(id) } })
  await load()
}

async function load() {
  loading.value = true
  try {
    if (crushId.value) {
      const data = await listVersions(crushId.value)
      versions.value = data as unknown as VersionVO[]
      // 尝试获取 crush 名称
      try {
        const { getCrush } = await import('@/api')
        const crush = await getCrush(crushId.value)
        crushName.value = crush.name
      } catch { /* ignore */ }
    } else {
      // 未指定对象：加载列表供选择（修复侧边栏直跳 /versions 时列表为空的联动缺口）
      versions.value = []
      try {
        crushes.value = await listCrushes()
      } catch { /* ignore */ }
    }
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.version-page {
  max-width: 700px;
}

.version-card {
  background: #fff;
  border-radius: var(--cupid-radius);
  box-shadow: var(--cupid-shadow-sm);
}

.version-date {
  font-size: 13px;
  color: var(--cupid-text-muted);
  margin-bottom: 8px;
}

.version-reason {
  font-size: 14px;
  color: var(--cupid-text);
  font-weight: 600;
}

.snapshot-pre {
  background: var(--cupid-bg-page);
  padding: 12px;
  border-radius: var(--cupid-radius-sm);
  font-size: 12px;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
}

.loading,
.empty {
  text-align: center;
  padding: 40px 0;
  color: var(--cupid-text-muted);
}

.empty-select {
  margin-top: 14px;
  width: 260px;
}
</style>
