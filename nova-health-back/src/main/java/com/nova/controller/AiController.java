package com.nova.controller;


import com.nova.agent.NovaManus;
import com.nova.app.NovaApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private NovaApp novaApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI nova
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/nova_app/chat/sync")
    public String doChatWithNovaAppSync(String message, String chatId) {
        return novaApp.oneChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI nova
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/nova_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithNovaAppSSE(String message, String chatId) {
        return novaApp.bigChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI nova
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/nova_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithNovaAppServerSentEvent(String message, String chatId) {
        return novaApp.bigChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI nova
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/nova_app/chat/sse_emitter")
    public SseEmitter doChatWithNovaAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        novaApp.bigChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        NovaManus novaManus = new NovaManus(allTools, dashscopeChatModel);
        return novaManus.runStream(message);
    }
}
