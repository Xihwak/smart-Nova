package com.nova.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.nova.agent.model.AgentMessage;
import com.nova.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 记录当前轮次调用过的工具（去重 + 保序）
    private final Set<String> usedToolNames = new LinkedHashSet<>();

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            
            // 发送思考消息
            if (StrUtil.isNotBlank(result)) {
                sendMessage(AgentMessage.thinking(result));
            }
            
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                setState(AgentState.FINISHED);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            sendMessage(AgentMessage.error("思考过程出错：" + e.getMessage()));
            return false;
        }
    }
    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        // 发送工具调用开始消息
        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        if (!toolCalls.isEmpty()) {
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                String toolName = toolCall.name();
                String toolArgs = toolCall.arguments();
                usedToolNames.add(toolName);
                sendMessage(AgentMessage.toolCallStart(toolName, toolArgs));
            }
        }

        // 执行工具调用
        ChatResponse chatResponse = getChatClient().prompt()
                .messages(getMessageList())
                .toolCallbacks(availableTools)
                .call()
                .chatResponse();
        // 获取结果
        Generation generation = chatResponse.getResult();
        org.springframework.ai.chat.messages.AssistantMessage responseAssistantMessage = generation.getOutput();
        // 记录上下文
        getMessageList().add(responseAssistantMessage);
        // 判断是否有工具调用
        List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> responseToolCalls = responseAssistantMessage.getToolCalls();
        // 如果没有工具调用，说明思考完成，返回结果
        if (responseToolCalls.isEmpty()) {
            setState(AgentState.FINISHED);
            // 发送最终回答
            sendMessage(AgentMessage.finalAnswer(responseAssistantMessage.getText(), new ArrayList<>(usedToolNames)));
            return responseAssistantMessage.getText();
        }
        // 否则，解析工具调用结果
        org.springframework.ai.chat.messages.ToolResponseMessage toolResponseMessage = 
            (org.springframework.ai.chat.messages.ToolResponseMessage) getMessageList().get(getMessageList().size() - 2);
        
        // 发送工具调用结果消息（简化版）
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> {
                    String toolName = response.name();
                    String summary = summarizeToolResult(toolName, response.responseData());
                    sendMessage(AgentMessage.toolCallResult(toolName, summary));
                    return "工具 " + toolName + " 返回的结果：" + response.responseData();
                })
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }
    
    /**
     * 简化工具返回结果，生成友好的摘要
     */
    private String summarizeToolResult(String toolName, String result) {
        try {
            // 如果是搜索工具，提取关键信息
            if (toolName.contains("search") || toolName.contains("Search")) {
                // 尝试解析JSON，提取标题
                if (result != null && result.startsWith("[")) {
                    // 是JSON数组，提取前3个结果的标题
                    return "搜索完成，已获取相关结果（共" + 
                           (result.split("\"").length / 10) + "条）";
                }
                return "搜索完成，已获取相关结果";
            }
            // 如果是网页抓取工具
            if (toolName.contains("scrape") || toolName.contains("Scrape")) {
                return "网页内容已抓取";
            }
            // 如果是文件操作工具
            if (toolName.contains("readFile")) {
                return "文件读取成功";
            }
            if (toolName.contains("writeFile")) {
                return "文件写入成功";
            }
            // 如果是下载工具
            if (toolName.contains("download") || toolName.contains("Download")) {
                return "资源下载成功";
            }
            // 如果是PDF生成工具
            if (toolName.contains("pdf") || toolName.contains("PDF")) {
                return "PDF生成成功";
            }
            // 默认返回前150个字符
            if (result != null && result.length() > 150) {
                return result.substring(0, 150) + "...";
            }
            return result != null ? result : "执行完成";
        } catch (Exception e) {
            return "执行完成";
        }
    }
}