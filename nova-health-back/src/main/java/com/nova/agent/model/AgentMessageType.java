package com.nova.agent.model;

/**
 * 智能体消息类型枚举
 */
public enum AgentMessageType {
    /**
     * 思考过程
     */
    THINKING,
    
    /**
     * 工具调用开始
     */
    TOOL_CALL_START,
    
    /**
     * 工具调用结果
     */
    TOOL_CALL_RESULT,
    
    /**
     * 最终回答
     */
    FINAL_ANSWER,
    
    /**
     * 错误信息
     */
    ERROR,
    
    /**
     * 步骤信息
     */
    STEP_INFO
}
