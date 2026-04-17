<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

function goToChatPage() {
  router.push("/chat");
}
</script>

<template>
  <div class="home-page">
    <div class="home-card">
      <div class="home-brand">
        <span class="home-brand-mark" aria-hidden="true" />
        <span class="home-brand-text">Nova Health</span>
      </div>
      <h1>智能健康助手</h1>
      <p>
        双通道对话：左侧为智慧健康场景（NovaApp），右侧为智能体能力（Agent）。流式回复、独立会话，适合对照体验两种能力。
      </p>
      <div class="home-features">
        <div class="home-feature">
          <strong>智慧健康</strong>
          <span>面向健康问答与知识场景的流式对话</span>
        </div>
        <div class="home-feature">
          <strong>智能体 Agent</strong>
          <span>工具与任务导向的智能体流式输出</span>
        </div>
      </div>
      <button type="button" class="home-cta" @click="goToChatPage">进入应用</button>
    </div>
  </div>
</template>
