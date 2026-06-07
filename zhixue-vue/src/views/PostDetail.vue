<template>
  <div class="detail-page">
    <div class="container">
      <a class="back-btn" @click="$router.push('/community')">← 返回社区</a>

      <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

      <div v-else-if="post" class="post-detail">
        <div class="post-header">
          <div class="avatar">
            <img v-if="post.userAvatar" :src="post.userAvatar" style="width:100%;height:100%;object-fit:cover;">
            <span v-else>{{ (post.username || '匿')[0] }}</span>
          </div>
          <div class="post-meta">
            <div class="post-author">{{ post.username || '匿名' }}</div>
            <div class="post-time">{{ formatTime(post.createTime) }}</div>
          </div>
          <button v-if="isAuthor" class="del-btn" @click="deletePost">删除帖子</button>
        </div>
        <div class="post-title">{{ post.title }}</div>
        <div class="post-content">{{ post.content }}</div>
        <img v-for="(img, idx) in getImages(post)" :key="idx" :src="img" class="post-image" @click="window.open(img)">
        <div class="post-actions">
          <span class="action-btn" :class="{ liked: post.likedByCurrentUser }" @click="toggleLike">{{ post.likedByCurrentUser ? '⭐' : '☆' }} {{ post.likeCount || 0 }} 点赞</span>
          <span class="action-btn">💬 {{ post.commentCount || 0 }} 评论</span>
        </div>
      </div>

      <div v-else style="text-align:center;padding:60px;color:#999;">帖子不存在</div>

      <div v-if="post" class="comment-section">
        <h3>评论</h3>
        <div class="comment-input-box">
          <input v-model="commentText" placeholder="写下你的评论...">
          <button @click="addComment">发送</button>
        </div>
        <div v-if="cLoading" class="loading"><div class="loading-spinner"></div></div>
        <div v-else-if="commentList.length===0" style="text-align:center;color:#999;padding:20px;">暂无评论</div>
        <div v-else>
          <div v-for="c in commentList" :key="c.id" class="comment-item">
            <b>{{ c.username }}</b>
            <span style="color:#999;font-size:12px;margin-left:8px;">{{ formatTime(c.createTime) }}</span>
            <span v-if="canDel(c)" style="color:#f44;cursor:pointer;margin-left:8px;font-size:12px;" @click="deleteComment(c.id)">删除</span>
            <div style="margin-top:4px;">{{ c.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const loading = ref(true)
const commentList = ref([])
const cLoading = ref(false)
const commentText = ref('')

const uid = () => { try { const t=localStorage.getItem('token'); if(!t) return null; const p=JSON.parse(atob(t.split('.')[1])); return p.id||p.userId } catch { return null } }
const curUid = uid()
const isAuthor = computed(() => curUid && post.value && curUid === post.value.userId)
const getImages = (p) => { if(!p||!p.image) return []; return p.image.split(',').filter(i=>i&&i.trim()) }
const canDel = (c) => curUid && (c.userId===curUid || (post.value&&post.value.userId===curUid))

const loadPost = async () => {
  const pid = route.params.id
  if (!pid) { router.push('/community'); return }
  loading.value = true
  try {
    const r = await request.get('/post/detail/' + pid)
    if (r.code!==200) { alert(r.msg||'帖子不存在'); router.push('/community'); return }
    post.value = r.data
    // 从缓存恢复点赞状态和数字
    try {
      const m = JSON.parse(localStorage.getItem('lk_map') || '{}')
      const c = m[pid]
      if (c && typeof c === 'object') {
        post.value.likedByCurrentUser = c.liked
        post.value.likeCount = c.count
      }
    } catch {}
    loadC()
  } catch(e) { console.error(e) } finally { loading.value = false }
}

const loadC = async () => {
  cLoading.value = true
  try { const r = await request.get('/comment/list', { params: { postId: route.params.id } }); if (r.code===200) commentList.value = r.data||[] } catch(e) {} finally { cLoading.value = false }
}

const toggleLike = async () => {
  if (!post.value) return
  const oldLiked = post.value.likedByCurrentUser
  const oldCount = post.value.likeCount || 0
  post.value.likedByCurrentUser = !oldLiked
  post.value.likeCount = oldLiked ? Math.max(0, oldCount - 1) : oldCount + 1
  const m = JSON.parse(localStorage.getItem('lk_map') || '{}')
  m[route.params.id] = { liked: post.value.likedByCurrentUser, count: post.value.likeCount }
  localStorage.setItem('lk_map', JSON.stringify(m))
  try {
    const r = await request.post('/like/add', { postId: route.params.id })
    if (r.code !== 200) {
      post.value.likedByCurrentUser = oldLiked
      post.value.likeCount = oldCount
      alert(r.msg || '点赞失败')
    }
  } catch(e) {
    post.value.likedByCurrentUser = oldLiked
    post.value.likeCount = oldCount
    alert('点赞出错')
  }
}

const addComment = async () => {
  const c = commentText.value.trim(); if (!c) { alert('请输入评论'); return }
  try { const r = await request.post('/comment/add', { postId: route.params.id, content: c }); if (r.code===200) { commentText.value = ''; loadC(); loadPost() } else alert(r.msg||'失败') } catch(e) {}
}

const deleteComment = async (id) => {
  if (!confirm('确定删除？')) return
  try { const r = await request.delete('/comment/delete/'+id); if (r.code===200) { loadC(); if (post.value) post.value.commentCount = Math.max(0,(post.value.commentCount||0)-1) } else alert(r.msg||'失败') } catch(e) {}
}

const deletePost = async () => {
  if (!confirm('确定删除这篇帖子？')) return
  try { const r = await request.delete('/post/delete/'+route.params.id); if (r.code===200) { alert('已删除'); router.push('/community') } else alert(r.msg||'失败') } catch(e) {}
}

const formatTime = (t) => {
  if (!t) return '';
  const d = Array.isArray(t) ? new Date(t[0],t[1]-1,t[2],t[3],t[4],t[5]) : new Date(t);
  const ms = Date.now()-d.getTime(), min=ms/60000, h=min/60, day=h/24, mon=day/30, y=mon/12;
  if (y>=1) return Math.floor(y)+'年前';
  if (mon>=1) return Math.floor(mon)+'个月前';
  if (day>=1) return Math.floor(day)+'天前';
  if (h>=1) return Math.floor(h)+'小时前';
  if (min>=1) return Math.floor(min)+'分钟前';
  return '刚刚';
}

onMounted(() => loadPost())
</script>

<style scoped>
.detail-page{background:#f5f6f7;min-height:100vh}.container{max-width:900px;margin:20px auto;padding:0 20px}
.back-btn{display:inline-block;margin-bottom:20px;padding:10px 20px;background:#fff;color:#1890ff;border:1px solid #1890ff;border-radius:8px;cursor:pointer}
.post-detail{background:#fff;border-radius:12px;padding:30px;box-shadow:0 1px 3px rgba(0,0,0,.1);margin-bottom:20px}
.post-header{display:flex;align-items:center;margin-bottom:20px;padding-bottom:15px;border-bottom:1px solid #f0f0f0}
.avatar{width:40px;height:40px;border-radius:50%;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:16px;overflow:hidden;flex-shrink:0}
.post-meta{margin-left:12px;flex:1}.post-author{font-weight:600;font-size:16px;color:#333}.post-time{font-size:13px;color:#999}
.del-btn{margin-left:auto;padding:8px 16px;background:#ff4d4f;color:#fff;border:none;border-radius:6px;cursor:pointer;font-size:14px}
.post-title{font-size:24px;font-weight:700;margin-bottom:20px;color:#1a1a1a}.post-content{color:#444;line-height:1.8;margin-bottom:20px;font-size:16px;white-space:pre-wrap}
.post-image{width:100%;border-radius:8px;margin-bottom:15px;max-height:600px;object-fit:cover;cursor:pointer}
.post-actions{display:flex;gap:20px;padding-top:20px;border-top:1px solid #f0f0f0}
.action-btn{display:flex;align-items:center;gap:6px;color:#666;cursor:pointer;font-size:15px}.action-btn:hover{color:#1890ff}.action-btn.liked{color:#ff4d4f !important}
.comment-section{background:#fff;border-radius:12px;padding:30px;box-shadow:0 1px 3px rgba(0,0,0,.1)}.comment-section h3{font-size:18px;margin-bottom:20px;color:#333}
.comment-input-box{display:flex;gap:10px;margin-bottom:20px}.comment-input-box input{flex:1;padding:12px 16px;border:1px solid #e8e8e8;border-radius:8px;font-size:14px}.comment-input-box button{padding:12px 24px;background:#1890ff;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:14px}
.comment-item{padding:15px 0;border-bottom:1px solid #f5f5f5}
.loading{display:flex;justify-content:center;align-items:center;padding:40px}
.loading-spinner{width:40px;height:40px;border:4px solid #e5e7eb;border-top:4px solid #4096ff;border-radius:50%;animation:spin 1s linear infinite}
@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}
</style>
