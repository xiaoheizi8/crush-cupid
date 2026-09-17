/**
 * @className i18n
 * @description 国际化入口：注册 vue-i18n 实例，支持 zh-CN / en-US 切换
 * @author cupid
 * @code i18n setup
 * @createTime 2026-09-04
 */

import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

/** 支持的语言列表 */
export const SUPPORTED_LOCALES = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en-US', label: 'English' },
] as const

/** 从 localStorage 读取语言偏好，默认中文 */
function getDefaultLocale(): string {
  const saved = localStorage.getItem('cupid-locale')
  if (saved && SUPPORTED_LOCALES.some(l => l.value === saved)) return saved
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  locale: getDefaultLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export default i18n
