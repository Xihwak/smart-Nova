package com.nova.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
public class TerminateTool {

    @Tool(description = """
            当请求已经满足或者助手无法继续执行任务时，终止互动。当你完成所有任务后，调用此工具来结束工作。
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
