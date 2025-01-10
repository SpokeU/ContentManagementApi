package dev.omyshko.contentmanagement.api.endpoint.test;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.source.FileSystemSource;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.output.ServiceOutputParser;
import dev.omyshko.contentmanagement.core.utils.ContentUtils;
import dev.omyshko.contentmanagement.knowledgegraph.schema.JavaClassSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestModelsEndpoint {

    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();

    private final ChatLanguageModel geminiJsonSchemaModel;

    private final ChatLanguageModel openAiJsonObjectModel;
    private final ChatLanguageModel openAiJsonSchemaModel;

    public TestModelsEndpoint(@Qualifier("geminiJsonSchemaModel") ChatLanguageModel geminiJsonSchemaModel,
                              @Qualifier("openAIJsonObjectModel") ChatLanguageModel openAiJsonObjectModel,
                              @Qualifier("openAIJsonSchemeModel") ChatLanguageModel openAiJsonSchemaModel) {
        this.openAiJsonObjectModel = openAiJsonObjectModel;
        this.openAiJsonSchemaModel = openAiJsonSchemaModel;
        this.geminiJsonSchemaModel = geminiJsonSchemaModel;
    }

    @PostMapping("test_schema")
    public JavaClassSchema.JavaClassBlocks testBuildSchema(@RequestBody FileProcessingRequest fileProcessingRequest) throws IOException {
        String contentUri = fileProcessingRequest.filePath();

        Path pathToFile = Paths.get(contentUri);
        Document load = DocumentLoader.load(FileSystemSource.from(pathToFile), new TextDocumentParser());

        String contentWithLines = ContentUtils.getLocalContentWithLineNumbers(pathToFile);
        JavaClassSchema.JavaClassBlocksExtractor blocksExtractor = AiServices.create(JavaClassSchema.JavaClassBlocksExtractor.class, geminiJsonSchemaModel);

        JavaClassSchema.JavaClassBlocks blocks = blocksExtractor.extractBlocks(contentWithLines);

        return blocks;
    }

    @PostMapping("test_json_mode")
    public JavaClassSchema.JavaClassBlocks testJsonMode(@RequestBody FileProcessingRequest fileProcessingRequest) throws IOException {
        String contentUri = fileProcessingRequest.filePath();

        Path pathToFile = Paths.get(contentUri);
        String contentWithLines = ContentUtils.getLocalContentWithLineNumbers(pathToFile);
        JavaClassSchema.JavaClassBlocksExtractor blocksExtractor = AiServices.create(JavaClassSchema.JavaClassBlocksExtractor.class, openAiJsonObjectModel);
        JavaClassSchema.JavaClassBlocks blocks = blocksExtractor.extractBlocks(contentWithLines);

        serviceOutputParser.outputFormatInstructions(JavaClassSchema.JavaClassBlocks.class);

        return blocks;
    }

    private record FileProcessingRequest(String filePath) {
    }

}
