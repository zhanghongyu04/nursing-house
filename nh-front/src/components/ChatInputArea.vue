<template>
  <div class="chat-input-area">
    <!-- 文件预览 -->
    <div class="file-previews" v-if="files.length > 0">
      <div v-for="(file, index) in files" :key="index" class="file-preview">
        <div class="file-icon">
          <component :is="getFileIcon(file)" class="icon" />
        </div>
        <div class="file-info">
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ formatFileSize(file.size) }}</span>
        </div>
        <button class="remove-file-btn" @click="$emit('remove-file', index)">
          <XMarkIcon class="icon" />
        </button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container" :class="{ 'focused': isFocused }">
      <div class="input-actions-left">
        <input
          ref="fileInputRef"
          type="file"
          @change="handleFileSelect"
          accept="image/*,audio/*,video/*,.pdf,.doc,.docx,.txt"
          multiple
          class="hidden-input"
        >
        <button
          class="action-btn"
          @click="fileInputRef?.click()"
          title="上传文件"
          :disabled="disabled"
        >
          <PaperClipIcon class="icon" />
        </button>
      </div>

      <div class="input-wrapper" @click="focusTextarea">
        <textarea
          ref="textareaRef"
          v-model="inputValue"
          @keydown="handleKeydown"
          @focus="isFocused = true"
          @blur="isFocused = false"
          @input="handleInput"
          :placeholder="placeholder"
          :disabled="disabled"
          :rows="rows"
          class="chat-textarea"
        ></textarea>
      </div>

      <div class="input-actions-right">
      <!-- 停止生成按钮 -->
        <button
          v-if="isStreaming"
          class="action-btn stop-btn"
          @click="$emit('stop-generation')"
          title="停止生成"
        >
          <StopIcon class="icon" />
        </button>

        <!-- 发送按钮 -->
        <button
          v-else
          class="send-btn"
          @click="handleSend"
          :disabled="!canSend"
          title="发送"
        >
          <PaperAirplaneIcon class="icon" />
        </button>
      </div>
    </div>

    <!-- 提示信息 -->
    <div class="input-hints" v-if="showHints">
      <span class="hint-text">
        <kbd>Enter</kbd> 发送
        <kbd>Shift + Enter</kbd> 换行
      </span>
    </div>

    <!-- 字符计数 -->
    <div class="char-count" v-if="showCharCount && inputValue.length > 0">
      {{ inputValue.length }} / {{ maxLength }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import {
  PaperClipIcon,
  XMarkIcon,
  PaperAirplaneIcon,
  StopIcon,
  PhotoIcon,
  DocumentIcon,
  VideoCameraIcon,
  MusicalNoteIcon
} from '@heroicons/vue/24/outline'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  files: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  },
  isStreaming: {
    type: Boolean,
    default: false
  },
  maxLength: {
    type: Number,
    default: 4000
  },
  placeholder: {
    type: String,
    default: '输入消息... (支持上传 PDF、Word、图片等文件)'
  },
  autoResize: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([
  'update:modelValue',
  'send',
  'stop-generation',
  'add-files',
  'remove-file'
])

const inputValue = ref(props.modelValue)
const isFocused = ref(false)
const textareaRef = ref(null)
const fileInputRef = ref(null)
const rows = ref(1)
const showHints = ref(false)
const showCharCount = ref(false)

const canSend = computed(() => {
  return !props.disabled &&
         !props.isStreaming &&
         (inputValue.value.trim().length > 0 || props.files.length > 0)
})

watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

watch(inputValue, (newVal) => {
  emit('update:modelValue', newVal)
  showCharCount.value = newVal.length > props.maxLength * 0.8
})

const focusTextarea = () => {
  textareaRef.value?.focus()
}

const handleInput = () => {
  if (props.autoResize) {
    adjustTextareaHeight()
  }
  showHints.value = inputValue.value.length > 0
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (canSend.value) {
      handleSend()
    }
  }
}

const handleSend = () => {
  if (!canSend.value) return

  emit('send', {
    content: inputValue.value,
    files: props.files
  })

  inputValue.value = ''
  rows.value = 1
  showHints.value = false
  if (textareaRef.value) {
    textareaRef.value.style.height = '26px'
  }
}

const handleFileSelect = (e) => {
  const selectedFiles = Array.from(e.target.files || [])
  if (selectedFiles.length === 0) return

  // 验证文件类型和大小
  const validFiles = []
  const MAX_SIZE = 10 * 1024 * 1024 // 10MB

  for (const file of selectedFiles) {
    // 检查文件大小
    if (file.size > MAX_SIZE) {
      alert(`文件 "${file.name}" 超过 10MB 限制，已跳过`)
      continue
    }

    // 检查文件类型
    const ext = getFileExtension(file.name).toLowerCase()
    const supportedExts = ['pdf', 'doc', 'docx', 'txt', 'jpg', 'jpeg', 'png', 'gif', 'mp3', 'mp4', 'wav']

    if (!supportedExts.includes(ext)) {
      alert(`不支持的文件类型: ${ext}，已跳过 "${file.name}"`)
      continue
    }

    validFiles.push(file)
  }

  if (validFiles.length > 0) {
    emit('add-files', validFiles)
  }

  e.target.value = ''
}

const getFileExtension = (fileName) => {
  if (!fileName) return ''
  const lastDotIndex = fileName.lastIndexOf('.')
  return lastDotIndex === -1 ? '' : fileName.substring(lastDotIndex + 1)
}

const adjustTextareaHeight = () => {
  const textarea = textareaRef.value
  if (!textarea) return

  textarea.style.height = 'auto'
  const newHeight = Math.min(textarea.scrollHeight, 72)
  textarea.style.height = newHeight + 'px'

  const newRows = Math.max(1, Math.min(Math.ceil(newHeight / 24), 3))
  rows.value = newRows
}

const getFileIcon = (file) => {
  const type = file.type
  const ext = getFileExtension(file.name).toLowerCase()

  // 优先根据文件扩展名判断
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return PhotoIcon
  if (['mp4', 'webm', 'mov'].includes(ext)) return VideoCameraIcon
  if (['mp3', 'wav', 'ogg'].includes(ext)) return MusicalNoteIcon
  if (['pdf', 'doc', 'docx', 'txt'].includes(ext)) return DocumentIcon

  // 降级到 MIME 类型判断
  if (type.startsWith('image/')) return PhotoIcon
  if (type.startsWith('video/')) return VideoCameraIcon
  if (type.startsWith('audio/')) return MusicalNoteIcon

  return DocumentIcon
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 自动调整高度
watch(() => inputValue.value, () => {
  nextTick(() => {
    if (props.autoResize) {
      adjustTextareaHeight()
    }
  })
})
</script>

<style scoped lang="scss">
.chat-input-area {
  position: relative;
  flex-shrink: 0;
  padding: 0.125rem 1.5rem 0.125rem;
  background: transparent;
  border-top: none;
  box-sizing: border-box;
  width: 100%;

  > * {
    max-width: 1120px;
    margin-left: auto;
    margin-right: auto;
  }
}

.file-previews {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.625rem;

  .file-preview {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    background: #f5f8fc;
    border-radius: 10px;
    border: 1px solid rgba(26, 61, 114, 0.08);
    animation: fileSlideIn 0.2s ease-out;

    @keyframes fileSlideIn {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .file-icon {
      width: 2rem;
      height: 2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      background: white;
      border-radius: 0.375rem;
      color: #007CF0;

      .icon {
        width: 1.125rem;
        height: 1.125rem;
      }
    }

    .file-info {
      display: flex;
      flex-direction: column;
      gap: 0.125rem;
      min-width: 0;
    }

    .file-name {
      font-size: 0.875rem;
      font-weight: 500;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 150px;
    }

    .file-size {
      font-size: 0.75rem;
      color: #999;
    }

    .remove-file-btn {
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
        background: rgba(255, 77, 79, 0.1);
        color: #ff4d4f;
      }

      .icon {
        width: 0.875rem;
        height: 0.875rem;
      }
    }
  }
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 0.625rem;
  min-height: 56px;
  max-height: 88px;
  padding: 0.625rem 0.875rem;
  background: #ffffff;
  border: 1px solid rgba(26, 61, 114, 0.1);
  border-radius: 24px;
  box-shadow: 0 8px 18px rgba(19, 46, 84, 0.05);
  transition: all 0.2s;

  &:hover {
    border-color: rgba(47, 111, 214, 0.2);
  }

  &.focused {
    border-color: #2f6fd6;
    box-shadow: 0 0 0 3px rgba(47, 111, 214, 0.08), 0 12px 28px rgba(19, 46, 84, 0.1);
  }
}

.input-actions-left,
.input-actions-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.action-btn {
  width: 2.125rem;
  height: 2.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #687b95;
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: #eff4fb;
    color: #2b486f;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.stop-btn {
    background: rgba(255, 77, 79, 0.1);
    color: #ff4d4f;

    &:hover {
      background: rgba(255, 77, 79, 0.2);
    }
  }

  .icon {
    width: 1.125rem;
    height: 1.125rem;
  }
}

.input-wrapper {
  flex: 1;
  min-height: 24px;
  display: flex;
  align-items: center;
}

.chat-textarea {
  width: 100%;
  height: 24px;
  min-height: 24px;
  max-height: 56px;
  padding: 0;
  border: none;
  background: transparent;
  resize: none;
  font-size: 1rem;
  line-height: 1.5;
  color: #22354d;
  font-family: inherit;

  &:focus {
    outline: none;
  }

  &::placeholder {
    color: #999;
  }

  &:disabled {
    opacity: 0.6;
  }
}

.send-btn {
  width: 2.125rem;
  height: 2.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #2f6fd6;
  color: white;
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: #255fbc;
    box-shadow: 0 4px 12px rgba(47, 111, 214, 0.22);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }

  .icon {
    width: 1.125rem;
    height: 1.125rem;
  }
}

.input-hints {
  display: none;

  .hint-text {
    font-size: 0.75rem;
    color: #7a8ba3;
    display: flex;
    align-items: center;
    gap: 0.5rem;

    kbd {
      padding: 0.125rem 0.375rem;
      background: rgba(0, 0, 0, 0.06);
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 0.25rem;
      font-family: inherit;
      font-size: 0.6875rem;
    }
  }
}

.char-count {
  position: absolute;
  bottom: 0.2rem;
  right: 1.8rem;
  font-size: 0.75rem;
  color: #999;
  pointer-events: none;

  &.warning {
    color: #f59e0b;
  }

  &.error {
    color: #ff4d4f;
  }
}

.hidden-input {
  display: none;
}

// 暗色模式
:deep(.dark) {
  .chat-input-area {
    background: transparent;
  }

  .file-previews .file-preview {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.08);

    .file-icon {
      background: rgba(255, 255, 255, 0.1);
    }

    .file-name {
      color: #e5e5e5;
    }

    .file-size {
      color: #666;
    }
  }

  .input-container {
    background: rgba(42, 42, 42, 0.92);
    border-color: rgba(255, 255, 255, 0.08);

    &:hover {
      border-color: rgba(0, 124, 240, 0.3);
    }

    &.focused {
      border-color: #007CF0;
      box-shadow: 0 0 0 3px rgba(0, 124, 240, 0.2);
    }
  }

  .toolbar-meta {
    color: #7e8795;
  }

  .action-btn {
    background: rgba(255, 255, 255, 0.05);
    color: #999;

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }
  }

  .chat-textarea {
    color: #e5e5e5;

    &::placeholder {
      color: #666;
    }
  }

  .input-hints .hint-text {
    color: #666;

    kbd {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.15);
      color: #999;
    }
  }

  .char-count {
    color: #666;
  }
}

@media (max-width: 980px) {
  .chat-input-area {
    padding: 0.125rem 1rem 0.25rem;

    > * {
      max-width: 100%;
    }
  }
}

@media (max-width: 768px) {
  .chat-input-area {
    padding: 0.125rem 0.75rem 0.25rem;
  }

  .input-container {
    min-height: 52px;
    max-height: 82px;
    padding: 0.5rem 0.75rem;
  }

  .action-btn,
  .send-btn {
    width: 2rem;
    height: 2rem;
  }

  .chat-textarea {
    font-size: 0.9375rem;
  }

  .file-previews {
    gap: 0.375rem;
    margin-bottom: 0.5rem;

    .file-preview {
      padding: 0.375rem 0.625rem;

      .file-name {
        max-width: 120px;
        font-size: 0.8125rem;
      }

      .file-size {
        font-size: 0.6875rem;
      }
    }
  }
}

@media (max-width: 560px) {
  .chat-input-area {
    padding: 0.125rem 0.5rem 0.25rem;
  }

  .input-container {
    gap: 0.5rem;
    border-radius: 18px;
    padding: 0.45rem 0.625rem;
  }

  .file-previews {
    .file-preview {
      padding: 0.3125rem 0.5rem;

      .file-icon {
        width: 1.75rem;
        height: 1.75rem;
      }

      .file-name {
        max-width: 100px;
      }

      .remove-file-btn {
        width: 1.25rem;
        height: 1.25rem;
      }
    }
  }
}
</style>
