// src/api/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',  // 使用相对路径，通过 Vite 代理到后端
  timeout: 5000
})

// 请求拦截器（带token）
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  // 验证 token 有效：存在且不是 "undefined" 字符串
  if (token && token !== 'undefined' && token.length > 10) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.status === 401) {
      // token 过期或无效，清除并跳转登录
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      // 避免路由还未准备好时跳转
      setTimeout(() => {
        window.location.href = '/login'
      }, 100)
    }
    return Promise.reject(error)
  }
)

export default request