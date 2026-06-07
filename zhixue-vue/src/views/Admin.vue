<template>
  <div>
    <!-- 登录框 -->
    <div v-if="!isAdmin" class="admin-login-box">
      <h3>管理员登录</h3>
      <input v-model="adminUser" type="text" placeholder="管理员账号">
      <input v-model="adminPwd" type="password" placeholder="管理员密码">
      <button @click="adminLogin">登录后台</button>
    </div>

    <!-- 后台面板 -->
    <div v-else>
      <div class="admin-nav">
        <div class="admin-nav-title">智学数理 - 管理员后台</div>
        <div class="admin-nav-links">
          <a href="/">返回首页</a>
          <a href="javascript:void(0)" @click="adminLogout">退出后台</a>
        </div>
      </div>

      <div class="admin-container">
        <!-- 统计 -->
        <div class="stats-row">
          <div class="stat-item">
            <h4>{{ users.length }}</h4>
            <p>注册用户数</p>
          </div>
          <div class="stat-item">
            <h4>{{ posts.length }}</h4>
            <p>社区帖子数</p>
          </div>
          <div class="stat-item">
            <h4>{{ comments.length }}</h4>
            <p>评论总数</p>
          </div>
        </div>

        <!-- 用户管理 -->
        <div class="admin-card">
          <h3>用户管理</h3>
          <table>
            <thead>
              <tr><th>用户ID</th><th>用户名</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.id }}</td>
                <td>{{ u.username }}</td>
                <td><button class="btn btn-del" @click="delUser(u.id)">删除</button></td>
              </tr>
              <tr v-if="users.length === 0"><td colspan="3">加载中...</td></tr>
            </tbody>
          </table>
        </div>

        <!-- 帖子管理 -->
        <div class="admin-card">
          <h3>帖子管理</h3>
          <table>
            <thead>
              <tr><th>ID</th><th>发布者</th><th>标题</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in posts" :key="p.id">
                <td>{{ p.id }}</td>
                <td>{{ p.username }}</td>
                <td>{{ p.title }}</td>
                <td><button class="btn btn-del" @click="delPost(p.id)">删除</button></td>
              </tr>
              <tr v-if="posts.length === 0"><td colspan="4">加载中...</td></tr>
            </tbody>
          </table>
        </div>

        <!-- 评论管理 -->
        <div class="admin-card">
          <h3>评论管理</h3>
          <table>
            <thead>
              <tr><th>评论ID</th><th>帖子ID</th><th>评论者</th><th>内容</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="c in comments" :key="c.id">
                <td>{{ c.id }}</td>
                <td>{{ c.postId }}</td>
                <td>{{ c.username }}</td>
                <td>{{ c.content }}</td>
                <td><button class="btn btn-del" @click="delComment(c.id)">删除</button></td>
              </tr>
              <tr v-if="comments.length === 0"><td colspan="5">加载中...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()
const isAdmin = ref(false)
const adminUser = ref('admin1')
const adminPwd = ref('admin123')
const users = ref([])
const posts = ref([])
const comments = ref([])

// 管理员登录
const adminLogin = async () => {
  try {
    const res = await request.post('/user/admin/login', {
      username: adminUser.value,
      password: adminPwd.value
    })
    if (res.code === 200) {
      localStorage.setItem('token', res.data)
      isAdmin.value = true
      loadAllData()
    } else {
      alert(res.msg || '登录失败')
    }
  } catch (err) {
    console.error(err)
    alert('请求失败')
  }
}

// 加载所有数据
const loadAllData = async () => {
  await loadUserList()
  await loadPostList()
  await loadCommentList()  // 依赖 posts 先加载完
}

const loadUserList = async () => {
  try {
    const res = await request.get('/user/list')
    if (res.code === 200) users.value = res.data || []
  } catch (err) {
    console.error(err)
  }
}

const loadPostList = async () => {
  try {
    const res = await request.get('/post/list', { params: { page: 1, size: 50 } })
    if (res.code === 200 && res.data) {
      posts.value = res.data.list || []
    }
  } catch (err) {
    console.error(err)
  }
}

const loadCommentList = async () => {
  try {
    const ps = posts.value
    if (!ps.length) { comments.value = []; return }
    // 并行加载所有帖子的评论
    const results = await Promise.allSettled(
      ps.map(p => request.get('/comment/list', { params: { postId: p.id } }))
    )
    const all = []
    results.forEach((r, i) => {
      if (r.status === 'fulfilled' && r.value?.code === 200 && r.value?.data) {
        for (const c of r.value.data) {
          all.push({ ...c, postTitle: ps[i]?.title || '' })
        }
      }
    })
    comments.value = all
  } catch (err) {
    console.error(err)
  }
}

const delUser = async (userId) => {
  if (!confirm('确定删除该用户？')) return
  try {
    const res = await request.delete(`/user/delete/${userId}`)
    if (res.code === 200) {
      loadUserList()
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const delPost = async (postId) => {
  if (!confirm('确定删除该帖子？')) return
  try {
    const res = await request.delete(`/post/delete/${postId}`)
    if (res.code === 200) {
      loadPostList()
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const delComment = async (commentId) => {
  if (!confirm('确定删除该评论？')) return
  try {
    const res = await request.delete(`/comment/delete/${commentId}`)
    if (res.code === 200) {
      loadCommentList()
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const adminLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  isAdmin.value = false
  router.push('/')
}

onMounted(async () => {
  // 检查是否已是管理员
  const token = localStorage.getItem('token')
  if (token && token !== 'undefined' && token.length > 10) {
    try {
      const res = await request.get('/user/info')
      if (res.code === 200 && res.data && res.data.role === 1) {
        isAdmin.value = true
        loadAllData()
      }
    } catch (err) {
      // 不是管理员
    }
  }
})
</script>

<style scoped>
.admin-login-box {
  width: 400px;
  margin: 120px auto;
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.08);
}
.admin-login-box h3 {
  text-align: center;
  margin-bottom: 20px;
  color: #2c3e50;
}
.admin-login-box input {
  width: 100%;
  padding: 12px;
  margin-bottom: 15px;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-sizing: border-box;
}
.admin-login-box button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background: linear-gradient(135deg,#4096ff,#2168e3);
  color: white;
  font-weight: bold;
  font-size: 16px;
}
.admin-nav {
  background: #2c3e50;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.admin-nav-title {
  color: white;
  font-size: 22px;
  font-weight: bold;
}
.admin-nav-links a {
  color: white;
  text-decoration: none;
  margin-left: 20px;
}
.admin-container {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 20px;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}
.stat-item {
  background: white;
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 3px 12px rgba(0,0,0,0.08);
}
.stat-item h4 {
  color: #2168e3;
  font-size: 28px;
  margin-bottom: 8px;
}
.stat-item p {
  color: #666;
}
.admin-card {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.08);
  margin-bottom: 30px;
}
.admin-card h3 {
  margin-bottom: 20px;
  color: #2c3e50;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th, td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}
th {
  background: #f8f9ff;
  color: #2c3e50;
}
.btn {
  border: none;
  padding: 6px 12px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
}
.btn-del {
  background: #e74c3c;
  color: white;
}
.btn-del:hover {
  background: #c0392b;
}
</style>
