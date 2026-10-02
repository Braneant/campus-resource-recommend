/**
 * 动效统一开关与能力探测
 * 演示或答辩前如需一键关闭所有动效，把 MOTION_ENABLED 改为 false 即可（页面外观保持，仅去掉动画）。
 */
export const MOTION_ENABLED = true

/**
 * 探测浏览器是否支持 WebGL（用于极光背景的优雅降级）
 * 不支持时自动回退为静态渐变背景，不会出现白屏或报错。
 */
export function isWebGLAvailable() {
  try {
    const canvas = document.createElement('canvas')
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    )
  } catch (e) {
    return false
  }
}

/** 是否启用极光背景（开关开启 且 浏览器支持 WebGL） */
export function canUseAurora() {
  return MOTION_ENABLED && isWebGLAvailable()
}
