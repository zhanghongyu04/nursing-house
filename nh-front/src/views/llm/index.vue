<template>
  <div class="ai-chat-page" :class="{ 'dark': isDark }">
    <!-- 侧边栏 -->
    <ChatSidebar
      :sessions="sessions"
      :current-session-id="currentSessionId"
      :user-name="userName"
      :user-avatar="userAvatar"
      @new-chat="handleNewChat"
      @select-session="handleSelectSession"
      @rename-session="handleRenameSession"
      @delete-session="handleDeleteSession"
    />

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 消息容器 -->
      <div class="chat-shell">
        <div
          class="messages-container"
          :class="{ 'is-empty': messages.length === 0 && !isStreaming }"
          ref="messagesContainerRef"
        >
          <!-- 欢迎界面 -->
          <div class="welcome-screen" v-if="messages.length === 0 && !isStreaming">
            <div class="welcome-content">
              <div class="welcome-main">
                <div class="welcome-badge">
                  <SparklesIcon class="icon" />
                  康养智能助手
                </div>
                <h2>智慧康养业务智能工作台</h2>
                <p>面向政府监管、机构管理与护理执行场景，提供政策制度、机构运营、护理任务、护理日志、系统操作与内部实时数据查询的统一问答入口。支持上传 PDF、Word、图片等文件进行智能解析。</p>
                <div class="welcome-tags">
                  <span class="welcome-tag">监管政策解读</span>
                  <span class="welcome-tag">机构运营答疑</span>
                  <span class="welcome-tag">护理服务知识</span>
                  <span class="welcome-tag">护理任务查询</span>
                  <span class="welcome-tag">护理日志查询</span>
                  <span class="welcome-tag">系统操作说明</span>
                  <span class="welcome-tag">实时业务数据查询</span>
                </div>
                <div class="welcome-scenes">
                  <div class="scene-card">
                    <div class="scene-label">推荐提问方式</div>
                    <div class="scene-title">说明统计范围 + 指标 + 对象</div>
                    <div class="scene-desc">例如“查询当前授权范围床位使用率”“查询正常护理任务”或“查看异常护理任务和日志”。</div>
                  </div>
                  <div class="scene-card compact">
                    <div class="scene-label">当前数据权限</div>
                    <div class="scene-title">{{ dataAccessLabel }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="messages-list" v-else>
            <ChatMessage
              v-for="(message, index) in messages"
              :key="message.id || index"
              :message="message"
              :is-streaming="isStreaming && index === messages.length - 1"
              :show-sender="shouldShowSender(index)"
              @regenerate="handleRegenerate(index)"
            />

            <!-- 流式响应完成提示 -->
            <div class="streaming-complete" v-if="showCompleteHint">
              <CheckIcon class="icon" />
              响应完成
            </div>
          </div>
        </div>

        <!-- 滚动到底部按钮 -->
        <button
          class="scroll-to-bottom"
          v-if="showScrollButton"
          @click="scrollToBottom"
        >
          <ChevronDownIcon class="icon" />
        </button>

        <div class="data-capability-bar">
          <div class="capability-summary">
            <div class="capability-badge">
              <SparklesIcon class="icon" />
              已接入系统数据查询
            </div>
            <p>{{ capabilityDescription }}</p>
          </div>
          <div class="capability-actions">
            <button
              v-for="item in capabilityPrompts"
              :key="item.label"
              class="capability-chip"
              @click="handleQuickPrompt(item.prompt)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <!-- 输入区域 -->
        <ChatInputArea
          v-model="inputContent"
          :files="selectedFiles"
          :is-streaming="isStreaming"
          :placeholder="inputPlaceholder"
          @send="handleSendMessage"
          @stop-generation="handleStopGeneration"
          @add-files="handleAddFiles"
          @remove-file="handleRemoveFile"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useDark, useToggle } from '@vueuse/core'
import {
  SparklesIcon,
  DocumentTextIcon,
  QuestionMarkCircleIcon,
  LightBulbIcon,
  ChartBarIcon,
  BuildingOffice2Icon,
  ChevronDownIcon,
  CheckIcon
} from '@heroicons/vue/24/outline'
import ChatSidebar from '@/components/ChatSidebar.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInputArea from '@/components/ChatInputArea.vue'
import { chatAPI } from '@/api/agent/api.js'
import { useUserStore } from '@/stores/userStore'

const isDark = useDark()
const toggleDark = useToggle(isDark)
const userStore = useUserStore()

// 状态
const currentSessionId = ref(null)
const sessions = ref([])
const messages = ref([])
const inputContent = ref('')
const selectedFiles = ref([])
const isStreaming = ref(false)
const showCompleteHint = ref(false)
const streamingMessageId = ref(null)
const STREAM_RENDER_INTERVAL_MS = 55
const STREAM_RENDER_CHUNK_SIZE = 4

// 引用
const messagesContainerRef = ref(null)
let abortController = null
let streamRenderTimer = null
let pendingStreamContent = ''
let renderedStreamContent = ''

// 前端本地消息主键，仅用于渲染和流式更新定位，不参与后端持久化。
const generateMessageId = () => {
  return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`
}

// 计算属性
const userName = computed(() => userStore.userInfo?.username || '')
const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const hasGlobalSanaScope = computed(() => {
  return !userStore.userInfo?.sanaId && (userStore.userInfo?.sanaScopeIds || []).length === 0
})
const roleLabels = computed(() => userStore.userInfo?.roleLabels || [])
const isNurseUser = computed(() => roleLabels.value.includes('NURSE'))
const isOrgManagerUser = computed(() => roleLabels.value.includes('ORG_ADMIN') || roleLabels.value.includes('PARENT_ORG_ADMIN'))
const hasMultiSanaScope = computed(() => (userStore.userInfo?.sanaScopeIds || []).length > 1)
const orgScopeText = computed(() => hasMultiSanaScope.value ? '当前授权机构范围' : '当前机构')
const dataAccessLabel = computed(() => {
  if (hasGlobalSanaScope.value) {
    return '可查询全局与指定机构数据'
  }
  return hasMultiSanaScope.value ? '可查询当前授权的多机构数据' : '仅可查询当前机构数据'
})
const capabilityDescription = computed(() => {
  return hasGlobalSanaScope.value
    ? '支持上传 PDF/Word 文档进行智能解析，可查询系统概览、区域机构分布、指定机构详情和老人自理能力分布。机构侧账号可进一步查询护理任务和护理日志。'
    : `支持上传 PDF/Word 文档进行智能解析，可查询${orgScopeText.value}床位使用率、机构详情、老人自理能力分布、护理任务和护理日志。`
})
const capabilityPrompts = computed(() => {
  if (hasGlobalSanaScope.value) {
    return [
      {
        label: '系统概览统计',
        prompt: '请查询当前系统概览统计，包括老人数量、机构数量、护理人员数量、医护人员数量、床位总数、已用床位数和床位使用率。',
        icon: ChartBarIcon
      },
      {
        label: '区域机构分布',
        prompt: '请查询当前各区县养老机构数量分布，并按区域列出结果。',
        icon: BuildingOffice2Icon
      },
      {
        label: '指定机构详情',
        prompt: '请查询德州市社会福利院的机构详情统计，包括老人数量、护理人员数量、医护人员数量、床位总数、已用床位数和床位使用率。',
        icon: DocumentTextIcon
      },
      {
        label: '老人能力分布',
        prompt: '请查询全局老人自理能力分布，分别列出能力完好、轻度失能、中度失能、重度失能、完全失能人数。',
        icon: LightBulbIcon
      }
    ]
  }

  const prompts = [
    {
      label: '本机构概览',
      prompt: `请查询${orgScopeText.value}概览统计，包括老人数量、护理人员数量、医护人员数量、床位总数、已用床位数和床位使用率。`,
      icon: ChartBarIcon
    },
    {
      label: '床位使用率',
      prompt: `请查询${orgScopeText.value}床位总数、已用床位数和床位使用率，并说明统计范围。`,
      icon: BuildingOffice2Icon
    },
    {
      label: '机构详情',
      prompt: `请查询${orgScopeText.value}详情统计，包括所属区划、地址、运营状态、老人数量、护理人员数量、医护人员数量和床位使用率。`,
      icon: DocumentTextIcon
    },
    {
      label: '老人能力分布',
      prompt: `请查询${orgScopeText.value}老人自理能力分布，分别列出能力完好、轻度失能、中度失能、重度失能、完全失能人数。`,
      icon: QuestionMarkCircleIcon
    }
  ]

  if (isNurseUser.value && !isOrgManagerUser.value) {
    prompts.push(
      {
        label: '我的护理任务',
        prompt: '请查询我的正常护理任务列表，正常任务按待执行和执行中统计，列出任务状态、老人、任务标题、计划开始时间和计划结束时间。',
        icon: ChartBarIcon
      },
      {
        label: '我的普通日志',
        prompt: '请查询我的普通护理日志，列出老人、关联任务、日志时间、是否异常和日志内容。',
        icon: DocumentTextIcon
      },
      {
        label: '异常任务日志',
        prompt: '请查询我的异常护理任务和异常护理日志；异常任务按已超时或逾期统计，异常日志按异常标记统计，并分别列出关键记录。',
        icon: DocumentTextIcon
      }
    )
  } else {
    prompts.push(
      {
        label: '正常护理任务',
        prompt: `请查询${orgScopeText.value}正常护理任务列表，正常任务按待执行和执行中统计，列出任务状态、老人、执行人、计划开始时间和计划结束时间。`,
        icon: ChartBarIcon
      },
      {
        label: '普通护理日志',
        prompt: `请查询${orgScopeText.value}普通护理日志，列出老人、关联任务、护理人员、日志时间和日志内容。`,
        icon: DocumentTextIcon
      },
      {
        label: '异常任务日志',
        prompt: `请查询${orgScopeText.value}异常护理任务和异常护理日志；异常任务按已超时或逾期统计，异常日志按异常标记统计，并分别列出关键记录。`,
        icon: DocumentTextIcon
      }
    )
  }

  return prompts
})
const inputPlaceholder = computed(() => {
  const baseText = hasGlobalSanaScope.value
    ? '查询系统概览、机构分布或上传文档分析'
    : `查询${orgScopeText.value}床位使用率、护理任务、护理日志或上传文档分析`
  return `${baseText}（支持上传 PDF、Word、图片等文件）`
})
const currentSessionTitle = computed(() => {
  if (messages.value.length === 0) return '新对话'
  const currentSession = sessions.value.find(s => s.id === currentSessionId.value)
  return currentSession?.title || '新对话'
})

const showScrollButton = ref(false)

// 生命周期
onMounted(() => {
  loadSessions()
  setupScrollListener()
})

onUnmounted(() => {
  stopStreamRender()
  removeScrollListener()
})

// 方法
const loadSessions = async () => {
  try {
    const history = await chatAPI.getChatHistory()
    sessions.value = history || []
    if (sessions.value.length > 0) {
      await handleSelectSession(sessions.value[0].id)
    }
  } catch (error) {
    console.error('加载会话历史失败:', error)
  }
}

const handleNewChat = async (sessionType = 'chat') => {
  try {
    const newSession = await chatAPI.createSession('新对话', sessionType)
    if (newSession) {
      sessions.value.unshift(newSession)
      await handleSelectSession(newSession.id)
    }
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

const handleSelectSession = async (sessionId) => {
  resetStreamRenderState()
  currentSessionId.value = sessionId
  try {
    const chatMessages = await chatAPI.getChatMessages(sessionId, 'chat')
    messages.value = chatMessages

    // 更新会话的消息数量
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.messageCount = chatMessages.length
    }

    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载会话消息失败:', error)
    messages.value = []
  }
}

const handleRenameSession = async (sessionId, newTitle) => {
  try {
    const success = await chatAPI.updateSessionTitle(sessionId, newTitle)
    if (success) {
      const session = sessions.value.find(s => s.id === sessionId)
      if (session) {
        session.title = newTitle
      }
    }
  } catch (error) {
    console.error('重命名会话失败:', error)
  }
}

const handleDeleteSession = async (sessionId) => {
  try {
    const success = await chatAPI.deleteSession(sessionId)
    if (success) {
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await handleSelectSession(sessions.value[0].id)
        } else {
          currentSessionId.value = null
          messages.value = []
        }
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
  }
}

const handleSendMessage = async ({ content, files }) => {
  if (!content.trim() && files.length === 0) return
  if (isStreaming.value) return

  // 文档类附件会走知识问答/文档解析链路，媒体类附件走多模态分析链路。
  const documentFiles = files.filter(f => {
    const ext = f.name.split('.').pop()?.toLowerCase() || ''
    return ['pdf', 'doc', 'docx', 'txt'].includes(ext)
  })

  const mediaFiles = files.filter(f => {
    const ext = f.name.split('.').pop()?.toLowerCase() || ''
    return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'mp3', 'wav', 'ogg', 'mp4', 'webm', 'mov'].includes(ext)
  })

  // 首条消息自动确定会话类型，保证后端能按不同策略加载历史和附件处理流程。
  if (!currentSessionId.value) {
    await handleNewChat(documentFiles.length > 0 ? 'pdf' : 'chat')
  }

  // 没有显式文本时，前端补一条默认提示词，避免纯附件消息在后端落空。
  let messageContent = content?.trim() || ''
  if (!messageContent) {
    if (documentFiles.length > 0) {
      messageContent = '请帮我分析上传的文档内容。'
    } else if (mediaFiles.length > 0) {
      messageContent = '请帮我分析上传的图片/音视频内容。'
    }
  }

  // 添加用户消息
  const userMessage = {
    id: generateMessageId(),
    role: 'user',
    content: messageContent,
    timestamp: new Date(),
    files: files,
    attachments: files.map(file => {
      const ext = file.name?.split('.').pop()?.toUpperCase() || 'FILE'
      return {
        name: file.name,
        fileType: ext
      }
    })
  }
  messages.value.push(userMessage)

  // 清空输入
  inputContent.value = ''
  selectedFiles.value = []

  // 如果是首条消息，更新会话标题
  if (messages.value.filter(m => m.role === 'user').length === 1 && content) {
    const title = content.slice(0, 30) + (content.length > 30 ? '...' : '')
    await handleRenameSession(currentSessionId.value, title)
  }

  await nextTick()
  scrollToBottom()

  // 创建AbortController
  abortController = new AbortController()

  // 先插入一个助手占位消息，后续流式分片直接原位覆盖，避免列表闪烁。
  const assistantId = generateMessageId()
  streamingMessageId.value = assistantId
  const assistantMessage = {
    id: assistantId,
    role: 'assistant',
    content: '',
    retrieval: null,
    timestamp: new Date()
  }
  messages.value.push(assistantMessage)
  isStreaming.value = true
  showCompleteHint.value = false
  resetStreamRenderState()

  try {
    // 准备表单数据
    const formData = new FormData()
    // 始终发送 prompt 参数（即使是空字符串）
    formData.append('prompt', messageContent || '')
    files.forEach(file => {
      formData.append('files', file)
    })

    // 后端返回的是可读流，前端按分片逐步写回到占位消息中。
    const response = await chatAPI.sendMessage(formData, currentSessionId.value)
    const reader = response.reader
    const decoder = new TextDecoder('utf-8')

    const metaIndex = messages.value.findIndex(m => m.id === streamingMessageId.value)
    if (metaIndex !== -1) {
      messages.value.splice(metaIndex, 1, {
        ...messages.value[metaIndex],
        retrieval: response.retrieval || null
      })
    }

    while (true) {
      if (abortController?.signal.aborted) {
        break
      }

      try {
        const { value, done } = await reader.read()
        if (done) {
          const finalChunk = decoder.decode()
          if (finalChunk) {
            queueStreamContent(finalChunk)
          }
          break
        }

        const chunk = decoder.decode(value, { stream: true })
        if (!chunk) {
          continue
        }
        queueStreamContent(chunk)
      } catch (readError) {
        if (abortController?.signal.aborted) {
          break
        }
        console.error('读取流错误:', readError)
        break
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    resetStreamRenderState()
    const msgIndex = messages.value.findIndex(m => m.id === streamingMessageId.value)
    if (msgIndex !== -1) {
      messages.value.splice(msgIndex, 1, {
        ...messages.value[msgIndex],
        content: '抱歉，发生了错误，请稍后重试。'
      })
    }
  } finally {
    await drainPendingStreamContent()
    isStreaming.value = false
    streamingMessageId.value = null
    abortController = null
    showCompleteHint.value = true
    setTimeout(() => {
      showCompleteHint.value = false
    }, 3000)
    await scrollToBottom()

    // 更新会话的消息数量
    const session = sessions.value.find(s => s.id === currentSessionId.value)
    if (session) {
      session.messageCount = messages.value.length
    }
  }
}

const handleStopGeneration = () => {
  if (abortController) {
    abortController.abort()
  }
}

const syncStreamingMessageContent = async (content) => {
  const msgIndex = messages.value.findIndex(m => m.id === streamingMessageId.value)
  if (msgIndex === -1) {
    return
  }
  messages.value.splice(msgIndex, 1, {
    ...messages.value[msgIndex],
    content
  })
}

const stopStreamRender = () => {
  if (streamRenderTimer) {
    clearTimeout(streamRenderTimer)
    streamRenderTimer = null
  }
}

const resetStreamRenderState = () => {
  stopStreamRender()
  pendingStreamContent = ''
  renderedStreamContent = ''
}

const scheduleStreamRender = () => {
  if (streamRenderTimer) {
    return
  }

  const tick = async () => {
    streamRenderTimer = null

    if (!streamingMessageId.value) {
      resetStreamRenderState()
      return
    }

    // 将大块响应切成小片段回填，降低长文本一次性刷新的跳动感。
    if (pendingStreamContent.length > 0) {
      const nextChunk = pendingStreamContent.slice(0, STREAM_RENDER_CHUNK_SIZE)
      pendingStreamContent = pendingStreamContent.slice(STREAM_RENDER_CHUNK_SIZE)
      renderedStreamContent += nextChunk
      await syncStreamingMessageContent(renderedStreamContent)
      await scrollToBottom()
    }

    if (pendingStreamContent.length > 0) {
      streamRenderTimer = setTimeout(tick, STREAM_RENDER_INTERVAL_MS)
    }
  }

  streamRenderTimer = setTimeout(tick, STREAM_RENDER_INTERVAL_MS)
}

const queueStreamContent = (chunk) => {
  pendingStreamContent += chunk
  scheduleStreamRender()
}

const flushPendingStreamContent = async () => {
  if (pendingStreamContent.length > 0) {
    renderedStreamContent += pendingStreamContent
    pendingStreamContent = ''
  }
  if (renderedStreamContent) {
    await syncStreamingMessageContent(renderedStreamContent)
    await scrollToBottom()
  }
  stopStreamRender()
}

const drainPendingStreamContent = async () => {
  // 结束时等待缓冲区全部刷完，避免最后一小段内容丢失。
  while (pendingStreamContent.length > 0 || streamRenderTimer) {
    await new Promise(resolve => setTimeout(resolve, STREAM_RENDER_INTERVAL_MS))
  }
  if (renderedStreamContent) {
    await syncStreamingMessageContent(renderedStreamContent)
    await scrollToBottom()
  }
  stopStreamRender()
}

const handleRegenerate = async (index) => {
  // 找到上一条用户消息并重新发送
  const userMessageIndex = messages.value.findIndex((m, i) => i < index && m.role === 'user')
  if (userMessageIndex !== -1) {
    const userMessage = messages.value[userMessageIndex]
    // 删除从用户消息之后的所有消息
    messages.value = messages.value.slice(0, userMessageIndex + 1)
    // 重新发送
    await handleSendMessage({
      content: userMessage.content,
      files: userMessage.files || []
    })
  }
}

const handleClearContext = () => {
  if (confirm('确定要清空当前对话上下文吗？')) {
    messages.value = []
  }
}

const handleQuickPrompt = (prompt) => {
  inputContent.value = prompt
}

const handleAddFiles = (files) => {
  selectedFiles.value.push(...files)
}

const handleRemoveFile = (index) => {
  selectedFiles.value = selectedFiles.value.filter((_, i) => i !== index)
}

const shouldShowSender = (index) => {
  if (index === 0) return true
  const currentMessage = messages.value[index]
  const previousMessage = messages.value[index - 1]
  return currentMessage.role !== previousMessage.role
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTo({
      top: messagesContainerRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const setupScrollListener = () => {
  const container = messagesContainerRef.value
  if (container) {
    container.addEventListener('scroll', handleScroll)
  }
}

const removeScrollListener = () => {
  const container = messagesContainerRef.value
  if (container) {
    container.removeEventListener('scroll', handleScroll)
  }
}

const handleScroll = () => {
  const container = messagesContainerRef.value
  if (container) {
    const { scrollTop, scrollHeight, clientHeight } = container
    showScrollButton.value = scrollHeight - scrollTop - clientHeight > 200
  }
}
</script>

<style scoped lang="scss">
// ChatGPT 风格布局
.ai-chat-page {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  background: #ffffff;
  overflow: hidden;

  &.dark {
    background: #1a1d23;
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #ffffff;
}

.chat-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 0, 0, 0.3);
  }

  .dark &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
  }

  .dark &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.3);
  }
}

// 欢迎界面 - ChatGPT 风格
.welcome-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 2rem 1.5rem;
  overflow-y: auto;
  overflow-x: visible;

  .welcome-content {
    width: 100%;
    max-width: 860px;
    margin: 0 auto;
    padding: 0;
    background: transparent;
  }

  .welcome-main {
    text-align: center;
  }

  .welcome-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    margin: 0 auto 1.5rem;
    background: linear-gradient(135deg, #2f6fd6 0%, #1f4f9c 100%);
    color: white;
    font-size: 0.875rem;
    font-weight: 500;
    border-radius: 999px;
    box-shadow: 0 2px 8px rgba(47, 111, 214, 0.25);

    .icon {
      width: 1rem;
      height: 1rem;
    }
  }

  h2 {
    font-size: 1.9rem;
    font-weight: 600;
    color: #111827;
    margin: 0 0 1rem;
    letter-spacing: -0.025em;
  }

  p {
    font-size: 0.975rem;
    color: #6b7280;
    margin: 0 0 2rem;
    max-width: 540px;
    margin-left: auto;
    margin-right: auto;
  }

  .welcome-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    justify-content: center;
    margin-bottom: 2rem;
  }

  .welcome-tag {
    padding: 0.375rem 0.75rem;
    background: rgba(47, 111, 214, 0.1);
    color: #2f6fd6;
    font-size: 0.875rem;
    border-radius: 999px;
    font-weight: 500;
  }

  .welcome-scenes {
    display: flex;
    gap: 0.75rem;
    margin-top: 1rem;
  }

  .scene-card {
    flex: 1;
    padding: 1rem;
    background: linear-gradient(135deg, #f7faff 0%, #f1f6fd 100%);
    border: 1px solid rgba(47, 111, 214, 0.12);
    border-radius: 0.75rem;
    text-align: left;

    &.compact {
      flex: 0 0 auto;
    }

    .scene-label {
      font-size: 0.75rem;
      font-weight: 600;
      color: #2f6fd6;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 0.375rem;
    }

    .scene-title {
      font-size: 0.9375rem;
      font-weight: 600;
      color: #1a2744;
      margin-bottom: 0.25rem;
    }

    .scene-desc {
      font-size: 0.875rem;
      color: #4b5563;
      line-height: 1.4;
    }
  }
}

// 消息列表
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: min(980px, 100%);
  margin: 0 auto;
  padding: 0 1rem 1rem;
  min-height: auto;
}

// 流式响应完成提示
.streaming-complete {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  font-size: 0.875rem;
  color: #2f6fd6;
  opacity: 0;
  animation: fadeIn 0.3s ease-out forwards;

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .icon {
    width: 1rem;
    height: 1rem;
  }
}

// 滚动到底部按钮
.scroll-to-bottom {
  position: absolute;
  bottom: 5.5rem;
  left: 50%;
  transform: translateX(-50%);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: white;
  color: #374151;
  cursor: pointer;
  border-radius: 999px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  border: 1px solid #e5e7eb;
  transition: all 0.25s ease;
  z-index: 10;

  &:hover {
    background: #f9fafb;
    transform: translateX(-50%) translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }

  &:active {
    transform: translateX(-50%);
  }

  .icon {
    width: 1.125rem;
    height: 1.125rem;
  }
}

.data-capability-bar {
  width: min(1120px, calc(100% - 2rem));
  margin: 0 auto 0.5rem;
  padding: 0.875rem 1rem;
  border: 1px solid rgba(47, 111, 214, 0.12);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(247, 250, 255, 0.96) 0%, rgba(241, 246, 253, 0.98) 100%);
  box-shadow: 0 8px 18px rgba(19, 46, 84, 0.05);
}

.capability-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;

  p {
    margin: 0;
    color: #53657d;
    font-size: 0.875rem;
    line-height: 1.6;
  }
}

.capability-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  flex-shrink: 0;
  padding: 0.4rem 0.7rem;
  border-radius: 999px;
  background: rgba(47, 111, 214, 0.1);
  color: #2f6fd6;
  font-size: 0.8125rem;
  font-weight: 600;

  .icon {
    width: 0.95rem;
    height: 0.95rem;
  }
}

.capability-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.capability-chip {
  padding: 0.5rem 0.8rem;
  border: 1px solid rgba(47, 111, 214, 0.16);
  border-radius: 999px;
  background: #ffffff;
  color: #294769;
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #2f6fd6;
    color: #1f4f9c;
    background: #f7faff;
    transform: translateY(-1px);
  }
}

// 响应式
@media (max-width: 1180px) {
  .welcome-screen {
    padding: 1.75rem 1.25rem;

    .welcome-content {
      max-width: 720px;
    }

    h2 {
      font-size: 1.75rem;
    }
  }
}

@media (max-width: 980px) {
  .welcome-screen {
    padding: 1.5rem 1rem;

    .welcome-content {
      max-width: 100%;
    }

    .welcome-tags {
      margin-bottom: 1.5rem;
    }

    .welcome-scenes {
      flex-direction: column;
    }

    .scene-card.compact {
      flex: 1 1 auto;
    }
  }

  .data-capability-bar {
    width: calc(100% - 1.5rem);
    padding: 0.75rem 0.875rem;
  }

  .capability-summary {
    flex-direction: column;
    margin-bottom: 0.625rem;
  }
}

@media (max-width: 768px) {
  .ai-chat-page {
    flex: 1;
  }

  .chat-main {
    min-width: 0;
  }

  .messages-list {
    width: 100%;
    padding: 0 0.75rem 0.875rem;
  }

  .welcome-screen {
    padding: 1.25rem 0.875rem;
    align-items: flex-start;

    .welcome-content {
      padding: 0;
    }

    h2 {
      font-size: 1.5rem;
    }

    p {
      font-size: 0.9375rem;
    }

  }

  .scroll-to-bottom {
    bottom: 4.75rem;
    width: 2rem;
    height: 2rem;
  }

  .data-capability-bar {
    width: calc(100% - 1rem);
    margin-bottom: 0.375rem;
    border-radius: 14px;
  }
}

@media (max-height: 760px) {
  .ai-chat-page {
    flex: 1;
  }

  .welcome-screen {
    padding: 1rem 1.5rem;
  }
}

@media (max-width: 560px) {
  .welcome-screen {
    .welcome-badge {
      font-size: 0.8125rem;
      padding: 0.45rem 0.875rem;
    }

    h2 {
      font-size: 1.35rem;
    }

    p {
      font-size: 0.875rem;
      margin-bottom: 1.5rem;
    }

    .welcome-tag {
      font-size: 0.8125rem;
    }

    .scene-card {
      padding: 0.875rem;
    }
  }
}
</style>
