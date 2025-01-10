package dev.omyshko.contentmanagement.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.openai.spring.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Configuration
public class AIConfig {

    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Primary
    @Bean(name = "chatModel")
    public ChatLanguageModel chatLanguageModel(Properties properties) {
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();

        return model;
    }

    //Not working yet
    @Bean(name = "geminiJsonObjectModel")
    public ChatLanguageModel geminiJsonObjectModel(Properties properties) {
        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder() // see [1] below
                .apiKey(geminiApiKey)
                .modelName("gemini-1.5-flash")
                .responseFormat(ResponseFormat.JSON) // see [2] below
                .logRequestsAndResponses(true)
                .temperature(0.1)
                .build();

        return chatModel;
    }

    @Bean(name = "geminiJsonSchemaModel")
    public ChatLanguageModel geminiJsonSchemaModel(Properties properties) {
        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder() // see [1] below
                .apiKey(geminiApiKey)
                .modelName("gemini-1.5-flash")
                .responseFormat(ResponseFormat.JSON) // see [2] below
                .logRequestsAndResponses(true)
                .temperature(0.1)
                .build();

        return chatModel;
    }

    @Bean(name = "openAIJsonSchemeModel")
    public ChatLanguageModel openAiJsonSchemaModel(Properties properties) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(openAiApiKey)
                .modelName(GPT_4_O_MINI)
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .build();

        return chatModel;
    }

    @Bean(name = "openAIJsonObjectModel")
    public ChatLanguageModel openAiJsonObjectModel(Properties properties) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(openAiApiKey)
                .modelName(GPT_4_O_MINI)
                .responseFormat("json_object") // see [2] below
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .build();

        return chatModel;
    }

}
