import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/index.css'

// 设计系统通用组件全局注册：状态块与骨架屏在全站 8+ 页面复用，
// 与上面图标注册方式保持一致，避免每个页面重复引入。
import StateBlock from './components/StateBlock.vue'
import TableSkeleton from './components/TableSkeleton.vue'
import CardSkeleton from './components/CardSkeleton.vue'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('StateBlock', StateBlock)
app.component('TableSkeleton', TableSkeleton)
app.component('CardSkeleton', CardSkeleton)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
