
<template>
  <PageContainer icon="📜" title="版本历史" :subtitle="crushName ? crushName + ' 的版本记录' : '加载中...'">
    <div v-if="loading" class="loading">加载中...</div>
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
    <div v-else class="empty">暂无版本记录</div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { listVersions } from '@/api'
import type { VersionVO } from '@/types'

const route = useRoute()
const versions = ref<VersionVO[]>([])
const loading = ref(true)
const crushName = ref('')

async function load() {
  try {
    const crushId = Number(route.query.crushId)
    if (crushId) {
      const data = await listVersions(crushId)
      versions.value = data as unknown as VersionVO[]
      // 尝试获取 crush 名称
      try {
        const { getCrush } = await import('@/api')
        const crush = await getCrush(crushId)
        crushName.value = crush.name
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
</style>
