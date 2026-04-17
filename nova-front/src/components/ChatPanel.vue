<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { buildSseUrl } from "../api/chat";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  endpoint: {
    type: String,
    required: true,
  },
  chatId: {
    type: String,
    default: "",
  },
  includeChatId: {
    type: Boolean,
    default: false,
  },
  variant: {
    type: String,
    default: "health",
    validator: (v) => ["health", "agent"].includes(v),
  },
  streamChunkAsBubble: {
    type: Boolean,
    default: false,
  },
});

const inputText = ref("");
const messages = ref([]);
const sending = ref(false);
const listRef = ref(null);
let eventSource = null;
let stoppedByUser = false;
let hasReceivedChunkInCurrentTurn = false;

function pushMessage(role, content, streaming = false) {
  messages.value.push({
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    streaming,
  });
}

function upsertAssistantMessage(content, append = false) {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.role === "assistant") {
    lastMessage.content = append ? `${lastMessage.content || ""}${content}` : content;
    return;
  }
  addMessage(content, "assistant");
}

async function scrollToBottom() {
  await nextTick();
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight;
  }
}

function closeStream() {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  sending.value = false;
}

function markCurrentAiMessageStopped() {
  if (props.streamChunkAsBubble) {
    pushMessage("assistant", "已停止生成。");
    return;
  }

  const currentAiMsg = messages.value[messages.value.length - 1];
  if (!currentAiMsg || currentAiMsg.role !== "assistant") return;

  if (!currentAiMsg.content) {
    currentAiMsg.content = "已停止生成。";
  } else {
    currentAiMsg.content += "\n\n（已手动停止）";
  }
  currentAiMsg.streaming = false;
}

function stopMessage() {
  if (!sending.value) return;
  stoppedByUser = true;
  markCurrentAiMessageStopped();
  closeStream();
}

// 添加消息
const addMessage = (content, role = 'user', messageClass = '') => {
  messages.value.push({
    id: Date.now(),
    role,
    content,
    messageClass
  });
};

// 处理结构化消息（仅展示最终回答，避免出现“思考框 + 回答框”）
const handleStructuredMessage = (message) => {
  const type = message.type;
  const title = message.title;
  const content = message.content;

  if (type === "FINAL_ANSWER") {
    if (content && typeof content === "object" && !Array.isArray(content)) {
      const answer = content.answer || "";
      const tools = Array.isArray(content.tools) ? content.tools.filter(Boolean) : [];
      const toolsText = tools.length ? `\n\n🧰 本次调用工具：${tools.join("、")}` : "";
      upsertAssistantMessage(`${answer}${toolsText}`);
    } else {
      upsertAssistantMessage(`${content || ""}`);
    }
    return;
  }

  if (type === "ERROR") {
    upsertAssistantMessage(`❌ ${title}\n${content || ""}`);
    return;
  }

  // THINKING / TOOL_CALL_START / TOOL_CALL_RESULT / STEP_INFO 不单独展示
};

// 累积数据块到最后一条助手消息
const accumulateChunk = (chunk) => {
  if (messages.value.length > 0) {
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage.role === 'assistant') {
      lastMessage.content += chunk;
    } else {
      addMessage(chunk, 'assistant');
    }
  } else {
    addMessage(chunk, 'assistant');
  }
};

function sendMessage() {
  if (sending.value) return;
  stoppedByUser = false;
  hasReceivedChunkInCurrentTurn = false;
  const text = inputText.value.trim();
  if (!text) return;

  pushMessage("user", text);
  inputText.value = "";
  if (!props.streamChunkAsBubble) {
    pushMessage("assistant", "", true);
  }
  sending.value = true;
  scrollToBottom();

  const params = { message: text };
  if (props.includeChatId && props.chatId) {
    params.chatId = props.chatId;
  }

  eventSource = new EventSource(buildSseUrl(props.endpoint, params));

  // 处理 SSE 消息
  eventSource.onmessage = (event) => {
    const chunk = event.data;
    if (chunk) {
      if (props.streamChunkAsBubble) {
        // 尝试解析为结构化消息
        try {
          const message = JSON.parse(chunk);
          if (message.type && message.title && message.timestamp) {
            // 结构化消息，友好展示
            handleStructuredMessage(message);
          } else {
            // 普通文本消息
            upsertAssistantMessage(chunk, true);
          }
        } catch (e) {
          // JSON 解析失败，作为普通文本处理
          upsertAssistantMessage(chunk, true);
        }
      } else {
        // 累积数据块到最后一条助手消息
        accumulateChunk(chunk);
      }
    }
  };

  eventSource.onerror = () => {
    if (props.streamChunkAsBubble) {
      if (!stoppedByUser && !hasReceivedChunkInCurrentTurn) {
        pushMessage("assistant", "连接中断，请稍后重试。");
      }
      closeStream();
      return;
    }

    const currentAiMsg = messages.value[messages.value.length - 1];
    if (!stoppedByUser && currentAiMsg && currentAiMsg.role === "assistant" && currentAiMsg.content === "") {
      currentAiMsg.content = "连接中断，请稍后重试。";
    }
    if (!stoppedByUser && currentAiMsg && currentAiMsg.role === "assistant") {
      currentAiMsg.streaming = false;
    }
    closeStream();
  };
}

onBeforeUnmount(() => {
  closeStream();
});

/**
 * 格式化消息内容，支持HTML渲染
 */
function formatMessageContent(content) {
  if (!content) return ''
  
  // 将换行符转换为<br>
  let formatted = content.replace(/\n/g, '<br>')
  
  // 格式化下载链接
  formatted = formatDownloadLinks(formatted)
  
  return formatted
}

/**
 * 格式化下载链接，将URL转换为可点击的下载按钮
 */
function formatDownloadLinks(text) {
  if (!text) return ''
  
  // 匹配下载链接格式：/api/files/download/{type}/{filename}
  const downloadRegex = /(Download URL: )?(\/api\/files\/download\/(pdf|file|resource)\/([^\s,，。！！?？]+))/g
  
  return text.replace(downloadRegex, (match, prefix, url, type, filename) => {
    // 返回带有点击事件的链接
    return `<a href="javascript:void(0)" class="download-link" onclick="window.downloadFile('${url}', '${filename}')">📥 下载 ${filename}</a>`
  })
}

onMounted(() => {
  // 添加全局下载函数
  window.downloadFile = (url, filename) => {
    // 创建隐藏的下载链接
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
})

</script>

<template>
  <div class="chat-panel" :class="variant === 'agent' ? 'chat-panel--agent' : 'chat-panel--health'">
    <div class="panel-header">
      <h2>{{ title }}</h2>
      <span v-if="includeChatId" class="chat-id">聊天室 ID：{{ chatId }}</span>
    </div>

    <div ref="listRef" class="message-list">
      <div v-if="messages.length === 0" class="empty-chat">
        <span class="empty-chat-icon" aria-hidden="true" />
        在下方输入问题，回复将以流式方式显示。<br />
        生成过程中可随时点击「停止」。
      </div>
      <div
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="['message', message.role, message.messageClass]"
      >
        <div class="message-stack">
          <span class="bubble-role" :class="message.role">{{ message.role === "user" ? "👤 我" : "🤖 助手" }}</span>
          <div class="message-bubble" v-html="formatMessageContent(message.content)"></div>
        </div>
      </div>
    </div>

    <div class="input-box">
      <input
        v-model="inputText"
        type="text"
        placeholder="请输入你的问题…"
        autocomplete="off"
        @keydown.enter="sendMessage"
      />
      <div class="input-actions">
        <button type="button" class="btn-send" :disabled="sending" @click="sendMessage">
          {{ sending ? "生成中" : "发送" }}
        </button>
        <button type="button" class="stop-btn" :disabled="!sending" @click="stopMessage">停止</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.message {
  margin-bottom: 1rem;
  padding: 0.75rem;
  border-radius: 0.5rem;
}

.message.user {
  background-color: #e3f2fd;
  margin-left: 2rem;
}

.message.assistant {
  background-color: #f5f5f5;
  margin-right: 2rem;
}

/* 思考消息 */
.message.thinking-message {
  background-color: #fff3e0;
  border-left: 3px solid #ff9800;
  font-style: italic;
  color: #666;
  font-size: 0.9rem;
}

/* 工具调用消息 */
.message.tool-message {
  background: linear-gradient(to right, #e8f5e9, #f1f8e9);
  border-left: 4px solid #4caf50;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  white-space: pre-wrap;
  box-shadow: 0 2px 4px rgba(76, 175, 80, 0.1);
}

/* 工具调用消息中的参数列表 */
.message.tool-message :deep(br) {
  line-height: 1.8;
}

/* 工具结果消息 */
.message.tool-result-message {
  background-color: #f1f8e9;
  border-left: 4px solid #66bb6a;
  font-size: 0.9rem;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
  box-shadow: 0 2px 4px rgba(102, 187, 106, 0.1);
}

/* 最终回答消息 */
.message.final-answer-message {
  background: linear-gradient(to right, #e3f2fd, #e1f5fe);
  border-left: 4px solid #2196f3;
  font-size: 1rem;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 错误消息 */
.message.error-message {
  background-color: #ffebee;
  border-left: 3px solid #f44336;
}

/* 步骤信息消息 */
.message.step-info-message {
  background-color: #f3e5f5;
  border-left: 3px solid #9c27b0;
  font-size: 0.85rem;
  color: #7b1fa2;
  padding: 4px 12px;
  margin: 4px 0;
}

.message-content {
  white-space: pre-wrap;
}

.bubble-role {
  font-size: 0.75rem;
  color: #999;
  margin-bottom: 4px;
  display: block;
}

.bubble-role.user {
  color: #1976d2;
}

.bubble-role.assistant {
  color: #388e3c;
}

.input-area {
  padding: 1rem;
  border-top: 1px solid #e0e0e0;
}

.input-area textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 0.25rem;
  resize: vertical;
}

.input-area button {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
}

.input-area button:hover {
  background-color: #1565c0;
}

.input-area button:disabled {
  background-color: #bdbdbd;
  cursor: not-allowed;
}

.message-error {
  background-color: #fff5f5;
  border-left: 3px solid #f56565;
  padding: 8px 12px;
  border-radius: 4px;
}

/* 下载链接样式 */
:deep(.download-link) {
  display: inline-block;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 8px 16px;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 500;
  margin: 4px 0;
  transition: all 0.3s ease;
  cursor: pointer;
}

:deep(.download-link:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

:deep(.download-link:active) {
  transform: translateY(0);
}
</style>