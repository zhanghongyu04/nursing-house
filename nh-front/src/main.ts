import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './assets/style/global.css'

import App from './App.vue'
import router from './router'

// 应用启动入口：统一挂载 UI 框架、状态管理和路由。
const app = createApp(App)
app.use(ElementPlus, { locale: zhCn }); // 全局引入 Element Plus（中文）
app.use(createPinia())
app.use(router)

app.mount('#app')
