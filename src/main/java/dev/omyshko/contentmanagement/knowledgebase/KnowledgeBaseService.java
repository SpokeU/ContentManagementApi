package dev.omyshko.contentmanagement.knowledgebase;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.omyshko.contentmanagement.core.utils.ContentStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeBaseService {

    private final KnowledgeBaseInformationProvider kbInformationProvider;
    private final ChatLanguageModel chatLanguageModel;

    public KnowledgeBaseService(KnowledgeBaseInformationProvider kbInformationProvider, @Qualifier("openAiChatModel") ChatLanguageModel chatLanguageModel) {
        this.kbInformationProvider = kbInformationProvider;
        this.chatLanguageModel = chatLanguageModel;
    }

    public String getTableOfContent() {
        return kbInformationProvider.getTableOfContent();
    }

    public String getTopicsInfo(List<String> topicCodes) {
        List<String> topicsInfo = topicCodes.stream()
                .map(kbInformationProvider::getTopicInfo).toList();

        return joinTopics(topicsInfo);
    }

    /**
     * 1. UserQuery + TableOfContent = Extract Topic codes
     * 2. Extract full info for the topic by Topic codes
     */
    public String search(String userQuery) {
        String knowledgeBaseTableOfContent = getTableOfContent();

        TopicCodeExtractor topicCodeExtractor = AiServices.create(TopicCodeExtractor.class, chatLanguageModel);
        TopicCodeExtractor.KBExtractResult topicCodes = topicCodeExtractor.extractTopicCodes(userQuery, knowledgeBaseTableOfContent);

        return getTopicsInfo(topicCodes.topicCodes);
    }

    private String joinTopics(List<String> topicsInfo) {
        return topicsInfo.stream().map(t -> ContentStringUtils.wrap(t, "topic")).collect(Collectors.joining("\n"));
    }

}
