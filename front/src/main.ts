/**
 * 应用程序入口文件
 * <p>
 * 初始化Vue应用，配置全局插件（Pinia状态管理、路由等）
 */

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './styles/global.less';

// 创建Vue应用实例
const app = createApp(App);

// 注册Pinia状态管理
app.use(createPinia());

// 注册路由
app.use(router);

// 挂载应用到DOM
app.mount('#app');

