package com.nova.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    /**
     * 消息类型
     */
    private AgentMessageType type;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private Object content;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 创建思考消息
     */
    public static AgentMessage thinking(String content) {
        return new AgentMessage(AgentMessageType.THINKING, "思考中...", content, System.currentTimeMillis());
    }
    
    /**
     * 创建工具调用开始消息
     */
    public static AgentMessage toolCallStart(String toolName, String arguments) {
        return new AgentMessage(AgentMessageType.TOOL_CALL_START, "🔧 调用工具: " + toolName, arguments, System.currentTimeMillis());
    }
    
    /**
     * 创建工具调用结果消息
     */
    public static AgentMessage toolCallResult(String toolName, Object result) {
        return new AgentMessage(AgentMessageType.TOOL_CALL_RESULT, "✅ 工具 " + toolName + " 执行完成", result, System.currentTimeMillis());
    }
    
    /**
     * 创建最终回答消息
     */
    public static AgentMessage finalAnswer(String content) {
        return new AgentMessage(AgentMessageType.FINAL_ANSWER, "回答", content, System.currentTimeMillis());
    }

    /**
     * 创建包含工具调用信息的最终回答消息
     */
    public static AgentMessage finalAnswer(String content, List<String> tools) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answer", content);
        payload.put("tools", tools);
        return new AgentMessage(AgentMessageType.FINAL_ANSWER, "回答", payload, System.currentTimeMillis());
    }
    
    /**
     * 创建错误消息
     */
    public static AgentMessage error(String content) {
        return new AgentMessage(AgentMessageType.ERROR, "错误", content, System.currentTimeMillis());
    }
    
    /**
     * 创建步骤信息消息
     */
    public static AgentMessage stepInfo(int currentStep, int maxSteps) {
        return new AgentMessage(AgentMessageType.STEP_INFO, "步骤 " + currentStep + "/" + maxSteps, null, System.currentTimeMillis());
    }
}
