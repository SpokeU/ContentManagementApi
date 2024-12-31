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
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseService;
import dev.omyshko.contentmanagement.knowledgegraph.schema.BlockSchemaParser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestEndpoint {

    public static final String DB_URI = "neo4j+s://6a2ca604.databases.neo4j.io";
    public static final String DB_USER = "neo4j";
    public static final String DB_PASSWORD = "";

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    @Value("${app.knowledge-base.path:}")
    String knowledgeBasePath;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseInformationProvider knowledgeBaseInformationProvider;

    private Driver driver;

    @PostConstruct
    public void init() {
        driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD));
    }

    @GetMapping("test_build_schema")
    public ResponseEntity<JsonNode> testBuildSchema() throws IOException {
        //Описання структури не вникаючи в блоки!!!
        //
        //2. Get Blocks description from KB
        String methodsContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_method.md");
        JsonObjectSchema methodsBlockSchema = new BlockSchemaParser().convert(methodsContent);
        return ResponseEntity.ok(null);
    }

    private String prependLineNumbers(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        StringBuilder result = new StringBuilder();
        AtomicInteger lineNumber = new AtomicInteger(1);

        lines.forEach(line -> {
            // Print line with its number
            result.append(String.format("%03d: %s%n", lineNumber.getAndIncrement(), line));
        });

        return result.toString();
    }


    @GetMapping("test_parse_using_kb")
    public ResponseEntity<JsonNode> testFileParseUsingKb(@RequestParam String filePath) throws IOException {
        //TODO Choose model based on file size? Larger files extraction doing better on 4o while smaller blocks can be used with 4o-mini
        //I'm changing approach to HOW TO ANALYZE <BLOCK_NAME>

        //1. Split file into root blocks
            //What to extract from java FILE ? = (Node:java_class {id: $id}) - [edge:$declares_method] > (Node:depepdency.name {id:$depepdency.method_signature} ),

        //FOR rootBlock = rootBlocks -- Iterating though root entities.
        //In case of java it will be single java class with relations to imports methods etc.
        //But in typescript you can have multiple classes in one file

            // 2. Split child blocks further recursively
            // Iterate though root blocks dependencies (imports, methods, inner_classes etc. ) and split those recursively according to KB page of block name
                // Expand content according line numbers




        //1. Search available Knowledge Base Graphs by subject KG (Knowledge Graph) based on File language and Main Frameworks
        Path pathToFile = Paths.get(filePath);
        FileType fileType = getFileType(pathToFile.getFileName().toString());
        //String content = new String(Files.readAllBytes(pathToFile));
        String content = prependLineNumbers(pathToFile);

        //2. Get Blocks description from KB
        String classesContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_class_lines.md");
        String methodsContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_method_lines.md");

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
                        .addProperty("declared_classes", classesArraySchema)
                        //.addProperty("declared_methods", methodsArraySchema) TODO methods
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
                .modelName(GPT_4_O)
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();


        JsonNode response = new ObjectMapper().readTree(output);

        //storeBlockToNeo4J(response);


        return ResponseEntity.ok(response);
    }

    @GetMapping("/describeGraph")
    public ResponseEntity<String> describeGraphEndpoint() {
        describeGraph();
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/describeNode")
    public ResponseEntity<String> describeNodndpoint(@RequestParam String id) {
        Node node = getNodeById(id);
        String description = describeNode(node);
        return ResponseEntity.ok(description);
    }

    private void describeGraph() {
        //getTerminalNodes. //TODO Find terminal nodes for ID if we want to describe specific controller
        //Describe each
        //store description result into node
        //Iterate and get closes siblings and repeat

        try (var driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD))) {
            driver.verifyConnectivity();
            Session session = driver.session();

            List<Node> terminalNodes = session.executeRead(tx -> getTerminalNodes(tx));

            for (Node terminalNode : terminalNodes) {
                describeNode(terminalNode);
            }
            log.info("terminalNodes: {}", terminalNodes);

        }
    }

    private String describeNode(Node node) {
        //What do I need to do to describe java_method?

        //Get java class where its declared and wrap class around a method
        //Get Input parameters
        //Get Output parameters
        //TODO If its not terminal then get everything that it calls

        String declaration_class_query = "MATCH (N)<-[G:declares]-(M:java_class) WHERE N.id = $id RETURN M";
        List<Node> declaring_java_class_node = queryNodes(declaration_class_query, Map.of("id", node.get("id").asString()));


        return "";
    }

    private List<Node> getTerminalNodes(TransactionContext tx) {
        String query = "MATCH (N)\n" +
                       "WHERE NOT EXISTS { MATCH (n)-->() }\n" +
                       "RETURN N";
        Result result = tx.run(query);
        List<Node> nodes = new ArrayList<>();
        while (result.hasNext()) {
            nodes.add(result.next().get("n").asNode());
        }
        return nodes;
    }

    private Node getNodeById(String id) {
        String query = "MATCH (N) WHERE N.id = $id RETURN N";
        return queryNodes(query, Map.of("id", id)).get(0);
    }

    private List<Node> queryNodes(String query, Map<String, Object> parameters) {
        List<Node> nodes = driver.session().executeRead(tx -> {
            Result result = tx.run(query, parameters);
            List<Node> nodesResult = new ArrayList<>();
            while (result.hasNext()) {
                Record next = result.next();
                List<Node> list = next.values().stream().map(v -> v.asNode()).toList();
                nodesResult.addAll(list);
            }
            return nodesResult;
        });

        return nodes;
    }


    private void storeBlockToNeo4J(JsonNode jsonNode) {
        // URI examples: "neo4j://localhost", "neo4j+s://xxx.databases.neo4j.io"

        try (var driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD))) {
            driver.verifyConnectivity();
            Session session = driver.session();
            createNode(session, jsonNode);
        }
    }

    private void createNode(Session session, JsonNode jsonNode) {
        for (JsonNode blocksArrays : jsonNode) {
            for (JsonNode block : blocksArrays) {
                storeBlock(session, block);
            }
        }
    }

    private void storeBlock(Session session, JsonNode node) {
        String id = node.get("id").asText();
        String name = node.get("name").asText();
        Map<String, List<String>> fields = extractFieldsAndValues(node, "fields", null);
        Map<String, List<String>> dependencies = extractFieldsAndValues(node, "dependencies", null);

        //Create node for block
        createNode(session, id, name, fields, dependencies);


        //Handle dependencies
        for (Map.Entry<String, List<String>> dependency : dependencies.entrySet()) {
            String dependencyName = dependency.getKey();

            for (String dependencyValue : dependency.getValue()) {
                if (dependencyName.contains("calls")) {
                    String[] classAndMethod = dependencyValue.split("#");

                    //Create java_class
                    createNode(session, classAndMethod[0], "java_class", Map.of());

                    //Create java_method
                    createNode(session, dependencyValue, "java_method", Map.of());

                    //Make relation between those (java_class -> declares -> java_method)
                    createConnection(session, classAndMethod[0], dependencyValue, "declares");

                    //Make relation between current_node -> calls -> java_method
                    createConnection(session, id, dependencyValue, "calls");
                } else {
                    createNode(session, dependencyValue, "placeholder", Map.of());
                    createConnection(session, id, dependencyValue, dependencyName);
                }
            }

        }

    }

    private void createNode(Session session, String id, String label, Map<String, List<String>>... fields) {
        Map<String, List<String>> parameters = new HashMap<>();
        Arrays.stream(fields).forEach(parameters::putAll);

        String createNode = "MERGE (source {id: $id}) " +
                            "SET source += $fields " +
                            "SET source:" + label + " " +
                            "RETURN source;";

        log.info("Creating node id: {}, label: {}", id, label);
        runSave(session, createNode, Map.of("id", id,
                "fields", parameters));
    }

    private void createConnection(Session session, String fromId, String toId, String label) {
        String query = "MATCH (from {id: $fromId}), (to {id: $toId}) " +
                       "MERGE (from)-[:" + label + "]->(to)";

        log.info("Creating connection from: {} to: {} label: {}", fromId, toId, label);
        runSave(session, query, Map.of("fromId", fromId, "toId", toId));
    }

    private void runSave(Session session, String query, Map<String, Object> parameters) {
        session.executeWrite(tx -> {
            tx.run(query, parameters);
            return null;
        });
    }

    private void storeBlock_v1(Session session, JsonNode node) {
        String id = node.get("id").asText();
        String name = node.get("name").asText();
        Map<String, List<String>> fields = extractFieldsAndValues(node, "fields", null);

        Map<String, List<String>> dependencies = extractFieldsAndValues(node, "dependencies", null);
        List<Map<String, String>> dependenciesParameter = new ArrayList<>();
        for (Map.Entry<String, List<String>> block : dependencies.entrySet()) {
            block.getValue().forEach(v -> dependenciesParameter.add(Map.of("type", block.getKey(), "targetId", v)));
        }

        Map<String, List<String>> dependenciesNodeParameters = extractFieldsAndValues(node, "dependencies", "dependencies");



/*        Map<String, Object> classifiers = objectMapper.convertValue(node.get("classifiers"), new TypeReference<Map<String, Object>>() {
        });*/

        String mergeQuery = "MERGE (source {id: $id}) " +
                            "SET source += $fields, source += $dependenciesParameters " +
                            "SET source:" + name + " ";
        String query = mergeQuery + """
                WITH source
                UNWIND $dependencies AS dependency
                MERGE (target {id: dependency.targetId})
                ON CREATE SET
                    target.name = "Placeholder"
                WITH source, target, dependency
                CALL apoc.create.relationship(source, dependency.type, {}, target) YIELD rel
                RETURN source, target, rel;
                """;

        session.executeWrite(tx -> {
            tx.run(query,

                    Map.of("id", id,
                            "fields", fields,
                            "dependencies", dependenciesParameter,
                            "dependenciesParameters", dependenciesNodeParameters));
            return null;
        });
    }

    public Map<String, List<String>> extractFieldsAndValues(JsonNode jsonNode, String fieldToExtract, String prefix) {
        Map<String, List<String>> fields = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = jsonNode.get(fieldToExtract).fields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldsIterator.next();
            String fieldName = fieldEntry.getKey();
            List<String> values = extractArrayValues(fieldEntry.getValue());


            String fieldNameToStore = prefix != null ? prefix + "." + fieldName : fieldName;
            fields.put(fieldNameToStore, values);
        }

        return fields;
    }

    public List<String> extractArrayValues(JsonNode jsonNode) {
        List<String> values = new ArrayList<>();
        Iterator<JsonNode> elements = jsonNode.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            values.add(element.asText()); // Convert each element to String and add to ArrayList
        }

        return values;
    }

    /**
     * Does not allow dynamic adding though KB. Bad approach
     *
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
