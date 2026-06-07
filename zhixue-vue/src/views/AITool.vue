<template>
  <div>
    <div class="page-title">
      <h1>AI数理答疑工具</h1>
      <p style="color: #666; margin-top: 8px;">输入你的问题，AI为你提供解题思路和知识点解析</p>
    </div>

    <div class="chat-container">
      <div class="chat-box" ref="chatBox">
        <div class="message ai-msg">
          <div class="content">你好！我是智学助手，有什么高数、线代、离散数学的问题都可以问我~</div>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" class="message" :class="msg.role === 'user' ? 'user-msg' : 'ai-msg'">
          <div class="content">{{ msg.content }}</div>
        </div>
      </div>
      <div class="input-area">
        <input v-model="userInput" placeholder="输入你的问题，比如：怎么求函数的导数？" @keyup.enter="sendMessage">
        <button class="send-btn" @click="sendMessage" :disabled="sending">{{ sending ? '思考中...' : '发送' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const API_KEY = "sk-7fd0821da80e491e986bbed23829bb76"
const API_URL = "https://api.deepseek.com/v1/chat/completions"

const chatBox = ref(null)
const userInput = ref('')
const sending = ref(false)
const messages = ref([])

const scrollToBottom = async () => {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

const sendMessage = async () => {
  const text = userInput.value.trim()
  if (!text || sending.value) return

  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  await scrollToBottom()

  sending.value = true
  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      },
      body: JSON.stringify({
        model: 'deepseek-chat',
        messages: [
          { role: 'system', content: '你是一个擅长数理化的AI助手，回答简洁准确。' },
          { role: 'user', content: text }
        ],
        stream: false
      })
    })
    const data = await res.json()
    const reply = data.choices[0].message.content
    messages.value.push({ role: 'ai', content: reply })
  } catch (err) {
    messages.value.push({ role: 'ai', content: 'AI 服务暂时不可用' })
    console.error(err)
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}
</script>

<style scoped>
.page-title {
  max-width: 1000px;
  margin: 30px auto 0;
  padding: 0 20px;
  color: #2c3e50;
  text-align: center;
}
.chat-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
}
.chat-box {
  background: white;
  border-radius: 10px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.08);
  height: 450px;
  padding: 20px;
  overflow-y: auto;
  margin-bottom: 20px;
}
.message {
  margin-bottom: 15px;
}
.user-msg {
  text-align: right;
}
.user-msg .content {
  display: inline-block;
  background: linear-gradient(135deg,#4096ff,#2168e3);
  color: white;
  padding: 10px 15px;
  border-radius: 15px;
  max-width: 70%;
}
.ai-msg {
  text-align: left;
}
.ai-msg .content {
  display: inline-block;
  background: #f0f0f0;
  color: #333;
  padding: 10px 15px;
  border-radius: 15px;
  max-width: 70%;
}
.input-area {
  display: flex;
  gap: 10px;
}
.input-area input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}
.input-area input:focus {
  outline: none;
  border-color: #4096ff;
}
.send-btn {
  padding: 12px 25px;
  background: linear-gradient(135deg,#4096ff,#2168e3);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  font-size: 14px;
}
.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
