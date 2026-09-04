
<template>
  <PageContainer icon="📊" title="关系报告详情" :subtitle="report?.title || '加载中...'">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="report" class="report-page">
      <a-card class="report-card">
        <template #extra>
          <a-button @click="handleDownload">📥 下载 .docx</a-button>
        </template>
        <div class="report-meta">
          <span>对象：{{ report.crushName }}</span>
          <span>报告日期：{{ report.reportDate }}</span>
          <span>来源：{{ report.source }}</span>
          <span>创建时间：{{ report.createdAt?.slice(0, 10) }}</span>
        </div>
        <div v-if="report.title" class="report-title">{{ report.title }}</div>
        <div v-if="report.markdown" class="report-content" style="white-space: pre-line;">
          {{ report.markdown }}
        </div>
        <div v-else class="report-empty">暂无报告内容</div>
      </a-card>
    </div>
    <div v-else class="empty">报告不存在</div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getReportDetail, downloadSavedReport } from '@/api'

const route = useRoute()
const router = useRouter()
const report = ref<any>(null)
const loading = ref(true)

async function load() {
  try {
    const id = Number(route.params.id)
    report.value = await getReportDetail(id)
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleDownload() {
  if (!report.value) return
  try {
    const crushName = report.value.crushName || ''
    await downloadSavedReport(report.value.id, crushName)
  } catch (e: any) {
    message.error(e?.message || '下载失败')
  }
}

onMounted(load)
</script>

<style scoped>
.report-page {
  max-width: 800px;
}

.report-card {
  background: #fff;
  border-radius: var(--cupid-radius-lg);
  box-shadow: var(--cupid-shadow);
}

.report-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--cupid-text-secondary);
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--cupid-border);
}

.report-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--cupid-text);
  margin-bottom: 24px;
  line-height: 1.4;
}

.report-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--cupid-text);
  white-space: pre-line;
}

.report-empty {
  color: var(--cupid-text-muted);
  text-align: center;
  padding: 40px 0;
}

.loading,
.empty {
  text-align: center;
  padding: 40px 0;
  color: var(--cupid-text-muted);
}
</style>
