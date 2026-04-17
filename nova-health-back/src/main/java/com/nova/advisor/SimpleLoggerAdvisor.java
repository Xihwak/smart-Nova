//package com.nova.advisor;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.*;
//import org.springframework.ai.chat.client.advisor.api.*;
//import reactor.core.publisher.Flux;
//
//@Slf4j
//public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
//
//    @Override
//    public String getName() {
//        return this.getClass().getSimpleName();
//    }
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//
//
//    @Override
//    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
//        logRequest(chatClientRequest);
//
//        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
//
//        logResponse(chatClientResponse);
//
//        return chatClientResponse;
//    }
//
//    @Override
//    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
//                                                 StreamAdvisorChain streamAdvisorChain) {
//        logRequest(chatClientRequest);
//
//        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
//
//        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
//    }
//
//    private void logRequest(ChatClientRequest request) {
//        log.debug("request: {}", request);
//    }
//
//    private void logResponse(ChatClientResponse chatClientResponse) {
//        log.debug("response: {}", chatClientResponse);
//    }
//
//}
