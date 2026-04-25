/**
 * HTTP请求工具
 * <p>
 * 基于axios封装的HTTP请求工具，提供：
 * - 统一的请求/响应拦截器
 * - Token自动注入
 * - 错误统一处理
 * - 特殊状态码自定义处理
 */

import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { message } from './message';
import { useAuthStore } from '@/store/auth';

/**
 * 创建axios实例
 * <p>
 * 配置基础URL、超时时间和默认请求头
 */
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

/**
 * 请求拦截器
 * <p>
 * 在发送请求前自动添加Token到请求头
 */
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 如果是 FormData，移除默认的 Content-Type，让浏览器自动设置 multipart/form-data 边界
    if (config.data instanceof FormData) {
      if (config.headers) {
        delete config.headers['Content-Type'];
      }
    } else {
      // 非 FormData 请求默认使用 JSON
      if (config.headers && !config.headers['Content-Type']) {
        config.headers['Content-Type'] = 'application/json';
      }
    }

    const authStore = useAuthStore();
    if (authStore.token && config.headers) {
      config.headers['Authorization'] = `${authStore.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * 特殊状态码处理函数映射表
 * <p>
 * 用于注册自定义的状态码处理逻辑，可以覆盖默认的错误提示行为
 */
const specialCodeHandlers: Record<number, (msg: string, data?: any) => void> = {
  // 示例：可以在这里添加特殊code的处理
  // 401: (msg) => { ... },
  // 403: (msg) => { ... },
};

/**
 * 注册特殊状态码的处理函数
 * <p>
 * 允许为特定的状态码注册自定义处理逻辑，覆盖默认的错误提示
 *
 * @param code 状态码
 * @param handler 处理函数，接收错误消息和数据作为参数
 *
 * @example
 * ```typescript
 * registerSpecialCodeHandler(500, (msg, data) => {
 *   // 自定义处理逻辑
 *   message.warning(msg);
 * });
 * ```
 */
export const registerSpecialCodeHandler = (
  code: number,
  handler: (msg: string, data?: any) => void
) => {
  specialCodeHandlers[code] = handler;
};

/**
 * 响应拦截器
 * <p>
 * 统一处理响应数据：
 * - 验证响应格式
 * - 处理错误状态码
 * - 提取响应数据
 * - 处理401未授权（自动登出）
 */
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    // 检查响应数据格式
    if (!res || typeof res !== 'object') {
      message.error('响应数据格式错误');
      return Promise.reject(new Error('响应数据格式错误'));
    }
    
    // 如果code存在且不等于200，显示错误信息
    if (res.code !== undefined && res.code !== null && res.code !== 200) {
      const errorMsg = res.msg || res.message || '请求失败';
      // 检查是否有特殊处理函数
      if (specialCodeHandlers[res.code]) {
        specialCodeHandlers[res.code](errorMsg, res.data);
      } else {
        message.error(errorMsg);
      }
      return Promise.reject(new Error(errorMsg));
    }
    
    // code为200或不存在code字段（可能是直接返回数据），返回数据
    return res.data !== undefined ? res.data : res;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      if (status === 401) {
        const authStore = useAuthStore();
        authStore.logout();
        // 优先显示后端返回的错误信息
        const errorMsg = data?.msg || data?.message || '登录已过期，请重新登录';
        message.error(errorMsg);
      } else {
        // 优先显示后端返回的错误信息
        const errorMsg = data?.msg || data?.message || `请求失败: ${status}`;
        message.error(errorMsg);
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      message.error('网络错误，请检查网络连接');
    } else {
      // 请求配置出错
      message.error('请求配置错误');
    }
    return Promise.reject(error);
  }
);

export default http;

