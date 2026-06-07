<template>
  <div class="community-page">
    <div class="container">
      <!-- 左侧：帖子流 -->
      <div class="feed">
        <!-- 搜索框 -->
        <div class="sidebar-card" style="margin-bottom: 20px;">
          <div class="search-box">
            <input v-model="keyword" placeholder="搜索标题或内容..." @keyup.enter="searchPosts">
            <button @click="searchPosts">搜索</button>
            <button class="reset-btn" @click="resetSearch">重置</button>
          </div>
        </div>

        <!-- loading -->
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
        </div>

        <!-- 帖子列表 -->
        <div v-else-if="postList.length === 0" style="text-align:center;color:#999;padding:60px 20px;">
          <div style="font-size:48px;margin-bottom:10px;">📭</div>
          <div style="font-size:16px;">暂无帖子，快来发布第一条吧！</div>
        </div>

        <div v-else>
          <div v-for="p in postList" :key="p.id" class="post">
            <!-- 帖子头部 -->
            <div class="post-header">
              <div class="avatar">
                <img v-if="p.userAvatar" :src="p.userAvatar" style="width:100%;height:100%;object-fit:cover;">
                <span v-else>{{ (p.username || '匿')[0] }}</span>
              </div>
              <div class="post-meta">
                <div class="post-author">{{ p.username || '匿名' }}</div>
                <div class="post-time">{{ p.createTime || '' }}</div>
              </div>
            </div>

            <!-- 帖子标题 -->
            <div class="post-title">
              <router-link :to="'/post/' + p.id" style="text-decoration:none;color:#2c3e50;">
                {{ p.title }}
              </router-link>
            </div>

            <!-- 帖子内容（截断） -->
            <div v-if="shouldTruncate(p)" class="post-content" style="margin-bottom:10px;">
              {{ truncateContent(p) }}...
            </div>
            <div v-else class="post-content">{{ p.content || '' }}</div>

            <!-- 帖子图片 -->
            <div v-if="!shouldTruncate(p) && getImages(p).length > 0">
              <img v-for="(img, idx) in getImages(p)" :key="idx" :src="img" class="post-image" @click="window.open(img)">
            </div>

            <!-- 详情链接（截断时） -->
            <router-link v-if="shouldTruncate(p)" :to="'/post/' + p.id"
              style="color:#1890ff;text-decoration:none;font-size:14px;display:inline-block;margin-bottom:15px;">
              查看详情 →
            </router-link>

            <!-- 操作栏 -->
            <div class="post-actions">
              <div class="action-btn" :class="{ liked: p.likedByCurrentUser }" @click="like(p)">
                <span class="icon">{{ p.likedByCurrentUser ? '⭐' : '☆' }}</span>
                <span>{{ p.likeCount || 0 }}</span>
              </div>
              <div class="action-btn">
                <span class="icon">💬</span>
                <span>{{ p.commentCount || 0 }}</span>
              </div>
            </div>

            <!-- 评论区 -->
            <div class="comment-section">
              <div class="comment-input-box">
                <input v-model="commentInputs[p.id]" placeholder="写下你的评论...">
                <button @click="addComment(p)">发送</button>
              </div>
              <div v-if="commentData[p.id]">
                <div id="commentContainer">
                  <div v-for="(c, idx) in visibleComments(p)" :key="c.id" class="comment-item">
                    <div class="comment-header">
                      <div class="comment-author">{{ c.username }}</div>
                      <div style="display:flex;align-items:center;">
                        <div class="comment-time">{{ formatTime(c.createTime) }}</div>
                        <span v-if="canDeleteComment(p, c)" style="color:#ff4d4f;cursor:pointer;margin-left:10px;font-size:12px;" @click="deleteComment(c.id, p.id)">删除</span>
                      </div>
                    </div>
                    <div class="comment-text">{{ c.content }}</div>
                  </div>
                </div>
                <button v-if="commentData[p.id].length > 8" class="expand-btn show" @click="toggleExpand(p.id)">
                  {{ expandedComments[p.id] ? '收起回复' : `展开 ${commentData[p.id].length - 8} 条隐藏评论` }}
                </button>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <button :disabled="currentPage <= 1" @click="loadList(currentPage - 1)">上一页</button>
            <span>第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
            <button :disabled="currentPage >= totalPages" @click="loadList(currentPage + 1)">下一页</button>
          </div>
        </div>
      </div>

      <!-- 右侧：发帖栏 -->
      <div class="sidebar">
        <div class="sidebar-card post-box">
          <div class="sidebar-title">发布新帖子</div>
          <input v-model="newPost.title" placeholder="输入标题...">
          <textarea v-model="newPost.content" placeholder="分享你的学习心得..."></textarea>
          <div class="preview-container">
            <label class="upload-btn" @click="triggerImageUpload">+</label>
            <input ref="fileInput" type="file" accept="image/*" multiple style="display:none" @change="onFileSelect">
            <div v-for="(file, idx) in selectedFiles" :key="idx" class="preview-item">
              <img :src="file.preview">
              <div class="delete-btn" @click="removeFile(idx)">×</div>
            </div>
          </div>
          <button class="publish-btn" :disabled="publishing" @click="addPost">
            {{ publishing ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

// 模块级：第一次加载显示loading，后面静默
let _loaded = false

const router = useRouter()

// 帖子列表
const postList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const total = ref(0)
const pageSize = 10

// 搜索
const keyword = ref('')

// 发帖
const newPost = reactive({ title: '', content: '' })
const selectedFiles = ref([])
const fileInput = ref(null)
const publishing = ref(false)

// 评论
const commentInputs = reactive({})
const commentData = reactive({})
const expandedComments = reactive({})

// 获取当前用户 ID（从 token）
const getUserIdFromToken = () => {
  const token = localStorage.getItem('token')
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload))
    return decoded.id || decoded.userId
  } catch { return null }
}

// 检查登录
const checkLogin = () => {
  if (!localStorage.getItem('token')) {
    alert('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// 加载帖子列表
const loadList = async (page = 1) => {
  if (!checkLogin()) return
  const firstLoad = !pageSize  // 只第一次显示loading
  if (firstLoad) loading.value = true
  try {
    const res = await request.get('/post/list', { params: { page, size: pageSize } })
    if (res.code === 200) {
      const result = res.data
      postList.value = result.list || []
      currentPage.value = result.page || 1
      totalPages.value = result.pages || 1
      total.value = result.total || 0
      // 从缓存恢复点赞状态和数字
      const m = JSON.parse(localStorage.getItem('lk_map') || '{}')
      for (const post of postList.value) {
        const c = m[post.id]
        if (c) {
          post.likedByCurrentUser = c.liked
          post.likeCount = c.count
        }
      }
      // 加载评论
      postList.value.forEach(p => loadComments(p.id))
    }
  } catch (err) {
    console.error('加载帖子失败:', err)
  } finally {
    if (firstLoad) loading.value = false
  }
}

// 获取图片数组
const getImages = (p) => {
  if (!p.image) return []
  return p.image.split(',').filter(img => img && img.trim() !== '')
}

// 判断是否需要截断
const shouldTruncate = (p) => {
  const images = getImages(p)
  const imgLen = images.length * 50
  const txtLen = (p.content || '').length
  return (txtLen + imgLen) > 200
}

const truncateContent = (p) => {
  return (p.content || '').substring(0, 200)
}

// 搜索
const searchPosts = async () => {
  if (!keyword.value.trim()) { alert('请输入搜索关键词'); return }
  loading.value = true
  try {
    const res = await request.get('/post/search', { params: { keyword: keyword.value.trim() } })
    if (res.code === 200) {
      postList.value = res.data || []
      postList.value.forEach(p => loadComments(p.id))
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  keyword.value = ''
  loadList(1)
}

const like = async (p) => {
  const wasLiked = p.likedByCurrentUser
  const wasCount = p.likeCount || 0
  p.likedByCurrentUser = !wasLiked
  p.likeCount = wasLiked ? Math.max(0, wasCount - 1) : wasCount + 1
  // 同时存 liked 和 likeCount
  const m = JSON.parse(localStorage.getItem('lk_map') || '{}')
  m[p.id] = { liked: p.likedByCurrentUser, count: p.likeCount }
  localStorage.setItem('lk_map', JSON.stringify(m))

  try {
    const res = await request.post('/like/add', { postId: p.id })
    if (res.code !== 200) {
      p.likedByCurrentUser = wasLiked
      p.likeCount = wasCount
      alert(res.msg || '点赞失败')
    }
  } catch (err) {
    p.likedByCurrentUser = wasLiked
    p.likeCount = wasCount
  }
}

// 图片选择
const triggerImageUpload = () => {
  fileInput.value?.click()
}

const onFileSelect = (e) => {
  const files = Array.from(e.target.files)
  files.forEach(f => {
    if (!selectedFiles.value.some(s => s.name === f.name && s.size === f.size)) {
      selectedFiles.value.push({
        file: f,
        name: f.name,
        size: f.size,
        preview: URL.createObjectURL(f)
      })
    }
  })
  e.target.value = ''
}

const removeFile = (idx) => {
  URL.revokeObjectURL(selectedFiles.value[idx].preview)
  selectedFiles.value.splice(idx, 1)
}

// 发布帖子
const addPost = async () => {
  if (!newPost.title.trim() || !newPost.content.trim()) {
    alert('请输入标题和内容')
    return
  }
  publishing.value = true
  try {
    const token = localStorage.getItem('token')
    let imageUrls = []
    for (const item of selectedFiles.value) {
      const formData = new FormData()
      formData.append('image', item.file)
      const uploadRes = await fetch('/api/post/uploadImage', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        body: formData
      })
      const uploadData = await uploadRes.json()
      if (uploadData.code === 200) {
        imageUrls.push(uploadData.data)
      }
    }
    const res = await request.post('/post/add', {
      title: newPost.title.trim(),
      content: newPost.content.trim(),
      image: imageUrls.join(',')
    })
    if (res.code === 200) {
      newPost.title = ''
      newPost.content = ''
      selectedFiles.value = []
      loadList(1)
    } else {
      alert(res.msg || '发布失败')
    }
  } catch (err) {
    console.error(err)
    alert('发布出错，请重试')
  } finally {
    publishing.value = false
  }
}

// 评论
const addComment = async (p) => {
  const content = commentInputs[p.id]?.trim()
  if (!content) { alert('请输入评论内容'); return }
  try {
    const res = await request.post('/comment/add', { postId: p.id, content })
    if (res.code === 200) {
      commentInputs[p.id] = ''
      loadComments(p.id)
    } else {
      alert(res.msg || '评论失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const loadComments = async (pid) => {
  try {
    const res = await request.get('/comment/list', { params: { postId: pid } })
    if (res.code === 200) {
      commentData[pid] = res.data || []
    }
  } catch (err) {
    // 静默处理，不刷屏
  }
}

const visibleComments = (p) => {
  const comments = commentData[p.id] || []
  if (expandedComments[p.id]) return comments
  return comments.slice(0, 8)
}

const toggleExpand = (pid) => {
  expandedComments[pid] = !expandedComments[pid]
}

const currentUserId = getUserIdFromToken()

const canDeleteComment = (p, c) => {
  const uid = getUserIdFromToken()
  return uid && (c.userId === uid)
}

const deleteComment = async (commentId, postId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    const res = await request.delete(`/comment/delete/${commentId}`)
    if (res.code === 200) {
      loadComments(postId)
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (err) {
    console.error(err)
  }
}

// 格式化时间（相对时间）
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  let date
  if (Array.isArray(timeStr)) {
    const [y, m, d, h, min, s] = timeStr
    date = new Date(y, m - 1, d, h, min, s)
  } else {
    date = new Date(timeStr)
  }
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const months = Math.floor(days / 30)
  const years = Math.floor(months / 12)
  if (years > 0) return `${years}年前`
  if (months > 0) return `${months}个月前`
  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  if (seconds > 0) return `${seconds}秒前`
  return '刚刚'
}

onMounted(() => {
  if (checkLogin()) {
    loadList()
  }
})
</script>

<style scoped>
.community-page {
  background: #f5f6f7;
  color: #1a1a1a;
  min-height: 100vh;
}
.container {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 30px;
  display: grid;
  grid-template-columns: 1fr 500px;
  gap: 25px;
}
.feed {
  min-height: 100vh;
}
.post {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  transition: box-shadow 0.3s;
}
.post:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.post-meta {
  flex: 1;
}
.post-author {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}
.post-time {
  font-size: 12px;
  color: #999;
}
.post-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #1a1a1a;
  line-height: 1.4;
}
.post-content {
  color: #444;
  line-height: 1.8;
  margin-bottom: 15px;
  font-size: 15px;
}
.post-image {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 15px;
  max-height: 500px;
  object-fit: cover;
  cursor: pointer;
}
.post-actions {
  display: flex;
  gap: 20px;
  padding-top: 15px;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.3s;
}
.action-btn:hover {
  color: #1890ff;
}
.action-btn.liked {
  color: #ff4d4f;
}
.action-btn .icon {
  font-size: 18px;
}
.comment-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}
.comment-input-box {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}
.comment-input-box input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  font-size: 14px;
}
.comment-input-box button {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}
.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}
.comment-author {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}
.comment-time {
  color: #999;
  font-size: 12px;
}
.comment-text {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}
.expand-btn {
  display: block;
  width: 100%;
  padding: 10px;
  background: #f5f7fb;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #1890ff;
  font-size: 14px;
  margin-top: 10px;
  transition: background 0.3s;
}
.expand-btn:hover {
  background: #e8f4ff;
}
.search-box {
  display: flex;
  gap: 10px;
}
.search-box input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
}
.search-box button {
  padding: 10px 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}
.search-box .reset-btn {
  background: #f0f0f0;
  color: #333;
}
.search-box .reset-btn:hover {
  background: #e0e0e0;
}
.sidebar-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}
.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}
.pagination button:hover:not(:disabled) {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}
.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.post-box {
  padding: 35px;
}
.post-box .sidebar-title {
  font-size: 22px;
  margin-bottom: 20px;
  color: #333;
  font-weight: 600;
}
.post-box input,
.post-box textarea {
  width: 100%;
  padding: 15px 18px;
  border: 2px solid #d0d5db;
  border-radius: 10px;
  margin-bottom: 15px;
  font-size: 16px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}
.post-box input:focus,
.post-box textarea:focus {
  outline: none;
  border-color: #1890ff;
}
.post-box textarea {
  min-height: 150px;
  resize: none;
}
.upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 70px;
  height: 70px;
  border: 3px dashed #1890ff;
  border-radius: 12px;
  font-size: 36px;
  color: #1890ff;
  cursor: pointer;
  margin-bottom: 15px;
  transition: all 0.3s;
  background: #f0f7ff;
}
.upload-btn:hover {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}
.publish-btn {
  padding: 15px 30px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  transition: all 0.3s;
  width: 100%;
}
.publish-btn:hover {
  background: #0d7ae8;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24,144,255,0.4);
}
.publish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.preview-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 15px;
  align-items: flex-start;
}
.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background: rgba(255,77,79,0.9);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
.delete-btn:hover {
  background: #ff4d4f;
  transform: scale(1.1);
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  overflow: hidden;
  margin-right: 10px;
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