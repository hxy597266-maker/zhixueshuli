<template>
  <div class="login-page">
    <div class="login-box">
      <h2>用户登录</h2>
      <div class="form-item">
        <label>用户名</label>
        <input v-model="username" type="text" placeholder="请输入用户名">
      </div>
      <div class="form-item">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="请输入密码">
      </div>
      <button class="btn btn-primary" style="width:100%;" @click="handleLogin">立即登录</button>
      <div class="register-link">
        没有账号？<a href="http://localhost:8080/register.html" target="_blank">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()
const username = ref('')
const password = ref('')

const handleLogin = async () => {
  if (!username.value.trim() || !password.value.trim()) {
    alert('请输入用户名和密码！')
    return
  }
  try {
    const res = await request.post('/user/login', {
      username: username.value,
      password: password.value
    })

    // 检查接口业务状态码
    if (res.code !== 200) {
      alert(res.msg || '账号或密码错误！')
      return
    }

    const token = res.data
    // 清除旧的脏数据
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.setItem('token', token)
    localStorage.setItem('username', username.value)

    alert('登录成功！')
    window.location.href = '/'

  } catch (err) {
    console.error(err)
    alert('登录失败：用户名或密码错误')
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-box {
  background: white;
  padding: 35px;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  width: 380px;
}
.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
  font-size: 24px;
}
.form-item {
  margin-bottom: 20px;
}
.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}
.form-item input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}
.form-item input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}
.register-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>