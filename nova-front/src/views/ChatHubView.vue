<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import ChatPanel from "../components/ChatPanel.vue";

const chatId = computed(() => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`;
});
</script>

<template>
  <div class="chat-hub-page">
    <div class="chat-shell">
      <header class="chat-topbar">
        <div class="chat-topbar-title">
          <h1>双通道对话</h1>
          <p>左右独立会话，互不影响</p>
        </div>
        <RouterLink to="/" class="chat-back">
          <span class="chat-back-icon" aria-hidden="true">←</span>
          返回首页
        </RouterLink>
      </header>
      <div class="split-layout">
        <ChatPanel
          variant="health"
          title="AI 智慧健康应用（NovaApp）"
          endpoint="/ai/nova_app/chat/sse"
          :chat-id="chatId"
          :include-chat-id="true"
        />
        <ChatPanel
          variant="agent"
          title="AI 智能体应用（Agent）"
          endpoint="/ai/manus/chat"
          :stream-chunk-as-bubble="true"
        />
      </div>
    </div>
  </div>
</template>
