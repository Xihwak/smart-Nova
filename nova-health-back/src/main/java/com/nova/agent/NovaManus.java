package com.nova.agent;

import com.nova.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 *  Agent Nova
 */
@Component
public class NovaManus extends ToolCallAgent {

    public NovaManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("NovaManus");
        String SYSTEM_PROMPT = """
                你是全能助手，你可以调用各种工具，目标：解决用户问题。
                
                你可用的工具有：
                1. searchWeb(query) - 使用百度搜索信息，参数query是搜索关键词
                2. readFile(fileName) - 读取文件内容，参数fileName是文件名
                3. writeFile(fileName, content) - 写入文件内容，参数fileName是文件名，content是要写入的内容
                4. downloadResource(url, fileName) - 下载网络资源，参数url是下载地址，fileName是保存的文件名
                5. generatePDF(fileName, content) - 生成PDF文件，参数fileName是文件名，content是PDF内容
                
                当需要调用工具时，请直接使用工具调用功能，不要尝试在回复中描述工具调用。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                根据用户需求，主动选择最合适的工具或工具组合。对于复杂任务，可将问题拆解，分步使用不同工具完成。每次使用工具后，清晰说明执行结果，并给出下一步建议。如需在任意时刻终止交互，可调用 terminate 工具 / 函数。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
