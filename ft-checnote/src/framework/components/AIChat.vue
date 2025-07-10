<template>
  <div class="ai-chat-container">
    <div class="chat-messages" ref="messageContainer">
      <div v-for="(message, index) in messages" :key="index" :class="['message', message.role]">
        <div class="message-content">
          <div class="avatar">
            {{ message.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="text" v-if="message.role === 'user'">{{ message.content }}</div>
          <div class="text" v-else>{{ message.role === 'assistant' ? displayedContent[index] || '' : message.content }}</div>
        </div>
      </div>
    </div>
    
    <div class="input-area">
      <el-input
        v-model="userInput"
        type="textarea"
        :rows="3"
        placeholder="请输入您的问题..."
        @keyup.enter.ctrl="sendMessage"
      />
      <el-button type="primary" @click="sendMessage" :loading="loading">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import {nextTick, onMounted, ref} from 'vue'
import axios from 'axios'

const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const messageContainer = ref(null)
const displayedContent = ref({})

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const typeMessage = async (messageIndex, content) => {
  displayedContent.value[messageIndex] = ''
  const delay = 50 // 每个字符的延迟时间（毫秒）
  
  for (let i = 0; i < content.length; i++) {
    await new Promise(resolve => setTimeout(resolve, delay))
    displayedContent.value[messageIndex] = content.slice(0, i + 1)
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return
  
  const userMessage = userInput.value.trim()
  const messageIndex = messages.value.length
  messages.value.push({ role: 'user', content: userMessage })
  userInput.value = ''
  scrollToBottom()
  
  loading.value = true
  try {
    const response = await axios.post('http://localhost:14231/chat', {
      message: userMessage
    }, {
      responseType: 'stream',
      onDownloadProgress: (progressEvent) => {
        const chunk = progressEvent.event.target.response
        if (chunk) {
          // 确保消息存在
          if (!messages.value[messageIndex + 1]) {
            messages.value.push({
              role: 'assistant',
              content: ''
            })
            displayedContent.value[messageIndex + 1] = ''
          }
          // 直接追加新的文本块
          messages.value[messageIndex + 1].content += chunk
          displayedContent.value[messageIndex + 1] = messages.value[messageIndex + 1].content
          scrollToBottom()
        }
      }
    })
  } catch (error) {
    const errorMessage = '抱歉，服务出现错误，请稍后再试。'
    messages.value.push({
      role: 'assistant',
      content: errorMessage
    })
    displayedContent.value[messageIndex + 1] = errorMessage
    console.error('Error:', error)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  const welcomeMessage = '你好！我是AI助手，请问有什么我可以帮你的吗？'
  messages.value = [{
    role: 'assistant',
    content: welcomeMessage
  }]
  typeMessage(0, welcomeMessage)
})
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 20px;
  box-sizing: border-box;
  background-color: #f5f5f5;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  margin-bottom: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.message {
  margin-bottom: 20px;
}

.message-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.avatar {
  font-size: 24px;
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 50%;
}

.text {
  padding: 12px 16px;
  border-radius: 8px;
  max-width: 80%;
  word-break: break-word;
  line-height: 1.5;
}

.user .text {
  background-color: #e3f2fd;
}

.assistant .text {
  background-color: #f5f5f5;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.input-area .el-input {
  flex: 1;
}

.el-button {
  height: 100px;
}
</style>