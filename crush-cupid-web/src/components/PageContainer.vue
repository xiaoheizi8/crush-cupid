
<template>
  <div class="page-container cupid-fade-in">
    <!-- 页面头部：标题 + 副标题 + 操作区 -->
    <div class="page-header">
      <div class="page-header__title">
        <div class="page-header__icon" v-if="icon">{{ icon }}</div>
        <div class="page-header__text">
          <div class="page-header__name">{{ title }}</div>
          <div class="page-header__sub" v-if="subtitle">{{ subtitle }}</div>
        </div>
      </div>
      <div class="page-header__extra">
        <slot name="extra" />
      </div>
    </div>

    <!-- 内容区 -->
    <div class="page-body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 页面容器组件 props
 */
defineProps<{
  /** 页面标题 */
  title?: string
  /** 副标题/描述 */
  subtitle?: string
  /** 标题前缀图标（emoji 或短文本） */
  icon?: string
}>()
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: var(--cupid-gradient-soft);
  border-radius: var(--cupid-radius);
  border: 1px solid var(--cupid-border);
  box-shadow: var(--cupid-shadow-sm);
}

.page-header__title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-header__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--cupid-gradient);
  font-size: 20px;
  box-shadow: var(--cupid-shadow-sm);
  flex-shrink: 0;
}

.page-header__text {
  min-width: 0;
}

.page-header__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--cupid-text);
  line-height: 1.3;
}

.page-header__sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--cupid-text-secondary);
}

.page-header__extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.page-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-container {
    gap: 12px;
  }
  .page-header {
    flex-wrap: wrap;
    gap: 12px;
    padding: 14px 16px;
  }
  .page-header__name {
    font-size: 16px;
  }
  .page-header__extra {
    flex-wrap: wrap;
  }
}
</style>
