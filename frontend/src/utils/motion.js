/**
 * 动效统一开关与能力探测
 * 演示或答辩前如需一键关闭所有动效，把 MOTION_ENABLED 改为 false 即可（页面外观保持，仅去掉动画）。
 */
export const MOTION_ENABLED = true

/** 用户是否开启了系统的"减弱动态效果" */
export function prefersReducedMotion() {
  try {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  } catch (e) {
    return false
  }
}

/**
 * 探测 WebGL2。极光着色器用了 #version 300 es，必须用 WebGL2。
 * 只测 WebGL1 会误判：浏览器能建 WebGL1 上下文，但着色器编译会失败。
 */
export function isWebGL2Available() {
  try {
    const canvas = document.createElement('canvas')
    return !!(window.WebGL2RenderingContext && canvas.getContext('webgl2'))
  } catch (e) {
    return false
  }
}

/** 旧名字保留，避免其他页面引用时断掉；现在按 WebGL2 判断 */
export function isWebGLAvailable() {
  return isWebGL2Available()
}

/** 低配设备：跳过 bloom 后处理，DPR 上限也更低 */
export function isLowPowerDevice() {
  try {
    const cores = navigator.hardwareConcurrency || 8
    const memory = navigator.deviceMemory || 8
    return cores <= 4 || memory <= 4
  } catch (e) {
    return false
  }
}

/** 是否启用极光背景（开关开启、未减弱动效、且浏览器支持 WebGL2） */
export function canUseAurora() {
  return MOTION_ENABLED && !prefersReducedMotion() && isWebGL2Available()
}
