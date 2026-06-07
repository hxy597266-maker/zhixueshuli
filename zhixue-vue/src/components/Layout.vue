<template>
  <div>
    <!-- 顶部导航 -->
    <div class="nav">
      <div class="nav-title" @click="$router.push('/')">智学数理</div>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/course">课程学习</router-link>
        <router-link to="/community">学习社区</router-link>
        <router-link to="/aitool">AI工具</router-link>
        <router-link to="/admin">后台管理</router-link>
        <router-link to="/user">个人中心</router-link>
        <span v-if="!isLoggedIn()">
          <router-link to="/login">登录/注册</router-link>
        </span>
        <span v-else>
          <router-link to="/user">欢迎你，{{ getUsername() }}</router-link>
          <a href="javascript:void(0)" @click="handleLogout">退出</a>
        </span>
      </div>
    </div>

    <router-view />

    <footer>河池学院 · 智学数理学习平台 © 2026</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const isLoggedIn = () => {
  const token = localStorage.getItem('token')
  return !!token && token !== 'undefined' && token.length > 10
}

const getUsername = () => {
  return localStorage.getItem('username') || '用户'
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.nav {
  background: #2c3e50;
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.nav-title {
  font-size: 22px;
  font-weight: bold;
  color: white;
  cursor: pointer;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-links a {
  color: white;
  text-decoration: none;
  font-size: 15px;
  transition: opacity 0.3s;
}
.nav-links a:hover {
  opacity: 0.8;
}
.nav-links a.router-link-active {
  color: #1890ff;
  font-weight: 600;
}
footer {
  background: #2c3e50;
  color: white;
  text-align: center;
  padding: 20px;
  margin-top: 50px;
}
</style>