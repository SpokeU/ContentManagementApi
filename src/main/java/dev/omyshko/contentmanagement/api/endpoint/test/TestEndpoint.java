package dev.omyshko.contentmanagement.api.endpoint.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseService;
import dev.omyshko.contentmanagement.knowledgegraph.schema.BlockSchemaParser;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

@RestController
@RequestMapping("/test")
public class TestEndpoint {

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatLanguageModel chatLanguageModel;

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    @Value("${app.knowledge-base.path:}") String knowledgeBasePath;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseInformationProvider knowledgeBaseInformationProvider;

    @GetMapping("test_parse_using_kb")
    public ResponseEntity<JsonNode> testFileParseUsingKb(@RequestParam String filePath) throws IOException {
        //1. Search available Knowledge Base Graphs by subject KG (Knowledge Graph) based on File language and Main Frameworks
        Path pathToFile = Paths.get(filePath);
        FileType fileType = getFileType(pathToFile.getFileName().toString());
        String content = new String(Files.readAllBytes(pathToFile));

        //2. Get Blocks description from KB
        String classesContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_class.md");
        String methodsContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_method.md");

        JsonObjectSchema classesBlockSchema = new BlockSchemaParser().convert(classesContent);
        JsonObjectSchema methodsBlockSchema = new BlockSchemaParser().convert(methodsContent);

        JsonSchemaElement classesArraySchema = JsonArraySchema.builder()
                .description("All java classes that are declared in this file")
                .items(classesBlockSchema)
                .build();

        JsonSchemaElement methodsArraySchema = JsonArraySchema.builder()
                .description("All java methods declared in this file")
                .items(methodsBlockSchema)
                .build();

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("JavaLanguageBlocks") // OpenAI requires specifying the name for the schema
                .rootElement(JsonObjectSchema.builder()
                        .description("A list of blocks. A **block** is a unit of information with a specific purpose and a defined structure.") //TODO remove?
                        .addProperty("classes", classesArraySchema)
                        .addProperty("methods", methodsArraySchema)
                        .build()
                ).build();

        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(jsonSchema)
                .build();


        dev.langchain4j.data.message.SystemMessage systemMessage = dev.langchain4j.data.message.SystemMessage.from(
        "You are a tool for extracting blocks of code that are present in a provided file according to schema.");

        PromptTemplate promptTemplate = PromptTemplate.from("Here is the file content <file-content>{{it}}</file-content>");
        String apply = promptTemplate.apply(content).text();
        UserMessage userMessage = UserMessage.from(apply);


        ChatRequest chatRequest = ChatRequest.builder()
                .messages(systemMessage, userMessage)
                .responseFormat(responseFormat)
                .build();
        //3. Convert KB to JsonSchema
        //4. Pass File content + JsonSchema to LLM

        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();


        return ResponseEntity.ok(new ObjectMapper().readTree(output));
    }

    /**
     * Does not allow dynamic adding though KB. Bad approach
     * @param fileName
     * @return
     */
    private FileType getFileType(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return switch (extension) {
            case "java" -> FileType.JAVA;
            case "sql" -> FileType.SQL;
            case "ts" -> FileType.TYPESCRIPT;
            default -> throw new IllegalStateException("Unexpected value: " + extension);
        };
    }

    private enum FileType {
        JAVA, SQL, TYPESCRIPT
    }

    @GetMapping("test_pojo")
    public ResponseEntity<BlocksModelSchemas.JavaBlocks> testPojo() {
        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .build();

        BlocksModelSchemas.JavaBlocksExtractor blocksExtractor = AiServices.create(BlocksModelSchemas.JavaBlocksExtractor.class, chatModel);

        BlocksModelSchemas.JavaBlocks blocks = blocksExtractor.extractBlocks(fileContent);
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("test_2")
    public ResponseEntity<Blocks> test2() throws JsonProcessingException {
        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .build();

        BlocksExtractor blocksExtractor = AiServices.create(BlocksExtractor.class, chatModel);

        String kgJava = knowledgeBaseService.getTopicsInfo(List.of("KG_JAVA"));

        Blocks blocks = blocksExtractor.extractBlocks(fileContent, kgJava);
        return ResponseEntity.ok(blocks);
    }

    record Blocks(List<Block> blocks) {
    }

    @Description("A block of text")
    record Block(@Description("Id of this block") String id,
                 @Description("Name") String name,
                 @Description("Field values") List<Field> fields,
                 List<String> classifiers, List<Dependency> dependencies) {
    }

    record Dependency(@Description("Dependency type") String type, @Description("Dependency generated id") String id) {
    }


    record Field(@Description("Block field name") String name, @Description("Field values") List<String> value) {
    }


    interface BlocksExtractor {

        @SystemMessage("You are a tool for processing a code. Provided a file content please extract blocks. Available blocks are follows: {{available_blocks}}. Each block has its own set of dependencies and classifications. For example a class cannot have dependency type 'CALLS' because its owned by method")
        @dev.langchain4j.service.UserMessage("Here is the file content <file-content>{{file_content}}</file-content>")
        Blocks extractBlocks(@V("file_content") String fileContent, @V("available_blocks") String blocks);

    }

    public JsonSchema getClassBlockSchema() {
        JsonSchemaElement itemSchema = JsonEnumSchema.builder()
                .enumValues("Controller", "SpringBean", "Entity", "Repository")
                .description("Classifier is a Role or concept that specific code is using. For example: EntryPoint (user-triggered functionality like APIs or Jobs), Repository (data access layer), Entity (domain objects), or AngularComponent (UI component). If an entity does not follow any specific concept, it can be left unclassified or marked as RegularCode")
                .build();

        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(JsonSchema.builder()
                        .name("ClassBlock") // OpenAI requires specifying the name for the schema
                        .rootElement(JsonObjectSchema.builder() // see [1] below
                                .addEnumProperty("name", List.of("Class", "Method", "Imports"))
                                .addStringProperty("id", "Fully qualified method signature. e.g. `dev.omyshko.usermanagementsystem.service.UserService#getAllUsers()`")
                                .addStringProperty("className", "Fully qualified class name e.g. `dev.omyshko.contentmanagement.api.endpoint.KnowledgeBaseEndpoint`")
                                .addProperty("classifiers", JsonArraySchema.builder()
                                        .description("A list of classifiers ")
                                        .items(itemSchema)
                                        .build())
                                .addStringProperty("content", "Should include class along with constructor, package declaration, annotations and variables declarations or static blocks. Imports, Methods etc should be replaces with '...'. For example: ```java\n" +
                                                              "package dev.omyshko.contentmanagement.api.endpoint;\n" +
                                                              "\n" +
                                                              "@RequiredArgsConstructor\n" +
                                                              "@Slf4j\n" +
                                                              "@Component\n" +
                                                              "public class KnowledgeBaseEndpoint implements KnowledgeBaseApiDelegate {\n" +
                                                              "    private final NativeWebRequest request;\n" +
                                                              "    private final ObjectMapper objectMapper;\n" +
                                                              "    private final KnowledgeBaseService knowledgeBaseService;\n" +
                                                              "\n" +
                                                              "    public KnowledgeBaseEndpoint(NativeWebRequest request, ObjectMapper objectMapper, KnowledgeBaseService knowledgeBaseService) {\n" +
                                                              "        this.request = request;\n" +
                                                              "        this.objectMapper = objectMapper;\n" +
                                                              "        this.knowledgeBaseService = knowledgeBaseService;\n" +
                                                              "    }\n" +
                                                              "    \n" +
                                                              "    ...\n" +
                                                              "}\n" +
                                                              "```")
                                .required("name", "id", "className", "classifiers", "content") // see [2] below
                                .build())
                        .build())
                .build();

        return null;
    }

    String fileContent = """
            package dev.omyshko.usermanagementsystem.api.model;
                        
                        
            import dev.omyshko.usermanagementsystem.entity.UserEntity;
            import dev.omyshko.usermanagementsystem.service.UserService;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.*;
                        
            import java.util.List;
            import java.util.Optional;
            import java.util.stream.Collectors;
                        
            @RestController
            @RequestMapping("/users")
            public class UserController {
                        
                @Autowired
                private UserService userService;
                        
                @PostMapping
                public ResponseEntity<UserApiModel> createUser(@RequestBody UserEntity userEntity) {
                    UserEntity savedUser = userService.saveUser(userEntity);
                    return ResponseEntity.ok(toApiModel(savedUser));
                }
                        
                @GetMapping
                public ResponseEntity<List<UserApiModel>> getAllUsers() {
                    List<UserEntity> users = userService.getAllUsers();
                    List<UserApiModel> userApiModels = users.stream()
                            .map(this::toApiModel)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(userApiModels);
                }
                        
                @GetMapping("/{id}")
                public ResponseEntity<UserApiModel> getUserById(@PathVariable Long id) {
                    Optional<UserEntity> user = userService.getUserById(id);
                    return user.map(value -> ResponseEntity.ok(toApiModel(value)))
                            .orElseGet(() -> ResponseEntity.notFound().build());
                }
                        
                @DeleteMapping("/{id}")
                public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
                    userService.deleteUser(id);
                    return ResponseEntity.noContent().build();
                }
                        
                // Convert Entity to API Model
                private UserApiModel toApiModel(UserEntity userEntity) {
                    String fullName = userEntity.getFirstName() + " " + userEntity.getLastName();
                    return new UserApiModel(userEntity.getId(), fullName, userEntity.getEmail());
                }
            }
             
            """;

    @GetMapping("test_old")
    public ResponseEntity<String> test() throws JsonProcessingException {
        JsonSchemaElement itemSchema = JsonEnumSchema.builder()
                .enumValues("Controller", "SpringBean", "Entity", "Repository")
                .description("Classifier is a Role or concept that specific code is using. For example: EntryPoint (user-triggered functionality like APIs or Jobs), Repository (data access layer), Entity (domain objects), or AngularComponent (UI component). If an entity does not follow any specific concept, it can be left unclassified or marked as RegularCode")
                .build();

        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(JsonSchema.builder()
                        .name("Block") // OpenAI requires specifying the name for the schema
                        .rootElement(JsonObjectSchema.builder() // see [1] below
                                .addEnumProperty("name", List.of("Class", "Method", "Imports"))
                                .addStringProperty("id", "Fully qualified method signature. e.g. `dev.omyshko.usermanagementsystem.service.UserService#getAllUsers()`")
                                .addStringProperty("className", "Fully qualified class name e.g. `dev.omyshko.contentmanagement.api.endpoint.KnowledgeBaseEndpoint`")
                                .addProperty("classifiers", JsonArraySchema.builder()
                                        .description("A list of classifiers ")
                                        .items(itemSchema)
                                        .build())
                                .addStringProperty("content", "Should include class along with constructor, package declaration, annotations and variables declarations or static blocks. Imports, Methods etc should be replaces with '...'. For example: ```java\n" +
                                                              "package dev.omyshko.contentmanagement.api.endpoint;\n" +
                                                              "\n" +
                                                              "@RequiredArgsConstructor\n" +
                                                              "@Slf4j\n" +
                                                              "@Component\n" +
                                                              "public class KnowledgeBaseEndpoint implements KnowledgeBaseApiDelegate {\n" +
                                                              "    private final NativeWebRequest request;\n" +
                                                              "    private final ObjectMapper objectMapper;\n" +
                                                              "    private final KnowledgeBaseService knowledgeBaseService;\n" +
                                                              "\n" +
                                                              "    public KnowledgeBaseEndpoint(NativeWebRequest request, ObjectMapper objectMapper, KnowledgeBaseService knowledgeBaseService) {\n" +
                                                              "        this.request = request;\n" +
                                                              "        this.objectMapper = objectMapper;\n" +
                                                              "        this.knowledgeBaseService = knowledgeBaseService;\n" +
                                                              "    }\n" +
                                                              "    \n" +
                                                              "    ...\n" +
                                                              "}\n" +
                                                              "```")
                                .required("name", "id", "className", "classifiers", "content") // see [2] below
                                .build())
                        .build())
                .build();

        UserMessage userMessage = UserMessage.from("""              
                """);

        ChatRequest chatRequest = ChatRequest.builder()
                .responseFormat(responseFormat)
                .messages(userMessage)
                .build();

        ChatResponse chatResponse = chatLanguageModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();
        System.out.println(output); // {"name":"John","age":42,"height":1.75,"married":false}

        JsonNode person = new ObjectMapper().readTree(output);
        System.out.println(person); // Person[name=John, age=42, height=1.75, married=false]
        return ResponseEntity.ok("Ok");
    }


}
