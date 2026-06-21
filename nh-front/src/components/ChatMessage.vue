<template>
  <div class="message" :class="{ 'message-user': isUser }">
    <div class="avatar">
      <template v-if="isUser">
        <img
          v-if="showUserAvatarImage"
          :src="userAvatarSrc"
          alt="用户头像"
          class="avatar-image"
          @error="handleUserAvatarError"
        />
        <UserCircleIcon v-else class="icon" />
      </template>
      <template v-else>
        <div v-if="isStreaming" class="assistant-cursor"></div>
        <SparklesIcon v-else class="icon assistant-icon" />
      </template>
    </div>
    <div class="content" :class="{ 'content-structured': !isUser && isStructuredAssistantResponse }">
      <div class="text-container">
        <button v-if="isUser" class="user-copy-button" @click="copyContent" :title="copyButtonTitle">
          <DocumentDuplicateIcon v-if="!copied" class="copy-icon" />
          <CheckIcon v-else class="copy-icon copied" />
        </button>
        <div v-if="isUser && userAttachments.length" class="attachment-list">
          <div class="attachment-card" v-for="(file, fileIndex) in userAttachments" :key="`${file.name}-${fileIndex}`">
            <div v-if="file.isImage && file.previewUrl" class="attachment-image-wrap">
              <img :src="file.previewUrl" :alt="file.name" class="attachment-image" />
            </div>
            <div v-else-if="file.isImage" class="attachment-image-placeholder">
              <PhotoIcon class="attachment-image-placeholder-icon" />
            </div>
            <div v-else class="attachment-icon">
              <DocumentTextIcon class="attachment-doc-icon" />
            </div>
            <div class="attachment-meta">
              <div class="attachment-name">{{ file.name }}</div>
              <div class="attachment-type">{{ file.fileType || 'FILE' }}</div>
            </div>
          </div>
        </div>
        <div class="text" ref="contentRef" v-if="isUser">
          {{ message.content }}
        </div>
        <div class="text markdown-content" ref="contentRef" v-else v-html="processedContent"></div>
      </div>
      <div class="message-footer" v-if="!isUser">
        <button class="copy-button" @click="copyContent" :title="copyButtonTitle">
          <DocumentDuplicateIcon v-if="!copied" class="copy-icon" />
          <CheckIcon v-else class="copy-icon copied" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, nextTick, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { UserCircleIcon, DocumentDuplicateIcon, CheckIcon, SparklesIcon, DocumentTextIcon, PhotoIcon } from '@heroicons/vue/24/outline'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { useUserStore } from '@/stores/userStore'
import { buildImageProxySrc } from '@/api/file'

const contentRef = ref(null)
const copied = ref(false)
const userStore = useUserStore()
const userAvatarLoadFailed = ref(false)
const filePreviewUrlCache = new Map()
const copyButtonTitle = computed(() => copied.value ? '已复制' : '复制内容')
const userAvatarSrc = computed(() => buildImageProxySrc(userStore.userInfo?.avatar || ''))
const showUserAvatarImage = computed(() => Boolean(userAvatarSrc.value) && !userAvatarLoadFailed.value)

// 配置 marked
marked.setOptions({
  breaks: false,
  gfm: true,
  sanitize: false
})

// 处理内容
const processContent = (content) => {
  if (!content) return ''
  const normalizedContent = normalizeAssistantWhitespace(content)
  const structuredContent = enhanceAssistantStructure(normalizedContent)

  // 分析内容中的 think 标签
  let result = ''
  let isInThinkBlock = false
  let currentBlock = ''

  // 逐字符分析，处理 think 标签
  for (let i = 0; i < structuredContent.length; i++) {
    if (structuredContent.slice(i, i + 7) === '<think>') {
      isInThinkBlock = true
      if (currentBlock) {
        // 将之前的普通内容转换为 HTML
        result += marked.parse(currentBlock)
      }
      currentBlock = ''
      i += 6 // 跳过 <think>
      continue
    }

    if (structuredContent.slice(i, i + 8) === '</think>') {
      isInThinkBlock = false
      // 将 think 块包装在特殊 div 中
      result += `<div class="think-block">${marked.parse(currentBlock)}</div>`
      currentBlock = ''
      i += 7 // 跳过 </think>
      continue
    }

    currentBlock += structuredContent[i]
  }

  // 处理剩余内容
  if (currentBlock) {
    if (isInThinkBlock) {
      result += `<div class="think-block">${marked.parse(currentBlock)}</div>`
    } else {
      result += marked.parse(currentBlock)
    }
  }

  // 净化处理后的 HTML
  const cleanHtml = DOMPurify.sanitize(result, {
    ADD_TAGS: ['think', 'code', 'pre', 'span'],
    ADD_ATTR: ['class', 'language']
  })

  // 在净化后的 HTML 中查找代码块并添加复制按钮
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = cleanHtml

  // 查找所有代码块
  const preElements = tempDiv.querySelectorAll('pre')
  preElements.forEach(pre => {
    const code = pre.querySelector('code')
    if (code) {
      // 创建包装器
      const wrapper = document.createElement('div')
      wrapper.className = 'code-block-wrapper'

      // 添加复制按钮
      const copyBtn = document.createElement('button')
      copyBtn.className = 'code-copy-button'
      copyBtn.title = '复制代码'
      copyBtn.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" class="code-copy-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      `

      // 添加成功消息
      const successMsg = document.createElement('div')
      successMsg.className = 'copy-success-message'
      successMsg.textContent = '已复制!'

      // 组装结构
      wrapper.appendChild(copyBtn)
      wrapper.appendChild(pre.cloneNode(true))
      wrapper.appendChild(successMsg)

      // 替换原始的 pre 元素
      pre.parentNode.replaceChild(wrapper, pre)
    }
  })

  const tables = tempDiv.querySelectorAll('table')
  tables.forEach(table => {
    const wrapper = document.createElement('div')
    wrapper.className = 'table-scroll'
    table.parentNode?.insertBefore(wrapper, table)
    wrapper.appendChild(table)
  })

  compactMarkdownDom(tempDiv)

  return tempDiv.innerHTML
}

// 归一化助手回复中的多余空白，避免命中知识库后出现大段空行。
// 注意：代码块内容保持原样，不做压缩。
const normalizeAssistantWhitespace = (raw) => {
  const text = String(raw ?? '')
  const sections = text.split(/(```[\s\S]*?```)/g)
  return sections
    .map((section) => {
      if (section.startsWith('```')) {
        return section
      }
      return section
        .replace(/\r\n/g, '\n')
        .replace(/\u00A0/g, ' ')
        .replace(/(<br\s*\/?>\s*){2,}/gi, '<br>')
        .replace(/<p>(\s|&nbsp;|<br\s*\/?>)*<\/p>/gi, '')
        .replace(/[ \t]+\n/g, '\n')
        .replace(/\n{3,}/g, '\n\n')
        .replace(/[ \t]{2,}/g, ' ')
        .trim()
    })
    .join('\n')
    .trim()
}

const enhanceAssistantStructure = (raw) => {
  const text = String(raw ?? '')
  const sections = text.split(/(```[\s\S]*?```)/g)

  return sections
    .map((section) => {
      if (section.startsWith('```')) {
        return section
      }
      return structurePlainAssistantSection(section)
    })
    .join('\n')
    .trim()
}

const SEMANTIC_LABELS = ['结论', '结果', '依据与说明', '下一步建议', '建议', '风险与限制', '补充说明', '特例']

const structurePlainAssistantSection = (section) => {
  const physicalLines = String(section ?? '')
    .split('\n')
    .map(line => line.trim())

  const semanticLines = []
  physicalLines.forEach((line) => {
    if (!line) {
      semanticLines.push('')
      return
    }
    const segments = splitInlineSemanticSegments(line)
    segments.forEach((segment, index) => {
      semanticLines.push(cleanSemanticLine(segment))
      if (index < segments.length - 1) {
        semanticLines.push('')
      }
    })
  })

  const blocks = []
  let currentBlock = null

  const flushCurrentBlock = () => {
    if (!currentBlock) return
    const content = currentBlock.type === 'table'
      ? currentBlock.lines
        .map(line => line.trim())
        .filter(Boolean)
        .join('\n')
        .trim()
      : currentBlock.lines
        .map(line => line.trim())
        .filter(Boolean)
        .join(' ')
        .replace(/\s{2,}/g, ' ')
        .trim()

    if (!content) {
      currentBlock = null
      return
    }

    if (currentBlock.type === 'label') {
      const tail = content ? ` ${content}` : ''
      blocks.push(`**${currentBlock.label}：**${tail}`)
    } else if (currentBlock.type === 'numbered') {
      blocks.push(`${currentBlock.index}. ${content}`)
    } else if (currentBlock.type === 'table') {
      blocks.push(content)
    } else {
      blocks.push(content)
    }

    currentBlock = null
  }

  semanticLines.forEach((line) => {
    if (!line) {
      flushCurrentBlock()
      return
    }

    const parsed = parseSemanticLine(line)
    if (!parsed) {
      if (!currentBlock) {
        currentBlock = { type: 'paragraph', lines: [line] }
      } else if (currentBlock.type === 'table') {
        flushCurrentBlock()
        currentBlock = { type: 'paragraph', lines: [line] }
      } else {
        currentBlock.lines.push(line)
      }
      return
    }

    if (!currentBlock) {
      currentBlock = parsed
      return
    }

    if (currentBlock.type === 'table' && parsed.type === 'table') {
      currentBlock.lines.push(...parsed.lines)
      return
    }

    if (currentBlock.type === 'paragraph') {
      flushCurrentBlock()
      currentBlock = parsed
      return
    }

    flushCurrentBlock()
    currentBlock = parsed
  })

  flushCurrentBlock()

  return blocks
    .filter(Boolean)
    .join('\n\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

const splitInlineSemanticSegments = (line) => {
  const normalized = String(line ?? '').replace(/\s+/g, ' ').trim()
  if (!normalized) return []
  if (isMarkdownTableLine(normalized)) return [normalizeMarkdownTableLine(normalized)]

  const segments = []
  let lastIndex = 0

  for (let i = 1; i < normalized.length; i++) {
    if (!shouldSplitBeforeMarker(normalized, i)) {
      continue
    }
    const segment = normalized.slice(lastIndex, i).trim()
    if (segment) {
      segments.push(segment)
    }
    lastIndex = i
    const markerLength = getSemanticMarkerLength(normalized.slice(i))
    if (markerLength > 1) {
      i += markerLength - 1
    }
  }

  const tail = normalized.slice(lastIndex).trim()
  if (tail) {
    segments.push(tail)
  }

  return segments
}

const isMarkdownTableLine = (line) => {
  const trimmed = String(line ?? '').trim()
  if (!trimmed.includes('|')) return false
  if (/^\|?(?:\s*:?-{3,}:?\s*\|){1,}\s*:?-{3,}:?\s*\|?$/.test(trimmed)) return true
  return /^\|?(?:[^|\n]+\|){2,}[^|\n]*\|?$/.test(trimmed)
}

const normalizeMarkdownTableLine = (line) => {
  return String(line ?? '')
    .replace(/\s*\|\s*/g, ' | ')
    .replace(/^ \| /, '| ')
    .replace(/ \| $/, ' |')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

const getSemanticMarkerLength = (line) => {
  const normalized = String(line ?? '')
  if (!normalized) return 0

  const numberedMatch = normalized.match(/^(\d+\.\s*)/)
  if (numberedMatch) {
    return numberedMatch[1].length
  }

  const bulletPrefixMatch = normalized.match(/^(?:[*＊•-]\s*)/)
  const bulletPrefixLength = bulletPrefixMatch ? bulletPrefixMatch[0].length : 0
  const withoutBulletPrefix = normalized.slice(bulletPrefixLength)
  const sortedLabels = [...SEMANTIC_LABELS].sort((a, b) => b.length - a.length)

  for (const label of sortedLabels) {
    if (!withoutBulletPrefix.startsWith(label)) {
      continue
    }
    const suffix = withoutBulletPrefix.slice(label.length)
    const separatorMatch = suffix.match(/^[：:\s]*/)
    return bulletPrefixLength + label.length + (separatorMatch ? separatorMatch[0].length : 0)
  }

  if (bulletPrefixLength > 0) {
    return bulletPrefixLength
  }

  return 0
}

const shouldSplitBeforeMarker = (line, index) => {
  const rest = line.slice(index)
  if (!rest) return false

  const previousChar = line[index - 1]
  const hasContentBefore = line.slice(0, index).trim().length > 0
  if (!hasContentBefore) return false

  if (/^\d+\.\s*\S/.test(rest) && !/\d/.test(previousChar)) {
    return true
  }

  const labelMatch = matchSemanticLabelStart(rest)
  if (labelMatch) {
    if (labelMatch.label === '建议' && !/[。；:：!?！？)\]】\s]/.test(previousChar)) {
      return false
    }
    return true
  }

  return false
}

const cleanSemanticLine = (line) => {
  return String(line ?? '')
    .replace(/\*\*/g, '')
    .replace(/[ \t]{2,}/g, ' ')
    .replace(/^(?:[*＊•-])\s+/, '')
    .replace(/(\s)[*＊](?=[\u4e00-\u9fa5A-Za-z0-9])/g, '$1')
    .replace(/([\u4e00-\u9fa5A-Za-z0-9])[*＊](?=[\u4e00-\u9fa5A-Za-z0-9])/g, '$1 ')
    .trim()
}

const matchSemanticLabelStart = (line) => {
  let normalized = String(line ?? '').trim()
  if (!normalized) return null

  normalized = normalized.replace(/^(?:[*＊•-])\s*/, '')
  const sortedLabels = [...SEMANTIC_LABELS].sort((a, b) => b.length - a.length)

  for (const label of sortedLabels) {
    if (!normalized.startsWith(label)) {
      continue
    }
    const rest = normalized.slice(label.length).replace(/^[：:\s]+/, '').trim()
    return { label, rest }
  }

  return null
}

const parseSemanticLine = (line) => {
  const trimmed = String(line ?? '').trim()
  if (!trimmed) return null

  const numberedMatch = trimmed.match(/^(\d+)\.\s*(.+)$/)
  if (numberedMatch) {
    return {
      type: 'numbered',
      index: numberedMatch[1],
      lines: [numberedMatch[2].trim()]
    }
  }

  if (isMarkdownTableLine(trimmed)) {
    return {
      type: 'table',
      lines: [normalizeMarkdownTableLine(trimmed)]
    }
  }

  const labelMatch = matchSemanticLabelStart(trimmed)
  if (labelMatch) {
    return {
      type: 'label',
      label: labelMatch.label,
      lines: labelMatch.rest ? [labelMatch.rest] : []
    }
  }

  return null
}

const compactMarkdownDom = (root) => {
  if (!root) return

  root.querySelectorAll('p, div, blockquote').forEach((node) => {
    if (node.classList?.contains('code-block-wrapper') || node.classList?.contains('table-scroll') || node.classList?.contains('think-block')) {
      return
    }
    const html = (node.innerHTML || '').replace(/<br\s*\/?>/gi, '').replace(/&nbsp;/gi, '').trim()
    const text = (node.textContent || '').replace(/\u00A0/g, '').trim()
    if (!html && !text) {
      node.remove()
    }
  })

  root.querySelectorAll('li').forEach((item) => {
    const directParagraphs = Array.from(item.children).filter(child => child.tagName === 'P')
    if (directParagraphs.length === 1 && item.children.length === 1) {
      const paragraph = directParagraphs[0]
      while (paragraph.firstChild) {
        item.insertBefore(paragraph.firstChild, paragraph)
      }
      paragraph.remove()
    }

    const itemText = (item.textContent || '').replace(/\u00A0/g, '').trim()
    const itemHtml = (item.innerHTML || '').replace(/<br\s*\/?>/gi, '').replace(/&nbsp;/gi, '').trim()
    if (!itemText && !itemHtml) {
      item.remove()
    }
  })

  root.querySelectorAll('ul, ol').forEach((list) => {
    if (!list.children.length) {
      list.remove()
    }
  })
}

// 修改计算属性
const processedContent = computed(() => {
  if (!props.message.content) return ''
  return processContent(props.message.content)
})

const isStructuredAssistantResponse = computed(() => {
  if (isUser.value || !props.message.content) return false
  return props.message.content.includes('| --- |')
    || props.message.content.includes('## ')
    || props.message.content.includes('> 统计范围')
    || props.message.content.includes('> 数据来源')
})

// 为代码块添加复制功能
const setupCodeBlockCopyButtons = () => {
  if (!contentRef.value) return;

  const codeBlocks = contentRef.value.querySelectorAll('.code-block-wrapper');
  codeBlocks.forEach(block => {
    const copyButton = block.querySelector('.code-copy-button');
    const codeElement = block.querySelector('code');
    const successMessage = block.querySelector('.copy-success-message');

    if (copyButton && codeElement) {
      // 移除旧的事件监听器
      const newCopyButton = copyButton.cloneNode(true);
      copyButton.parentNode.replaceChild(newCopyButton, copyButton);

      // 添加新的事件监听器
      newCopyButton.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();
        try {
          const code = codeElement.textContent || '';
          await navigator.clipboard.writeText(code);

          // 显示成功消息
          if (successMessage) {
            successMessage.classList.add('visible');
            setTimeout(() => {
              successMessage.classList.remove('visible');
            }, 2000);
          }
        } catch (err) {
          console.error('复制代码失败:', err);
        }
      });
    }
  });
}

// 在内容更新后手动应用高亮和设置复制按钮
const highlightCode = async () => {
  await nextTick()
  if (contentRef.value) {
    contentRef.value.querySelectorAll('pre code').forEach((block) => {
      hljs.highlightElement(block)
    })

    // 设置代码块复制按钮
    setupCodeBlockCopyButtons()
  }
}

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const isUser = computed(() => props.message.role === 'user')
const userAttachments = computed(() => {
  if (!isUser.value) return []

  const fileMap = new Map()
  if (Array.isArray(props.message.files)) {
    props.message.files
      .filter(file => file && file.name)
      .forEach(file => fileMap.set(file.name, file))
  }

  const normalizeAttachment = (name, fileType, url) => {
    const normalizedType = (fileType || (name.includes('.') ? name.split('.').pop().toUpperCase() : 'FILE')).toUpperCase()
    const localFile = fileMap.get(name)
    const mimeType = localFile?.type || ''
    const isImageByExt = ['JPG', 'JPEG', 'PNG', 'GIF', 'WEBP', 'BMP'].includes(normalizedType)
    const isImage = isImageByExt || mimeType.startsWith('image/')
    const localPreview = isImage ? getFilePreviewUrl(localFile) : ''
    const persistedPreview = isImage ? buildImageProxySrc(url || '') : ''

    return {
      name,
      fileType: normalizedType,
      isImage,
      url: url || '',
      previewUrl: localPreview || persistedPreview
    }
  }

  if (Array.isArray(props.message.attachments) && props.message.attachments.length) {
    return props.message.attachments
      .filter(item => item && item.name)
      .map(item => normalizeAttachment(item.name, item.fileType, item.url))
  }

  if (Array.isArray(props.message.files) && props.message.files.length) {
    return props.message.files
      .filter(file => file && file.name)
      .map(file => normalizeAttachment(file.name, file.name.includes('.') ? file.name.split('.').pop().toUpperCase() : 'FILE', ''))
  }

  return []
})

const getFilePreviewUrl = (file) => {
  if (!file) return ''
  if (filePreviewUrlCache.has(file)) {
    return filePreviewUrlCache.get(file)
  }
  const objectUrl = URL.createObjectURL(file)
  filePreviewUrlCache.set(file, objectUrl)
  return objectUrl
}

const handleUserAvatarError = () => {
  userAvatarLoadFailed.value = true
}

watch(userAvatarSrc, () => {
  userAvatarLoadFailed.value = false
})

// 复制内容到剪贴板
const copyContent = async () => {
  try {
    // 获取纯文本内容
    let textToCopy = props.message.content;

    // 如果是AI回复，需要去除HTML标签
    if (!isUser.value && contentRef.value) {
      // 创建临时元素来获取纯文本
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = processedContent.value;
      textToCopy = tempDiv.textContent || tempDiv.innerText || '';
    }

    await navigator.clipboard.writeText(textToCopy);
    copied.value = true;

    // 3秒后重置复制状态
    setTimeout(() => {
      copied.value = false;
    }, 3000);
  } catch (err) {
    console.error('复制失败:', err);
  }
}

// 监听内容变化
watch(() => props.message.content, () => {
  if (!isUser.value) {
    highlightCode()
  }
})

// 初始化时也执行一次
onMounted(() => {
  if (!isUser.value) {
    highlightCode()
  }
})

onUnmounted(() => {
  for (const objectUrl of filePreviewUrlCache.values()) {
    URL.revokeObjectURL(objectUrl)
  }
  filePreviewUrlCache.clear()
})
</script>

<style scoped lang="scss">
.message {
  display: flex;
  margin-bottom: 1.25rem;
  gap: 0.875rem;

  &.message-user {
    flex-direction: row-reverse;

    .content {
      align-items: flex-end;

      .text-container {
        position: relative;

        .text {
          background: transparent;
          color: #000000;
          border-radius: 0;
          border: none;
          box-shadow: none;
          padding: 0;
        }

        .user-copy-button {
          position: absolute;
          left: -30px;
          top: 50%;
          transform: translateY(-50%);
          background: transparent;
          border: none;
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          opacity: 0;
          transition: opacity 0.2s;

          .copy-icon {
            width: 16px;
            height: 16px;
            color: #666;

            &.copied {
              color: #4ade80;
            }
          }
        }

        &:hover .user-copy-button {
          opacity: 1;
        }
      }

      .message-footer {
        flex-direction: row-reverse;
      }
    }
  }

  .avatar {
    width: 36px;
    height: 36px;
    flex-shrink: 0;

    .avatar-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 8px;
      border: 1px solid rgba(47, 111, 214, 0.16);
      background: #f3f6fb;
      display: block;
    }

    .icon {
      width: 100%;
      height: 100%;
      color: #666;
      padding: 4px;
      border-radius: 8px;
      transition: all 0.2s ease;

      &.assistant-icon {
        color: #2f6fd6;
      }
    }

    .assistant-cursor {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;

      &::before,
      &::after {
        content: '';
        width: 4px;
        height: 4px;
        background: #2f6fd6;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;
      }

      &::after {
        animation-delay: 0.2s;
      }
    }
  }

  .content {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    max-width: min(78%, 860px);

    &.content-structured {
      max-width: min(92%, 1120px);

      .text-container {
        width: 100%;
      }

      .text {
        width: auto;
        padding: 0;
        border-radius: 0;
        box-shadow: none;
      }
    }

    .text-container {
      position: relative;
    }

    .message-footer {
      display: flex;
      align-items: center;
      margin-top: 0.25rem;

      .time {
        font-size: 0.75rem;
        color: #666;
      }

      .copy-button {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        background: transparent;
        border: none;
        font-size: 0.75rem;
        color: #666;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        cursor: pointer;
        margin-right: auto;
        transition: background-color 0.2s;

        &:hover {
          background-color: rgba(0, 0, 0, 0.05);
        }

        .copy-icon {
          width: 14px;
          height: 14px;

          &.copied {
            color: #4ade80;
          }
        }

        .copy-text {
          font-size: 0.75rem;
        }
      }
    }

    .text {
      padding: 0;
      border-radius: 0;
      line-height: 1.65;
      white-space: pre-wrap;
      color: #000000;
      background: transparent;
      border: none;
      box-shadow: none;

      .cursor {
        animation: blink 1s infinite;
      }

      :deep(.think-block) {
        position: relative;
        padding: 0.75rem 1rem 0.75rem 1.5rem;
        margin: 0.5rem 0;
        color: #666;
        font-style: italic;
        border-left: 4px solid #ddd;
        background-color: rgba(0, 0, 0, 0.03);
        border-radius: 0 0.5rem 0.5rem 0;

        // 添加平滑过渡效果
        opacity: 1;
        transform: translateX(0);
        transition: opacity 0.3s ease, transform 0.3s ease;

        &::before {
          content: '思考';
          position: absolute;
          top: -0.75rem;
          left: 1rem;
          padding: 0 0.5rem;
          font-size: 0.75rem;
          background: #f5f5f5;
          border-radius: 0.25rem;
          color: #999;
          font-style: normal;
        }

        // 添加进入动画
        &:not(:first-child) {
          animation: slideIn 0.3s ease forwards;
        }
      }

      :deep(pre) {
        background: #f6f8fb;
        padding: 1rem;
        border-radius: 0.5rem;
        overflow-x: auto;
        margin: 0.5rem 0;
        border: 1px solid #e1e4e8;

        code {
          background: transparent;
          padding: 0;
          font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace;
          font-size: 0.9rem;
          line-height: 1.5;
          tab-size: 2;
        }
      }

      :deep(.hljs) {
        color: #24292e;
        background: transparent;
      }

      :deep(.hljs-keyword) {
        color: #d73a49;
      }

      :deep(.hljs-built_in) {
        color: #005cc5;
      }

      :deep(.hljs-type) {
        color: #6f42c1;
      }

      :deep(.hljs-literal) {
        color: #005cc5;
      }

      :deep(.hljs-number) {
        color: #005cc5;
      }

      :deep(.hljs-regexp) {
        color: #032f62;
      }

      :deep(.hljs-string) {
        color: #032f62;
      }

      :deep(.hljs-subst) {
        color: #24292e;
      }

      :deep(.hljs-symbol) {
        color: #e36209;
      }

      :deep(.hljs-class) {
        color: #6f42c1;
      }

      :deep(.hljs-function) {
        color: #6f42c1;
      }

      :deep(.hljs-title) {
        color: #6f42c1;
      }

      :deep(.hljs-params) {
        color: #24292e;
      }

      :deep(.hljs-comment) {
        color: #6a737d;
      }

      :deep(.hljs-doctag) {
        color: #d73a49;
      }

      :deep(.hljs-meta) {
        color: #6a737d;
      }

      :deep(.hljs-section) {
        color: #005cc5;
      }

      :deep(.hljs-name) {
        color: #22863a;
      }

      :deep(.hljs-attribute) {
        color: #005cc5;
      }

      :deep(.hljs-variable) {
        color: #e36209;
      }
    }
  }
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.dark {
  .message {
    .avatar {
      .assistant-cursor {
        &::before,
        &::after {
          background: #5a9df7;
        }
      }

      .icon.assistant-icon {
        color: #5a9df7;
      }
    }

    &.message-user {
      .content .text-container {
        .text {
          background: transparent;
          color: #000000;
          border: none;
          padding: 0;
        }

        .user-copy-button {
          .copy-icon {
            color: #999;

            &.copied {
              color: #4ade80;
            }
          }
        }
      }
    }

    .content {
      .text {
        color: #000000;
      }

      .message-footer {
        .time {
          color: #999;
        }

        .copy-button {
          color: #999;

          &:hover {
            background-color: rgba(255, 255, 255, 0.1);
          }
        }
      }

      .text {
        :deep(.think-block) {
          background-color: rgba(255, 255, 255, 0.03);
          border-left-color: #666;
          color: #999;

          &::before {
            background: #2a2a2a;
            color: #888;
          }
        }

        :deep(pre) {
          background: #161b22;
          border-color: #30363d;

          code {
            color: #c9d1d9;
          }
        }

        :deep(.hljs) {
          color: #c9d1d9;
          background: transparent;
        }

        :deep(.hljs-keyword) {
          color: #ff7b72;
        }

        :deep(.hljs-built_in) {
          color: #79c0ff;
        }

        :deep(.hljs-type) {
          color: #ff7b72;
        }

        :deep(.hljs-literal) {
          color: #79c0ff;
        }

        :deep(.hljs-number) {
          color: #79c0ff;
        }

        :deep(.hljs-regexp) {
          color: #a5d6ff;
        }

        :deep(.hljs-string) {
          color: #a5d6ff;
        }

        :deep(.hljs-subst) {
          color: #c9d1d9;
        }

        :deep(.hljs-symbol) {
          color: #ffa657;
        }

        :deep(.hljs-class) {
          color: #f2cc60;
        }

        :deep(.hljs-function) {
          color: #d2a8ff;
        }

        :deep(.hljs-title) {
          color: #d2a8ff;
        }

        :deep(.hljs-params) {
          color: #c9d1d9;
        }

        :deep(.hljs-comment) {
          color: #8b949e;
        }

        :deep(.hljs-doctag) {
          color: #ff7b72;
        }

        :deep(.hljs-meta) {
          color: #8b949e;
        }

        :deep(.hljs-section) {
          color: #79c0ff;
        }

        :deep(.hljs-name) {
          color: #7ee787;
        }

        :deep(.hljs-attribute) {
          color: #79c0ff;
        }

        :deep(.hljs-variable) {
          color: #ffa657;
        }
      }

      &.message-user .content .text {
        background: transparent;
        color: #000000;
      }
    }
  }

  .attachment-card {
    background: transparent;
    border-color: transparent;
    box-shadow: none;

    &:hover {
      border-color: transparent;
      box-shadow: none;
    }
  }

  .attachment-name {
    color: #000000;
  }

  .attachment-type {
    color: #000000;
    background: transparent;
    border-color: transparent;
  }
}

.markdown-content {
  white-space: normal !important;
  line-height: 1.55;

  :deep(p:first-child),
  :deep(ol:first-child),
  :deep(ul:first-child),
  :deep(blockquote:first-child),
  :deep(h1:first-child),
  :deep(h2:first-child),
  :deep(h3:first-child),
  :deep(h4:first-child) {
    margin-top: 0 !important;
  }

  :deep(p:last-child),
  :deep(ol:last-child),
  :deep(ul:last-child),
  :deep(blockquote:last-child),
  :deep(h1:last-child),
  :deep(h2:last-child),
  :deep(h3:last-child),
  :deep(h4:last-child) {
    margin-bottom: 0 !important;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 0.12rem 0 0.45rem;
    color: #000000;
    line-height: 1.25;
    letter-spacing: -0.02em;
  }

  :deep(h2) {
    font-size: 1.1rem;
    font-weight: 700;
  }

  :deep(h3) {
    font-size: 1rem;
    font-weight: 650;
  }

  :deep(p) {
    margin: 0.2rem 0;
    color: #000000;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(ul),
  :deep(ol) {
    margin: 0.12rem 0;
    padding-left: 1.5rem;
  }

  :deep(li) {
    margin: 0.04rem 0;
    line-height: 1.55;
    color: #000000;
  }

  :deep(p) {
    margin: 0.18rem 0;
    line-height: 1.55;
  }

  :deep(li > p) {
    margin: 0;
    display: block;
  }

  :deep(p + ol),
  :deep(p + ul),
  :deep(ol + p),
  :deep(ul + p) {
    margin-top: 0.08rem;
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.05);
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-size: 0.9em;
    font-family: ui-monospace, monospace;
  }

  :deep(pre code) {
    background: transparent;
    padding: 0;
  }

  :deep(table) {
    border-collapse: collapse;
    margin: 0;
    width: 100%;
    min-width: 560px;
    background: transparent;
    color: #000000;
    table-layout: auto;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #d9e5f4;
    padding: 0.8rem 0.9rem;
    text-align: left;
    vertical-align: top;
  }

  :deep(th) {
    background: transparent;
    color: #000000;
    font-weight: 600;
  }

  :deep(tbody tr:nth-child(even)) {
    background: transparent;
  }

  :deep(tbody tr:hover) {
    background: transparent;
  }

  :deep(blockquote) {
    margin: 0.32rem 0;
    padding: 0;
    border-left: none;
    color: #000000;
    background: transparent;
    border-radius: 0;
  }

  :deep(blockquote.info-blockquote) {
    border-left: none;
    background: transparent;
    color: #000000;
  }

  :deep(.table-scroll) {
    margin: 0.45rem 0 0.2rem;
    overflow-x: auto;
    width: 100%;
    border-radius: 0;
    border: none;
    background: transparent;

    &::-webkit-scrollbar {
      height: 7px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(47, 111, 214, 0.2);
      border-radius: 999px;
    }
  }

  :deep(.code-block-wrapper) {
    position: relative;
    margin: 0.55rem 0;
    border-radius: 6px;
    overflow: hidden;

    .code-copy-button {
      position: absolute;
      top: 0.5rem;
      right: 0.5rem;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      color: #e6e6e6;
      cursor: pointer;
      padding: 0.25rem;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.2s, background-color 0.2s;
      z-index: 10;

      &:hover {
        background-color: rgba(255, 255, 255, 0.2);
      }

      .code-copy-icon {
        width: 16px;
        height: 16px;
      }
    }

    &:hover .code-copy-button {
      opacity: 0.8;
    }

    pre {
      margin: 0;
      padding: 1rem;
      background: #1e1e1e;
      overflow-x: auto;

      code {
        background: transparent;
        padding: 0;
        font-family: ui-monospace, monospace;
      }
    }

    .copy-success-message {
      position: absolute;
      top: 0.5rem;
      right: 0.5rem;
      background: rgba(74, 222, 128, 0.9);
      color: white;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      opacity: 0;
      transform: translateY(-10px);
      transition: opacity 0.3s, transform 0.3s;
      pointer-events: none;
      z-index: 20;

      &.visible {
        opacity: 1;
        transform: translateY(0);
      }
    }
  }
}

.dark {
  .retrieval-panel {
    background: #f3f7fd;
    border-color: rgba(47, 111, 214, 0.14);
  }

  .markdown-content {
    :deep(.code-block-wrapper) {
      .code-copy-button {
        background: rgba(255, 255, 255, 0.05);

        &:hover {
          background-color: rgba(255, 255, 255, 0.1);
        }
      }

      pre {
        background: #0d0d0d;
      }
    }

    :deep(code) {
      background: rgba(255, 255, 255, 0.1);
    }

    :deep(th),
    :deep(td) {
      border-color: #d9e5f4;
      color: #000000;
    }

    :deep(th) {
      background: transparent;
      color: #000000;
    }

    :deep(blockquote) {
      color: #000000;
      background: transparent;
      border-left: none;
    }

    :deep(blockquote.info-blockquote) {
      border-left: none;
      background: transparent;
      color: #000000;
    }

    :deep(table) {
      background: transparent;
      color: #000000;
    }

    :deep(tbody tr:nth-child(even)) {
      background: transparent;
    }

    :deep(tbody tr:hover) {
      background: transparent;
    }

    :deep(.table-scroll) {
      border: none;
      background: transparent;
    }
  }
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  min-width: 240px;
  max-width: 430px;
  background: transparent;
  border: none;
  border-radius: 10px;
  padding: 0.7rem 0.8rem;
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: transparent;
    box-shadow: none;
  }
}

.attachment-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #ff4d4f 0%, #e53935 100%);
  color: #fff;
  flex-shrink: 0;
}

.attachment-image-wrap {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(26, 61, 114, 0.12);
  background: #f3f6fb;
  flex-shrink: 0;
}

.attachment-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.attachment-image-placeholder {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 8px;
  border: 1px solid rgba(47, 111, 214, 0.16);
  background: linear-gradient(180deg, #eef5ff 0%, #e5f0ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2f6fd6;
  flex-shrink: 0;
}

.attachment-image-placeholder-icon {
  width: 1.1rem;
  height: 1.1rem;
}

.attachment-doc-icon {
  width: 1.15rem;
  height: 1.15rem;
  stroke-width: 2;
  line-height: 1;
}

.attachment-meta {
  min-width: 0;
}

.attachment-name {
  font-size: 0.88rem;
  color: #000000;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-type {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  color: #000000;
  margin-top: 4px;
  padding: 1px 8px;
  border-radius: 999px;
  background: transparent;
  border: none;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.retrieval-panel {
  margin-top: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: 12px;
  border: 1px solid rgba(47, 111, 214, 0.12);
  background: linear-gradient(180deg, #f8fbff 0%, #f2f7fd 100%);
}

.retrieval-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.retrieval-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 600;

  &.hit {
    color: #1f4f9c;
    background: rgba(47, 111, 214, 0.16);
  }

  &.miss {
    color: #6b7280;
    background: rgba(107, 114, 128, 0.14);
  }

  &.unknown {
    color: #7c5e10;
    background: rgba(250, 204, 21, 0.2);
  }
}

.retrieval-sources {
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.retrieval-source {
  padding: 0.45rem 0.55rem;
  border-radius: 10px;
  border: 1px dashed rgba(47, 111, 214, 0.2);
  background: #ffffff;
}

.source-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.source-file {
  font-size: 0.8rem;
  color: #1f3f66;
  font-weight: 600;
}

.source-meta {
  font-size: 0.75rem;
  color: #4f6786;
  background: rgba(47, 111, 214, 0.1);
  border-radius: 999px;
  padding: 0.15rem 0.4rem;
}

.source-snippet {
  margin-top: 0.35rem;
  font-size: 0.75rem;
  color: #576b86;
  line-height: 1.45;
}

@media (max-width: 768px) {
  .message {
    .content {
      max-width: 100%;

      &.content-structured {
        max-width: 100%;

        .text-container {
          width: 100%;
        }
      }
    }
  }

  .markdown-content {
    :deep(h2) {
      font-size: 1rem;
    }

    :deep(table) {
      min-width: 100%;
    }

    :deep(th),
    :deep(td) {
      padding: 0.55rem 0.625rem;
      font-size: 0.875rem;
    }
  }
}
</style>


