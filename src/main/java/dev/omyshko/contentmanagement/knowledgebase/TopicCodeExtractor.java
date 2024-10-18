package dev.omyshko.contentmanagement.knowledgebase;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.Data;

import java.util.List;

public interface TopicCodeExtractor {

    @SystemMessage("""
            You are provided with knowledge base
            Your task is to extract all Topic Codes that relates to a user message.
            {{knowledgeBaseTableOfContent}}.
            "
            """)
    @UserMessage("{{userMessage}}")
    KBExtractResult extractTopicCodes(@V("userMessage") String userMessage, @V("knowledgeBaseTableOfContent") String knowledgeBaseTableOfContent);

    @Data
    public static class KBExtractResult {
        List<String> topicCodes;
    }

}
