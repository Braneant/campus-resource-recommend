<template>
  <div ref="ctnDom" class="aurora-container" />
</template>

<script setup lang="ts">
import { Color, Mesh, Program, Renderer, RenderTarget, Triangle, Post } from 'ogl'
import { onMounted, onUnmounted, ref } from 'vue'
import { isLowPowerDevice } from '@/utils/motion'

const VERT = `#version 300 es
in vec2 position;
void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}
`

const FRAG = `#version 300 es
precision highp float;

uniform float uTime;
uniform float uAmplitude;
uniform float uBlend;
uniform vec3 uColorStops[3];
uniform vec3 uWarm;
uniform vec2 uResolution;
uniform vec2 uPointer;
uniform float uPointerActive;
uniform float uGrain;
uniform float uIntensity;

out vec4 fragColor;

vec3 permute(vec3 x) {
  return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v) {
  const vec4 C = vec4(
      0.211324865405187, 0.366025403784439,
      -0.577350269189626, 0.024390243902439
  );
  vec2 i  = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);

  vec3 p = permute(
      permute(i.y + vec3(0.0, i1.y, 1.0))
    + i.x + vec3(0.0, i1.x, 1.0)
  );

  vec3 m = max(
      0.5 - vec3(
          dot(x0, x0),
          dot(x12.xy, x12.xy),
          dot(x12.zw, x12.zw)
      ),
      0.0
  );
  m = m * m;
  m = m * m;

  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);

  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

float fbm(vec2 p) {
  float v = 0.0;
  float a = 0.55;
  mat2 rot = mat2(1.62, 1.18, -1.18, 1.62);
  for (int i = 0; i < 4; i++) {
    v += a * snoise(p);
    p = rot * p;
    a *= 0.5;
  }
  return v;
}

float hash21(vec2 p) {
  p = fract(p * vec2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

vec3 aces(vec3 x) {
  const float a = 2.51;
  const float b = 0.03;
  const float c = 2.43;
  const float d = 0.59;
  const float e = 0.14;
  return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 ramp(float t) {
  vec3 c = mix(uColorStops[0], uColorStops[1], smoothstep(0.0, 0.55, t));
  c = mix(c, uColorStops[2], smoothstep(0.45, 1.0, t));
  c += uWarm * pow(smoothstep(0.62, 1.0, t), 3.0) * 0.22;
  return c;
}

void main() {
  vec2 uv = gl_FragCoord.xy / uResolution;
  vec2 ptr = (uPointer - 0.5) * 2.0;
  float pa = uPointerActive;

  vec2 p1 = uv + ptr * vec2(0.012, 0.007) * pa;
  vec2 p2 = uv - ptr * vec2(0.007, 0.005) * pa;

  float t = uTime;
  float amp = clamp(uAmplitude, 0.3, 2.5);
  float n1 = fbm(vec2(p1.x * 2.4 + t * 0.05, p1.y * 1.6 - t * 0.035));
  float n2 = fbm(vec2(p2.x * 5.2 - t * 0.08, p2.y * 2.6 + t * 0.05));
  float curtain = (n1 * 0.65 + n2 * 0.35) * amp;

  float rays = fbm(vec2(p1.x * 30.0 + t * 0.02, p1.y * 1.2 - t * 0.10));
  rays = smoothstep(0.30, 0.80, rays);

  float soft = mix(0.10, 0.36, clamp(uBlend, 0.0, 1.0));
  float horizon = 0.58 + 0.07 * sin(uv.x * 3.2 + t * 0.08) + 0.055 * curtain;
  float dist = abs(uv.y - horizon);
  float ribbon = exp(-pow(dist / soft, 2.0));
  float core = exp(-pow(dist / (soft * 0.38), 2.0));

  float heightFade = smoothstep(0.0, 0.22, uv.y) * (0.72 + 0.28 * (1.0 - uv.y));
  float intensity = (ribbon * (0.65 + 0.35 * rays) + core * 0.35) * heightFade;
  intensity *= 0.75 + 0.35 * smoothstep(-0.4, 0.6, curtain);
  intensity *= clamp(uIntensity, 0.0, 1.5);

  float horizon2 = 0.78 + 0.05 * sin(uv.x * 2.1 - t * 0.06) + 0.045 * n2 * amp;
  float dist2 = abs(uv.y - horizon2);
  float ribbon2 = exp(-pow(dist2 / (soft * 0.7), 2.0));
  intensity += ribbon2 * 0.16 * (0.5 + 0.5 * rays);

  vec3 col = ramp(clamp(uv.x * 0.85 + uv.y * 0.15, 0.0, 1.0));
  col *= 0.95 + 0.38 * intensity;
  col += uWarm * pow(clamp(intensity, 0.0, 1.0), 3.0) * 0.10;

  float grain = hash21(gl_FragCoord.xy + fract(t) * 100.0) - 0.5;
  col += grain * uGrain;
  col += (hash21(gl_FragCoord.xy) - 0.5) / 255.0;
  col = aces(col * 1.18);

  float alpha = clamp(intensity * 1.35, 0.0, 0.95);
  fragColor = vec4(col * alpha, alpha);
}
`

const BRIGHT_FRAG = `
precision highp float;
uniform sampler2D tMap;
uniform float uThreshold;
uniform float uKnee;
varying vec2 vUv;

void main() {
  vec3 c = texture2D(tMap, vUv).rgb;
  float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
  float f = smoothstep(uThreshold, uThreshold + uKnee, l);
  gl_FragColor = vec4(c * f, 1.0);
}
`

const BLUR_FRAG = `
precision highp float;
uniform sampler2D tMap;
uniform vec2 uTexel;
uniform vec2 uDirection;
varying vec2 vUv;

void main() {
  vec2 o1 = uDirection * uTexel;
  vec2 o2 = o1 * 2.0;
  vec2 o3 = o1 * 3.0;
  vec2 o4 = o1 * 4.0;

  vec3 sum = texture2D(tMap, vUv).rgb * 0.227027;
  sum += (texture2D(tMap, vUv + o1).rgb + texture2D(tMap, vUv - o1).rgb) * 0.1945946;
  sum += (texture2D(tMap, vUv + o2).rgb + texture2D(tMap, vUv - o2).rgb) * 0.1216216;
  sum += (texture2D(tMap, vUv + o3).rgb + texture2D(tMap, vUv - o3).rgb) * 0.054054;
  sum += (texture2D(tMap, vUv + o4).rgb + texture2D(tMap, vUv - o4).rgb) * 0.016216;

  gl_FragColor = vec4(sum, 1.0);
}
`

const COMPOSITE_FRAG = `
precision highp float;
uniform sampler2D tMap;
uniform sampler2D uScene;
uniform float uBloom;
uniform float uCA;
uniform float uVignette;
varying vec2 vUv;

void main() {
  vec2 d = vUv - 0.5;
  float ca = uCA * dot(d, d);
  vec3 scene;
  scene.r = texture2D(uScene, vUv + d * ca).r;
  scene.g = texture2D(uScene, vUv).g;
  scene.b = texture2D(uScene, vUv - d * ca).b;

  vec3 bloom = texture2D(tMap, vUv).rgb;
  vec3 col = scene + bloom * uBloom;

  float vig = 1.0 - smoothstep(0.28, 0.95, length(d) * 1.3);
  col *= mix(1.0, vig, uVignette);

  float a = texture2D(uScene, vUv).a;
  gl_FragColor = vec4(col, a);
}
`

interface AuroraProps {
  colorStops?: string[]
  amplitude?: number
  blend?: number
  speed?: number
  warm?: string
  bloom?: number
  grain?: number
  intensity?: number
  post?: boolean
}

const props = withDefaults(defineProps<AuroraProps>(), {
  colorStops: () => ['#5b8def', '#2f6df6', '#8b5cf6'],
  amplitude: 1.0,
  blend: 0.5,
  speed: 1.0,
  warm: '#ffd9a0',
  bloom: 0.55,
  grain: 0.028,
  intensity: 1,
  post: true
})

const ctnDom = ref<HTMLDivElement | null>(null)

let raf = 0
let running = false
let last = 0
let time = 0
let renderer: InstanceType<typeof Renderer> | null = null
let sceneProgram: InstanceType<typeof Program> | null = null
let sceneMesh: InstanceType<typeof Mesh> | null = null
let sceneTarget: InstanceType<typeof RenderTarget> | null = null
let post: InstanceType<typeof Post> | null = null
let blurH: any = null
let blurV: any = null
let dpr = 1
let usePost = true
let pointerActive = 0
let pointerActiveTarget = 0
const pointer = { x: 0.5, y: 0.5 }
const pointerTarget = { x: 0.5, y: 0.5 }

let resizeHandler: (() => void) | null = null
let visibilityHandler: (() => void) | null = null
let pointerHandler: ((e: PointerEvent) => void) | null = null
let motionHandler: (() => void) | null = null
let motionQuery: MediaQueryList | null = null
let contextLostHandler: ((e: Event) => void) | null = null

function normalizeStops(list: string[]) {
  const fallback = ['#5b8def', '#2f6df6', '#8b5cf6']
  const src = Array.isArray(list) && list.length >= 2 ? list : fallback
  const picked = [src[0], src[Math.min(1, src.length - 1)], src[src.length - 1]]
  return picked.map((hex) => {
    const c = new Color(hex)
    return [c.r, c.g, c.b]
  })
}

function toRgb(hex: string) {
  const c = new Color(hex)
  return [c.r, c.g, c.b]
}

function halfSize(width: number, height: number) {
  return {
    width: Math.max(1, Math.floor((width * dpr) / 2)),
    height: Math.max(1, Math.floor((height * dpr) / 2))
  }
}

function stop() {
  running = false
  if (raf) cancelAnimationFrame(raf)
  raf = 0
}

function start() {
  if (running) return
  running = true
  last = performance.now()
  raf = requestAnimationFrame(update)
}

function update(now: number) {
  if (!running) return
  raf = requestAnimationFrame(update)

  const dt = Math.min(0.05, Math.max(0, (now - last) / 1000))
  last = now
  time += dt * props.speed

  const k = 1 - Math.exp(-dt * 7)
  pointer.x += (pointerTarget.x - pointer.x) * k
  pointer.y += (pointerTarget.y - pointer.y) * k
  pointerActive += (pointerActiveTarget - pointerActive) * Math.min(1, dt * 5)

  if (!renderer || !sceneProgram || !sceneMesh) return

  sceneProgram.uniforms.uTime.value = time
  sceneProgram.uniforms.uPointer.value = [pointer.x, pointer.y]
  sceneProgram.uniforms.uPointerActive.value = pointerActive

  const withPost = usePost && !!post && !!sceneTarget
  renderer.render({ scene: sceneMesh, target: withPost ? sceneTarget : null })
  if (withPost) {
    post.render({ texture: sceneTarget.texture })
  }
}

onMounted(() => {
  const ctn = ctnDom.value
  if (!ctn) return

  const width = ctn.offsetWidth
  const height = ctn.offsetHeight
  if (width < 2 || height < 2) return

  dpr = Math.min(window.devicePixelRatio || 1, isLowPowerDevice() ? 1.25 : 1.75)
  usePost = props.post !== false && !isLowPowerDevice()

  renderer = new Renderer({
    alpha: true,
    premultipliedAlpha: true,
    antialias: false,
    dpr
  })

  const gl = renderer.gl
  gl.clearColor(0, 0, 0, 0)
  gl.enable(gl.BLEND)
  gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
  gl.canvas.style.backgroundColor = 'transparent'

  renderer.setSize(width, height)

  const geometry = new Triangle(gl)
  // 场景只需要 position；uv 留着也不会影响，但删掉能少一个 attribute
  delete (geometry.attributes as any).uv

  sceneProgram = new Program(gl, {
    vertex: VERT,
    fragment: FRAG,
    transparent: true,
    depthTest: false,
    depthWrite: false,
    uniforms: {
      uTime: { value: 0 },
      uAmplitude: { value: props.amplitude },
      uBlend: { value: props.blend },
      uColorStops: { value: normalizeStops(props.colorStops) },
      uWarm: { value: toRgb(props.warm) },
      uResolution: { value: [width * dpr, height * dpr] },
      uPointer: { value: [0.5, 0.5] },
      uPointerActive: { value: 0 },
      uGrain: { value: props.grain },
      uIntensity: { value: props.intensity }
    }
  })

  sceneMesh = new Mesh(gl, { geometry, program: sceneProgram })
  ctn.appendChild(gl.canvas)

  sceneTarget = new RenderTarget(gl, {
    width: Math.max(1, Math.floor(width * dpr)),
    height: Math.max(1, Math.floor(height * dpr)),
    depth: false
  })

  if (usePost) {
    try {
      const half = halfSize(width, height)
      post = new Post(gl, { width: half.width, height: half.height, dpr: 1, depth: false })

      post.addPass({
        fragment: BRIGHT_FRAG,
        uniforms: {
          uThreshold: { value: 0.52 },
          uKnee: { value: 0.22 }
        }
      })

      blurH = post.addPass({
        fragment: BLUR_FRAG,
        uniforms: {
          uTexel: { value: [1 / half.width, 1 / half.height] },
          uDirection: { value: [1, 0] }
        }
      })

      blurV = post.addPass({
        fragment: BLUR_FRAG,
        uniforms: {
          uTexel: { value: [1 / half.width, 1 / half.height] },
          uDirection: { value: [0, 1] }
        }
      })

      post.addPass({
        fragment: COMPOSITE_FRAG,
        uniforms: {
          uScene: { value: sceneTarget.texture },
          uBloom: { value: props.bloom },
          uCA: { value: 0.0015 },
          uVignette: { value: 0.22 }
        }
      })
    } catch (e) {
      // 后处理失败不影响主画面，退回到单 pass 渲染
      console.warn('[Aurora] post-processing disabled:', e)
      usePost = false
      post = null
    }
  }

  resizeHandler = () => {
    if (!ctn || !renderer || !sceneProgram || !sceneTarget) return
    const w = ctn.offsetWidth
    const h = ctn.offsetHeight
    if (w < 2 || h < 2) return

    renderer.setSize(w, h)
    sceneTarget.setSize(Math.max(1, Math.floor(w * dpr)), Math.max(1, Math.floor(h * dpr)))
    sceneProgram.uniforms.uResolution.value = [w * dpr, h * dpr]

    if (usePost && post) {
      const half = halfSize(w, h)
      post.resize({ width: half.width, height: half.height, dpr: 1 })
      if (blurH) blurH.uniforms.uTexel.value = [1 / half.width, 1 / half.height]
      if (blurV) blurV.uniforms.uTexel.value = [1 / half.width, 1 / half.height]
    }
  }

  visibilityHandler = () => {
    if (document.hidden) stop()
    else start()
  }

  pointerHandler = (e: PointerEvent) => {
    if (!ctn) return
    const rect = ctn.getBoundingClientRect()
    if (!rect.width || !rect.height) return
    const x = (e.clientX - rect.left) / rect.width
    const yTop = (e.clientY - rect.top) / rect.height
    if (x < -0.05 || x > 1.05 || yTop < -0.05 || yTop > 1.05) {
      pointerActiveTarget = 0
      return
    }
    pointerTarget.x = Math.min(1, Math.max(0, x))
    pointerTarget.y = Math.min(1, Math.max(0, 1 - yTop))
    pointerActiveTarget = 1
  }

  contextLostHandler = (e: Event) => {
    e.preventDefault()
    stop()
    if (renderer) renderer.gl.canvas.style.opacity = '0'
  }

  window.addEventListener('resize', resizeHandler)
  document.addEventListener('visibilitychange', visibilityHandler)
  window.addEventListener('pointermove', pointerHandler, { passive: true })
  gl.canvas.addEventListener('webglcontextlost', contextLostHandler as EventListener, false)

  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionHandler = () => {
    if (!motionQuery) return
    if (motionQuery.matches) {
      stop()
      if (renderer) renderer.gl.canvas.style.opacity = '0'
    } else {
      if (renderer) renderer.gl.canvas.style.opacity = '1'
      start()
    }
  }
  motionQuery.addEventListener?.('change', motionHandler)

  start()
})

onUnmounted(() => {
  stop()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (visibilityHandler) document.removeEventListener('visibilitychange', visibilityHandler)
  if (pointerHandler) window.removeEventListener('pointermove', pointerHandler)
  if (motionQuery && motionHandler) motionQuery.removeEventListener?.('change', motionHandler)

  if (renderer) {
    const ctn = ctnDom.value
    const canvas = renderer.gl.canvas
    canvas.removeEventListener('webglcontextlost', contextLostHandler as EventListener)
    if (ctn && canvas.parentNode === ctn) ctn.removeChild(canvas)
    renderer.gl.getExtension('WEBGL_lose_context')?.loseContext()
  }

  post = null
  sceneTarget = null
  sceneMesh = null
  sceneProgram = null
  renderer = null
})
</script>

<style scoped>
.aurora-container {
  width: 100%;
  height: 100%;
}
</style>
