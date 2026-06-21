<template>
  <div class="chat-sidebar">
    <!-- 顶部区域 -->
    <div class="sidebar-header">
      <button class="new-chat-btn" @click="handleNewChat">
        <PlusIcon class="icon" />
        <span>新对话</span>
      </button>
    </div>

    <!-- 搜索框 -->
    <div class="sidebar-search">
      <div class="search-input-wrapper">
        <MagnifyingGlassIcon class="search-icon" />
        <input
          type="text"
          v-model="searchQuery"
          placeholder="搜索会话..."
          class="search-input"
        />
        <XMarkIcon v-if="searchQuery" @click="searchQuery = ''" class="clear-icon" />
      </div>
    </div>

    <!-- 统一会话入口 -->
    <div class="session-type-tabs">
      <div class="type-tab active single-tab">
        <ChatBubbleLeftRightIcon class="icon" />
        对话记录
      </div>
    </div>

    <!-- 会话列表 -->
    <div class="sidebar-content">
      <div class="sessions-list" v-if="filteredSessions.length > 0">
        <div
          v-for="group in groupedSessions"
          :key="group.key"
          class="session-group"
        >
          <div class="group-header" v-if="group.key !== 'all'">
            {{ group.label }}
          </div>
          <div
            v-for="session in group.sessions"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === currentSessionId }"
            @click="handleSelectSession(session)"
          >
            <div class="session-icon">
              <ChatBubbleLeftRightIcon class="icon" />
            </div>
            <div class="session-info">
              <div class="session-title">{{ session.title || '新对话' }}</div>
              <div class="session-meta">
                <span class="session-time">{{ formatTime(session.createTime) }}</span>
                <span class="session-count">{{ session.messageCount || 0 }} 条消息</span>
              </div>
            </div>
            <div class="session-actions">
              <button class="action-btn" @click.stop="handleRename(session)" title="重命名">
                <PencilIcon class="icon" />
              </button>
              <button class="action-btn danger" @click.stop="handleDelete(session)" title="删除">
                <TrashIcon class="icon" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <ChatBubbleLeftRightIcon class="empty-icon" />
        <p>暂无会话记录</p>
        <button class="create-first-btn" @click="handleNewChat">
          开始第一次对话
        </button>
      </div>
    </div>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <div class="user-info">
        <div class="user-avatar">
          <img
            v-if="showAvatarImage"
            :src="avatarSrc"
            alt="用户头像"
            class="avatar-image"
            @error="handleAvatarError"
          />
          <UserIcon v-else class="icon" />
        </div>
        <div class="user-details">
          <div class="user-name">{{ userName || '用户' }}</div>
          <div class="user-status">在线</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  PlusIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
  ChatBubbleLeftRightIcon,
  PencilIcon,
  TrashIcon,
  UserIcon
} from '@heroicons/vue/24/outline'
import { buildImageProxySrc } from '@/api/file'

const props = defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  currentSessionId: {
    type: String,
    default: null
  },
  userName: {
    type: String,
    default: ''
  },
  userAvatar: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'new-chat',
  'select-session',
  'rename-session',
  'delete-session'
])

const searchQuery = ref('')
const avatarLoadFailed = ref(false)
const avatarSrc = computed(() => buildImageProxySrc(props.userAvatar || ''))
const showAvatarImage = computed(() => Boolean(avatarSrc.value) && !avatarLoadFailed.value)

const handleAvatarError = () => {
  avatarLoadFailed.value = true
}

watch(avatarSrc, () => {
  avatarLoadFailed.value = false
})

const filteredSessions = computed(() => {
  let sessions = props.sessions

  // 仅保留 chat / pdf 两类；兼容历史数据缺失 sessionType 的情况（按 chat 展示）
  sessions = sessions.filter(s => !s.sessionType || s.sessionType === 'chat' || s.sessionType === 'pdf')

  // 按搜索词过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    sessions = sessions.filter(s =>
      (s.title || '').toLowerCase().includes(query)
    )
  }

  return sessions
})

const groupedSessions = computed(() => {
  const groups = []
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)

  const todaySessions = []
  const yesterdaySessions = []
  const weekSessions = []
  const olderSessions = []

  filteredSessions.value.forEach(session => {
    const date = new Date(session.createTime)
    if (date >= today) {
      todaySessions.push(session)
    } else if (date >= yesterday) {
      yesterdaySessions.push(session)
    } else if (date >= weekAgo) {
      weekSessions.push(session)
    } else {
      olderSessions.push(session)
    }
  })

  if (todaySessions.length > 0) {
    groups.push({ key: 'today', label: '今天', sessions: todaySessions })
  }
  if (yesterdaySessions.length > 0) {
    groups.push({ key: 'yesterday', label: '昨天', sessions: yesterdaySessions })
  }
  if (weekSessions.length > 0) {
    groups.push({ key: 'week', label: '最近7天', sessions: weekSessions })
  }
  if (olderSessions.length > 0) {
    groups.push({ key: 'older', label: '更早', sessions: olderSessions })
  }

  if (groups.length === 0) {
    groups.push({ key: 'all', label: '', sessions: filteredSessions.value })
  }

  return groups
})

const handleNewChat = () => {
  emit('new-chat')
}

const handleSelectSession = (session) => {
  emit('select-session', session.id)
}

const handleRename = async (session) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话标题', '重命名会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: session.title || '新对话',
      inputPlaceholder: '请输入会话标题',
      inputValidator: (value) => {
        if (!value || !value.trim()) return '会话标题不能为空'
        if (value.trim().length > 50) return '会话标题不能超过50个字符'
        return true
      }
    })

    if (value && value.trim()) {
      emit('rename-session', session.id, value.trim())
    }
  } catch {
    // 用户取消时不处理
  }
}

const handleDelete = async (session) => {
  try {
    await ElMessageBox.confirm(`确定删除会话“${session.title || '新对话'}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    emit('delete-session', session.id)
  } catch {
    // 用户取消时不处理
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const normalized = typeof timestamp === 'string' && !timestamp.includes('T')
    ? timestamp.replace(' ', 'T')
    : timestamp
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped lang="scss">
.chat-sidebar {
  display: flex;
  flex-direction: column;
  width: 296px;
  min-width: 296px;
  height: 100%;
  background:
    linear-gradient(180deg, #f7faff 0%, #f1f6fd 100%);
  border-right: 1px solid rgba(26, 61, 114, 0.08);
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 0.875rem 0.75rem;
  border-bottom: 1px solid rgba(26, 61, 114, 0.08);
  background: rgba(255, 255, 255, 0.72);

  .new-chat-btn {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    min-width: 0;
    padding: 0.75rem 0.875rem;
    border: none;
    border-radius: 10px;
    background: #2f6fd6;
    color: white;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      background: #255fbc;
      box-shadow: 0 4px 10px rgba(47, 111, 214, 0.18);
    }

    .icon {
      width: 1.125rem;
      height: 1.125rem;
    }
  }
}

.sidebar-search {
  padding: 0.75rem 0.875rem;

  .search-input-wrapper {
    position: relative;
    display: flex;
    align-items: center;

    .search-icon {
      position: absolute;
      left: 0.75rem;
      width: 1rem;
      height: 1rem;
      color: #999;
    }

    .search-input {
      width: 100%;
      padding: 0.5rem 2rem 0.5rem 2.25rem;
      border: 1px solid rgba(26, 61, 114, 0.12);
      border-radius: 8px;
      font-size: 0.875rem;
      background: #fff;
      transition: all 0.2s;

      &:focus {
        outline: none;
        border-color: #2f6fd6;
      }

      &::placeholder {
        color: #999;
      }
    }

    .clear-icon {
      position: absolute;
      right: 0.5rem;
      width: 1rem;
      height: 1rem;
      color: #999;
      cursor: pointer;
      transition: color 0.2s;

      &:hover {
        color: #666;
      }
    }
  }
}

.session-type-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0 0.875rem 0.75rem;

  .type-tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.375rem;
    padding: 0.5rem 0.75rem;
    border: none;
    border-radius: 0.375rem;
    background: transparent;
    color: #666;
    font-size: 0.75rem;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
      color: #333;
    }

    &.active {
      background: rgba(47, 111, 214, 0.1);
      color: #2f6fd6;
    }

    .icon {
      width: 0.875rem;
      height: 0.875rem;
    }
  }
}

.sidebar-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 0.5rem 1rem;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 2px;
  }
}

.sessions-list {
  padding-top: 0.25rem;

  .session-group {
    margin-bottom: 0.5rem;
  }

  .group-header {
    padding: 0.5rem 0.5rem;
    font-size: 0.75rem;
    font-weight: 600;
    color: #7b8da6;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
}

.session-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    background: rgba(47, 111, 214, 0.06);

    .session-actions {
      opacity: 1;
    }
  }

  &.active {
    background: rgba(47, 111, 214, 0.1);
    border: 1px solid rgba(47, 111, 214, 0.12);

    .session-icon {
      background: #2f6fd6;
      color: white;
    }

    .session-title {
      color: #1f4f9c;
    }
  }

  .session-icon {
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #eaf1fb;
    border-radius: 0.5rem;
    color: #666;
    transition: all 0.2s;

    .icon {
      width: 1rem;
      height: 1rem;
    }
  }

  .session-info {
    flex: 1;
    min-width: 0;
  }

  .session-title {
    font-size: 0.875rem;
    font-weight: 500;
    color: #26384f;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .session-meta {
    display: flex;
    gap: 0.5rem;
    font-size: 0.75rem;
    color: #8a9ab0;
    margin-top: 0.125rem;
  }

  .session-actions {
    display: flex;
    gap: 0.125rem;
    opacity: 0;
    transition: opacity 0.2s;
  }

  .action-btn {
    width: 1.5rem;
    height: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    color: #999;
    cursor: pointer;
    border-radius: 0.25rem;
    transition: all 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.06);
      color: #666;
    }

    &.danger:hover {
      background: rgba(255, 77, 79, 0.1);
      color: #ff4d4f;
    }

    .icon {
      width: 0.875rem;
      height: 0.875rem;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;

  .empty-icon {
    width: 3rem;
    height: 3rem;
    color: #ddd;
    margin-bottom: 1rem;
  }

  p {
    font-size: 0.875rem;
    color: #999;
    margin-bottom: 1rem;
  }

  .create-first-btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 0.5rem;
    background: #007CF0;
    color: white;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #0066cc;
    }
  }
}

.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem;
  border-top: 1px solid rgba(26, 61, 114, 0.08);
  background: rgba(255, 255, 255, 0.68);

  .user-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .user-avatar {
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #2f6fd6;
    border-radius: 50%;
    color: white;
    overflow: hidden;

    .avatar-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .icon {
      width: 1rem;
      height: 1rem;
    }
  }

  .user-details {
    .user-name {
      font-size: 0.875rem;
      font-weight: 500;
      color: #333;
    }

    .user-status {
      font-size: 0.75rem;
      color: #4ade80;
      display: flex;
      align-items: center;
      gap: 0.25rem;

      &::before {
        content: '';
        width: 0.375rem;
        height: 0.375rem;
        background: #4ade80;
        border-radius: 50%;
      }
    }
  }
}

// 暗色模式
:deep(.dark) {
  .chat-sidebar {
    background: rgba(32, 37, 46, 0.98);
    border-right-color: rgba(255, 255, 255, 0.08);
  }

  .sidebar-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .sidebar-search .search-input-wrapper {
    .search-input {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);
      color: #fff;

      &:focus {
        background: rgba(255, 255, 255, 0.08);
        border-color: #007CF0;
      }

      &::placeholder {
        color: #666;
      }
    }

    .clear-icon {
      color: #666;

      &:hover {
        color: #999;
      }
    }
  }

  .session-type-tabs .type-tab {
    color: #999;

    &:hover {
      background: rgba(255, 255, 255, 0.05);
      color: #fff;
    }

    &.active {
      background: rgba(0, 124, 240, 0.2);
      color: #007CF0;
    }
  }

  .sessions-list .group-header {
    color: #666;
  }

  .session-item {
    &:hover {
      background: rgba(255, 255, 255, 0.05);
    }

    &.active {
      background: rgba(0, 124, 240, 0.2);

      .session-title {
        color: #007CF0;
      }
    }

    .session-icon {
      background: rgba(255, 255, 255, 0.1);
      color: #999;
    }

    .session-title {
      color: #e5e5e5;
    }

    .session-meta {
      color: #666;
    }

    .action-btn {
      color: #666;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #999;
      }
    }
  }

  .empty-state {
    .empty-icon {
      color: #444;
    }

    p {
      color: #666;
    }
  }

  .sidebar-footer {
    border-top-color: rgba(255, 255, 255, 0.08);

    .user-details .user-name {
      color: #e5e5e5;
    }
  }
}

@media (max-width: 1180px) {
  .chat-sidebar {
    width: 264px;
    min-width: 264px;
  }
}

@media (max-width: 980px) {
  .chat-sidebar {
    width: 228px;
    min-width: 228px;
  }

  .session-type-tabs {
    gap: 0.125rem;
    padding: 0 0.625rem 0.625rem;

    .type-tab {
      padding: 0.5rem 0.375rem;
      font-size: 0.6875rem;
    }
  }

  .sidebar-search,
  .sidebar-header,
  .sidebar-footer {
    padding-left: 0.625rem;
    padding-right: 0.625rem;
  }
}

@media (max-width: 768px) {
  .chat-sidebar {
    width: 88px;
    min-width: 88px;
  }

  .sidebar-header {
    padding: 0.75rem 0.5rem;

    .new-chat-btn {
      justify-content: center;
      padding: 0.75rem;

      span {
        display: none;
      }
    }
  }

  .sidebar-search {
    display: none;
  }

  .session-type-tabs {
    flex-direction: column;
    padding: 0 0.5rem 0.625rem;

    .type-tab {
      padding: 0.625rem 0;
      font-size: 0;

      .icon {
        width: 1rem;
        height: 1rem;
      }
    }
  }

  .sidebar-content {
    padding: 0 0.375rem 0.75rem;
  }

  .sessions-list .group-header {
    display: none;
  }

  .session-item {
    justify-content: center;
    padding: 0.75rem 0.375rem;

    .session-info,
    .session-actions {
      display: none;
    }

    .session-icon {
      margin: 0;
    }
  }

  .empty-state {
    padding: 1.75rem 0.5rem;

    p,
    .create-first-btn {
      display: none;
    }
  }

  .sidebar-footer {
    justify-content: center;
    padding: 0.75rem 0.5rem;

    .user-details {
      display: none;
    }
  }
}
</style>
