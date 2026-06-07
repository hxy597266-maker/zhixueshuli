<template>
  <div class="profile-page">
    <div class="profile-wrapper">
      <div class="profile-container">

        <!-- 左侧个人信息模块 -->
        <div class="profile-left">
          <div class="user-header">
            <div class="avatar-wrap">
              <div class="avatar" id="userAvatar" :style="avatarStyle">{{ avatarLetter }}</div>
              <label class="upload-avatar-btn">
                更换头像
                <input type="file" accept="image/*" style="display:none" @change="uploadAvatar">
              </label>
            </div>
            <div class="user-info">
              <h2>{{ userInfo.username || '用户' }}</h2>
              <div class="status">已登录</div>
            </div>
          </div>

          <!-- 数据统计 -->
          <div class="stats-box">
            <div class="stat-item">
              <h3>{{ userInfo.postCount || 0 }}</h3>
              <p>发布帖子</p>
            </div>
            <div class="stat-item">
              <h3>{{ userInfo.likeCount || 0 }}</h3>
              <p>获得点赞</p>
            </div>
            <div class="stat-item">
              <h3>{{ userInfo.commentCount || 0 }}</h3>
              <p>被评论</p>
            </div>
          </div>

          <!-- 功能按钮 -->
          <div class="action-btns">
            <button class="btn-change-password" @click="showUsernameModal = true">修改用户名</button>
            <button class="btn-change-password" @click="showPasswordModal = true">修改密码</button>
            <button class="btn-logout" @click="handleLogout">退出登录</button>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div class="profile-right">
          <div class="tabs">
            <div class="tab" :class="{ active: activeTab === 'myPost' }" @click="switchTab('myPost')">我发布的帖子</div>
            <div class="tab" :class="{ active: activeTab === 'myLike' }" @click="switchTab('myLike')">我点赞的帖子</div>
          </div>

          <!-- loading -->
          <div v-if="tabLoading" class="loading">
            <div class="loading-spinner"></div>
          </div>

          <!-- 我发布的帖子 -->
          <div v-else-if="activeTab === 'myPost'">
            <div v-if="myPosts.length === 0" class="empty-state">
              <div class="empty-state-icon">📝</div>
              <div>暂无发布的帖子</div>
            </div>
            <div v-for="post in myPosts" :key="post.id" class="post-item" @click="$router.push('/post/' + post.id)">
              <div class="post-item-title">{{ post.title }}</div>
              <div class="post-item-content">{{ truncate(post.content, 100) }}</div>
              <div class="post-item-meta">
                <span>{{ post.createTime || '' }}</span>
                <span>⭐ {{ post.likeCount || 0 }} 点赞</span>
                <span>💬 {{ post.commentCount || 0 }} 评论</span>
              </div>
            </div>
          </div>

          <!-- 我点赞的帖子 -->
          <div v-else>
            <div v-if="likedPosts.length === 0" class="empty-state">
              <div class="empty-state-icon">⭐</div>
              <div>暂无点赞的帖子</div>
            </div>
            <div v-for="post in likedPosts" :key="post.id" class="post-item" @click="$router.push('/post/' + post.id)">
              <div class="post-item-title">{{ post.title }}</div>
              <div class="post-item-content">{{ truncate(post.content, 100) }}</div>
              <div class="post-item-meta">
                <span>作者：{{ post.username || '匿名' }}</span>
                <span>⭐ {{ post.likeCount || 0 }} 点赞</span>
                <span>💬 {{ post.commentCount || 0 }} 评论</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 修改用户名弹窗 -->
    <div class="modal" :class="{ active: showUsernameModal }">
      <div class="modal-content">
        <h3>修改用户名</h3>
        <div class="form-group">
          <label>新用户名</label>
          <input v-model="newUsername" type="text" placeholder="请输入新用户名">
        </div>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showUsernameModal = false">取消</button>
          <button class="btn-confirm" @click="updateUsername">确认修改</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div class="modal" :class="{ active: showPasswordModal }">
      <div class="modal-content">
        <h3>修改密码</h3>
        <div class="form-group">
          <label>旧密码</label>
          <input v-model="oldPassword" type="password" placeholder="请输入旧密码">
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="newPassword" type="password" placeholder="请输入新密码">
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input v-model="confirmPassword" type="password" placeholder="请再次输入新密码">
        </div>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showPasswordModal = false">取消</button>
          <button class="btn-confirm" @click="updatePassword">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()

// 用户信息
const userInfo = reactive({ username: '', avatar: '', postCount: 0, likeCount: 0, commentCount: 0 })
const avatarStyle = computed(() => {
  return userInfo.avatar ? { background: `url(${userInfo.avatar}) center/cover`, border: 'none' } : {}
})
const avatarLetter = computed(() => {
  return userInfo.avatar ? '' : (userInfo.username ? userInfo.username[0] : '用')
})

// 帖子列表
const myPosts = ref([])
const likedPosts = ref([])
const activeTab = ref('myPost')
const tabLoading = ref(false)

// 修改用户名
const showUsernameModal = ref(false)
const newUsername = ref('')

// 修改密码
const showPasswordModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

// 检查登录
const checkLogin = () => {
  if (!localStorage.getItem('token')) {
    alert('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await request.get('/user/info')
    if (res.code === 200 && res.data) {
      const info = res.data
      userInfo.username = info.username
      userInfo.avatar = info.avatar || ''
      userInfo.postCount = info.postCount || 0
      userInfo.likeCount = info.likeCount || 0
      userInfo.commentCount = info.commentCount || 0
    }
  } catch (err) {
    console.error(err)
  }
}

// 加载我发布的帖子
const loadMyPosts = async () => {
  tabLoading.value = true
  try {
    const res = await request.get('/post/myPosts')
    if (res.code === 200) {
      myPosts.value = res.data || []
    }
  } catch (err) {
    console.error(err)
  } finally {
    tabLoading.value = false
  }
}

// 加载我点赞的帖子
const loadLikedPosts = async () => {
  tabLoading.value = true
  try {
    const res = await request.get('/post/likedPosts')
    if (res.code === 200) {
      likedPosts.value = res.data || []
    }
  } catch (err) {
    console.error(err)
  } finally {
    tabLoading.value = false
  }
}

// 切换标签
const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'myPost') loadMyPosts()
  else loadLikedPosts()
}

// 截断文字
const truncate = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}

// 修改用户名
const updateUsername = async () => {
  if (!newUsername.value.trim()) { alert('请输入新用户名'); return }
  if (newUsername.value.trim().length < 2 || newUsername.value.trim().length > 20) {
    alert('用户名长度必须在2-20个字符之间')
    return
  }
  try {
    const res = await request.put('/user/updateUsername', { username: newUsername.value.trim() })
    if (res.code === 200) {
      alert('修改成功')
      showUsernameModal.value = false
      loadUserInfo()
      localStorage.setItem('username', newUsername.value.trim())
    } else {
      alert(res.msg || '修改失败')
    }
  } catch (err) {
    console.error(err)
    alert('修改出错，请重试')
  }
}

// 修改密码
const updatePassword = async () => {
  if (!oldPassword.value) { alert('请输入旧密码'); return }
  if (!newPassword.value) { alert('请输入新密码'); return }
  if (newPassword.value !== confirmPassword.value) { alert('两次输入的新密码不一致'); return }
  if (newPassword.value.length < 6) { alert('新密码长度不能少于6位'); return }
  try {
    const res = await request.put('/user/updatePassword', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    if (res.code === 200) {
      alert('密码修改成功，请重新登录')
      showPasswordModal.value = false
      handleLogout()
    } else {
      alert(res.msg || '修改失败')
    }
  } catch (err) {
    console.error(err)
    alert('修改出错，请重试')
  }
}

// 上传头像
const uploadAvatar = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { alert('请上传图片文件'); return }
  if (file.size > 10 * 1024 * 1024) { alert('图片大小不能超过 10MB'); return }

  try {
    const formData = new FormData()
    formData.append('avatar', file)
    const token = localStorage.getItem('token')
    const res = await fetch('/api/user/uploadAvatar', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token },
      body: formData
    })
    const data = await res.json()
    if (data.code === 200) {
      userInfo.avatar = data.data
      alert('头像上传成功')
    } else {
      alert('上传失败：' + (data.msg || data.message))
    }
  } catch (err) {
    console.error(err)
    alert('上传出错，请检查网络')
  }
  e.target.value = ''
}

onMounted(() => {
  if (checkLogin()) {
    loadUserInfo()
    loadMyPosts()
  }
})
</script>

<style scoped>
.profile-page {
  background: #f5f6f7;
  color: #1a1a1a;
  min-height: 100vh;
}
.profile-wrapper {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 30px;
}
.profile-container {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 25px;
}
.profile-left {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  height: fit-content;
}
.user-header {
  text-align: center;
  margin-bottom: 25px;
  padding-bottom: 25px;
  border-bottom: 1px solid #f0f0f0;
}
.avatar-wrap {
  position: relative;
  display: inline-block;
  margin-bottom: 15px;
}
.avatar {
  width: 100px;
  height: 100px;
  font-size: 36px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}
.upload-avatar-btn {
  position: absolute;
  bottom: 5px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 15px;
  cursor: pointer;
  white-space: nowrap;
}
.user-info h2 {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.user-info .status {
  color: #52c41a;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.user-info .status::before {
  content: '';
  width: 8px;
  height: 8px;
  background: #52c41a;
  border-radius: 50%;
  display: inline-block;
}
.stats-box {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 25px;
}
.stat-item {
  text-align: center;
  padding: 15px 8px;
  background: #f5f7fb;
  border-radius: 8px;
}
.stat-item h3 {
  font-size: 22px;
  color: #1890ff;
  margin-bottom: 5px;
  font-weight: 600;
}
.stat-item p {
  color: #666;
  font-size: 12px;
}
.action-btns {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-btns button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s;
}
.btn-change-password {
  background: #1890ff;
  color: white;
}
.btn-change-password:hover {
  background: #0d7ae8;
}
.btn-logout {
  background: #f0f0f0;
  color: #666;
}
.btn-logout:hover {
  background: #e0e0e0;
}
.profile-right {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 15px;
}
.tab {
  padding: 10px 20px;
  cursor: pointer;
  color: #666;
  font-size: 16px;
  border-radius: 8px;
  transition: all 0.3s;
  font-weight: 500;
}
.tab:hover {
  background: #f5f7fb;
  color: #1890ff;
}
.tab.active {
  color: #1890ff;
  background: #e8f4ff;
  font-weight: 600;
}
.post-item {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;
}
.post-item:hover {
  background: #f5f7fb;
}
.post-item:last-child {
  border-bottom: none;
}
.post-item-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  cursor: pointer;
}
.post-item-title:hover {
  color: #1890ff;
}
.post-item-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 2;
}
.post-item-meta {
  display: flex;
  gap: 20px;
  color: #999;
  font-size: 13px;
}
.post-item-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}
.empty-state-icon {
  font-size: 48px;
  margin-bottom: 15px;
}
.modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal.active {
  display: flex;
}
.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
}
.modal-content h3 {
  margin-bottom: 20px;
  color: #1a1a1a;
  font-size: 18px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
}
.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-group input:focus {
  outline: none;
  border-color: #1890ff;
}
.modal-btns {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.modal-btns button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
}
.btn-cancel {
  background: #f0f0f0;
  color: #666;
}
.btn-confirm {
  background: #1890ff;
  color: white;
}
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #4096ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>