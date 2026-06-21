const BASE_URL = import.meta.env.VITE_API_BASE_URL || window.location.origin

const normalizeDateValue = (value) => {
    if (!value) return null
    if (value instanceof Date) return value
    if (typeof value === 'number') return new Date(value)
    if (typeof value === 'string') {
        const normalized = value.includes('T') ? value : value.replace(' ', 'T')
        const date = new Date(normalized)
        return Number.isNaN(date.getTime()) ? null : date
    }
    return null
}

// 获取 token（仅新格式：{ data, expiration }）
const getToken = () => {
  try {
    const storedData = localStorage.getItem('userInfo')
    if (storedData) {
      const { data, expiration } = JSON.parse(storedData)
      if (data?.token && (!expiration || new Date().getTime() < expiration)) {
        return data.token
      }
    }
  } catch (error) {
    console.error('获取token失败', error)
  }
  return ''
}

const parseRagHeaders = (headers) => {
    const hitHeader = headers.get('X-RAG-HIT')
    const hitCountHeader = headers.get('X-RAG-HIT-COUNT')
    const hit = hitHeader === null ? null : hitHeader === '1'
    const hitCount = Number(hitCountHeader || 0)
    const encodedSources = headers.get('X-RAG-SOURCES') || ''
    const decoded = encodedSources ? decodeURIComponent(encodedSources) : ''
    const sources = decoded
        ? decoded.split('||').filter(Boolean).map(item => {
            const [fileName, chunkIndex, score, snippet] = item.split('|')
            return {
                fileName: fileName || '未知来源',
                chunkIndex: chunkIndex ? Number(chunkIndex) : null,
                score: score ? Number(score) : null,
                snippet: snippet || ''
            }
        })
        : []

    return {
        available: hitHeader !== null || hitCountHeader !== null || !!encodedSources,
        hit,
        hitCount,
        sources
    }
}

export const chatAPI = {
    // 发送聊天消息
    async sendMessage(data, chatId) {
        try {
            const url = new URL(`${BASE_URL}/api/v1/agent/sence`)
            if (chatId) {
                url.searchParams.append('chatId', chatId)
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': getToken()
                },
                body: data instanceof FormData ? data :
                    new URLSearchParams({ prompt: data })
            })

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }

            return {
                reader: response.body.getReader(),
                retrieval: parseRagHeaders(response.headers)
            }
        } catch (error) {
            console.error('API Error:', error)
            throw error
        }
    },

    // 获取聊天历史列表
    async getChatHistory(type = null) {
        try {
            const url = new URL(`${BASE_URL}/api/v1/agent/session/list`)
            if (type) {
                url.searchParams.append('sessionType', type)
            }
            const response = await fetch(url, {
                headers: {
                    'Authorization': getToken()
                }
            })
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            const result = await response.json()
            if (result.code === 200 && result.data) {
                return result.data.map(session => ({
                    id: session.conversationId,
                    title: session.title || `${session.sessionType === 'pdf' ? '文档' : '对话'} ${session.conversationId.slice(-6)}`,
                    createTime: session.createTime,
                    sessionType: session.sessionType,
                    status: session.status,
                    messageCount: session.messageCount || 0
                }))
            }
            return []
        } catch (error) {
            console.error('API Error:', error)
            return []
        }
    },

    // 获取特定对话的消息历史
    async getChatMessages(chatId, type = 'chat') {
        try {
            const response = await fetch(`${BASE_URL}/api/v1/agent/${type}/${chatId}`, {
                headers: {
                    'Authorization': getToken()
                }
            })
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            const messages = await response.json()

            const orderedMessages = Array.isArray(messages) ? messages : []
            const baseTime = Date.now()

            return orderedMessages.map((msg, index) => {
                const timestamp =
                    normalizeDateValue(msg.timestamp) ||
                    normalizeDateValue(msg.createTime) ||
                    new Date(baseTime + index)

                return {
                    ...msg,
                    id: msg.id || `${chatId}-${index}-${msg.role || 'message'}`,
                    timestamp,
                    attachments: Array.isArray(msg.attachments) ? msg.attachments : []
                }
            })
        } catch (error) {
            console.error('API Error:', error)
            return []
        }
    },

    // 删除会话（新增）
    async deleteSession(chatId) {
        try {
            const response = await fetch(`${BASE_URL}/api/v1/agent/session/${chatId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': getToken(),
                    'Content-Type': 'application/json'
                }
            })
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            const result = await response.json()
            return result.code === 200 && result.data === true
        } catch (error) {
            console.error('API Error:', error)
            return false
        }
    },

    // 创建新会话（新增）
    async createSession(title = '新对话', type = 'chat') {
        try {
            const response = await fetch(`${BASE_URL}/api/v1/agent/session`, {
                method: 'POST',
                headers: {
                    'Authorization': getToken(),
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ title, sessionType: type })
            })
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            const result = await response.json()
            if (result.code === 200 && result.data) {
                const createTime = result.data.createTime || new Date().toISOString()
                return {
                    id: result.data.conversationId,
                    title: result.data.title || title,
                    createTime,
                    sessionType: result.data.sessionType || type,
                    status: result.data.status || 'ACTIVE',
                    messageCount: result.data.messageCount || 0
                }
            }
            return null
        } catch (error) {
            console.error('API Error:', error)
            return null
        }
    },

    // 更新会话标题（新增）
    async updateSessionTitle(chatId, title) {
        try {
            const response = await fetch(`${BASE_URL}/api/v1/agent/session`, {
                method: 'PUT',
                headers: {
                    'Authorization': getToken(),
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ conversationId: chatId, title })
            })
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`)
            }
            const result = await response.json()
            return result.code === 200
        } catch (error) {
            console.error('API Error:', error)
            return false
        }
    }
}

