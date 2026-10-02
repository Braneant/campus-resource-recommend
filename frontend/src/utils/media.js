/**
 * 媒体地址过滤
 *
 * 种子数据中的封面 / 头像使用了占位域名（不存在的 OSS 地址），直接渲染会产生大量
 * 无效网络请求（浏览器控制台报 ERR_NAME_NOT_RESOLVED）以及破图 alt 文本残留。
 * 统一通过本工具过滤：占位地址一律视为「无图」，由调用方回退到图标 / 文字占位。
 */

/** 种子数据使用的占位资源域名 */
export const PLACEHOLDER_MEDIA_HOST = 'oss.campus.edu'

/**
 * 返回可直接使用的媒体地址；占位地址或空值返回空字符串。
 * @param {string} url 原始地址
 * @returns {string} 可用地址（空字符串表示无可用图片）
 */
export function usableMediaUrl(url) {
  if (!url || typeof url !== 'string') return ''
  return url.includes(PLACEHOLDER_MEDIA_HOST) ? '' : url
}
