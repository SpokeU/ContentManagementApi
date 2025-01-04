package dev.omyshko.contentmanagement.api.endpoint.test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseService;
import dev.omyshko.contentmanagement.knowledgegraph.schema.BlockSchemaParser;
import dev.omyshko.contentmanagement.knowledgegraph.schema.KBSchemaModels;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestEndpoint {

    public static final String DB_URI = "neo4j+s://6a2ca604.databases.neo4j.io";
    public static final String DB_USER = "neo4j";
    public static final String DB_PASSWORD = "CeCkmRS4il8LJyZpxj-6M2AgF10K8gTfQb3R2qqT4R4";
    public static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        String methodsContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_method.md").content();
        JsonObjectSchema methodsBlockSchema = new BlockSchemaParser().convert(methodsContent);
        return ResponseEntity.ok(null);
    }


    /**
     * <pre>
     * Stage 1: Syntax analysis splitting + building notRequiredDependencies
     * I'm changing approach to HOW TO ANALYZE <BLOCK_NAME>
     *
     * 1. Split file into root blocks
     * What to extract from java FILE ? = (Node:java_class {id: $id}) - [edge:$declares_method] > (Node:depepdency.name {id:$depepdency.method_signature} ),
     *
     *
     * 2. Iterating though root entities.
     *          Iterate though notRequiredDependencies of rootBlock (imports, methods, inner_classes etc. ) and expand + split those recursively according to KB page of block name
     *          IF dependency content is in current file (has content lines) then Expand it/ Analyze and create notRequiredDependencies to placeholder nodes
     *          ELSE quit
     *
     * #Notes
     *         Placeholder node is a dependency which cannot be expanded yet but we can see it when analyzing current content.
     *         For example File1 has a Method1 block. Analyzing this method you can see its calling another method from class2.method2.
     *         But that method contained in another file thus the content will be filled when  analyzing that file. But at this point we need to create a dependency to indicate relations between blocks.
     *
     *
     *         **Id** - is an URI - Universal Resource Identifier of an info block.
     *         This way constructed Id should be the same as if you are analyzing current block or creating placeholder to not yet processed block.
     *
     *         ** Nodes vs Dependencies
     *         Different nodes are different not related entities. While a single node with notRequiredDependencies draws association.
     *         Only File can have multiple different root blocks. But child block will always have single KB page. Or not? If not then multiple analyzers can be created for single type of block.
     * </pre>
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    @GetMapping("test_parse_v2")
    public ResponseEntity<JsonNode> testParsingUsingKbV2(@RequestParam String filePath) throws IOException {
        //get file type
        //get root nodes according to file type
        //expand root nodes recursively

        //#1
        Path pathToFile = Paths.get(filePath);
        List<String> lines = Files.readAllLines(pathToFile);
        String contentWithLines = merge(prependLineNumbers(lines));
        //Get Root Blocks according to file type from KB
        List<KnowledgeBaseInformationProvider.KnowledgeBasePageNode> rootBlocksKBPages = getRootBlocksKBPages(filePath);

        //Convert KB page to Json schema
        JsonSchema jsonSchema = new BlockSchemaParser().convertToSchema(rootBlocksKBPages);

        //Call llm and return nodes
        String output = callLLM(jsonSchema, contentWithLines);
        JsonNode jsonNode = new ObjectMapper().readTree(output);
        Blocks blocks = objectMapper.readValue(output, Blocks.class);

        createNode(driver.session(), filePath, "File", Map.of("uri", filePath));
        //Store to Neo4j
        //storeBlocksToNeo4J(blocks, filePath);

        //TODO!!!
        // 1. Не включає закриваючу скобку метода }
        // Можна давати приклати контенту як в оригінальнцій доці про клас
        //2. не хоче включати java_doc в from_line. А якщо додати окреме поле java_doc_starting_line то часто збивається і не ту лінійку дає

        //Короче залупа. Результати дуже інконсістент. Заїбало

        //#2
        //Analyze each dependency
        for (JsonNode blocksArrays : jsonNode) { //"declared_classes": Array
            for (JsonNode block : blocksArrays) { // Array item

                Iterator<Map.Entry<String, JsonNode>> dependencies = block.get("dependencies").fields();
                while (dependencies.hasNext()) {
                    Map.Entry<String, JsonNode> dependency = dependencies.next();

                    log.info(dependency.getKey());//Connection type

                    //Analyze dependency
                    for (JsonNode node : dependency.getValue()) {
                        //analyzeDependency(node.get("id").textValue());
                        log.info(node.toPrettyString());
                    }
                }

                //Important - To pass child block to LLM for extracting properties/notRequiredDependencies it needs to be enriched with parent block stripped content (ParentContent - currentBlock siblings content)
                //expand Content(add filename to dependency for ease of expanding content)

                //Get blocks to extract from KB according to current block name we are analyzing (same block name that is in KB page. So based on KB you can check consistency of you blocks)

                //Convert KB page to Json schema
                //Call llm and return nodes
                //Store to Neo4j
                //Repeat (currently won't be done for POC)
            }
        }

        return ResponseEntity.ok(jsonNode);
    }

    private List<String> prependLineNumbers(List<String> lines) {
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<String> linesWithNumbers = lines.stream().map(line -> String.format("%03d: %s", lineNumber.getAndIncrement(), line)).toList();
        return linesWithNumbers;
    }

    private String merge(List<String> lines) {
        return String.join("\n", lines);
    }


    /**
     * <pre>
     *     1. Expand content
     *     2. Enrich content with parent blocks
     *     3. Get block schema from KB according to current block name
     *     4. Call LLM
     *     5. Store to Neo4j
     * </pre>
     * <p>
     * </pre>
     *
     * @return
     */
    private String parseBlock(String id) {
        Node nodeById = getNodeById(id);
        //Check if its placeholder
/*        if (!nodeById.hasLabel("placeholder")) {
            log.warn("Skip processing {} node as its not a placeholder node means it already been processed", id);
            return;
        }*/

        List<String> shortenedContent = getShortenedContent(nodeById);
        log.info(shortenedContent.toString());
        //get Dependency object from Neo4j
        return merge(shortenedContent);
    }

    /**
     * <pre>Basically here we say - get me all the content needed so LLM can split provided block id further.
     * </pre>
     * <pre>
     * For given blockId - searches parent blocks content and enriches current block content with parent one.
     * !!!IMPORTANT If there are more than one level nesting then we need to get all the content until the file node!!!
     *
     * For example given *java_method* block - to properly analyze it with LLM we need to enrich it with its class information, class imports, class fields and constructor.
     * Thus getting parent content is essential for understanding a child
     *
     * Question: How to know which blocks are needed to enrich context and which are not?
     * For example: For java_method we need - java_class, java_class_imports etc.
     * Suggestion: Add tags to dependency in KB page because a user identified blocks he need to identify relations himself
     * Get all **siblings** of parent
     * </pre>
     */
    private List<String> getShortenedContent(Node node) {
        List<String> shortenedContent = new ArrayList<>();

        //How to get content for java_method?
        if (node.hasLabel("java_method")) {
            String fileUri = node.get("fileUri").asString();
            ParentAndNotRequiredDependencies nodes = findNodeBlocksRequiredForJavaMethodBlockExplanation(node.get("id").asString());
            //Get all parent content (all the content based on which parent has been analyzed and split) - for java class its from package declaration till closing
            Node parent = nodes.parent;

            //Subtract content of nodes that are not affecting this node functionality or context
            List<String> parentContent = getParentContent(parent);
            List<String> parentContentWithLines = prependLineNumbers(parentContent);

            //Я криво рахую
            List<Integer> linesToRemove = getLinesToRemove(nodes.notRequiredDependencies);

            shortenedContent = stripContent(parentContentWithLines, linesToRemove);
        }
        return shortenedContent;
    }

    private List<String> stripContent(List<String> content, List<Integer> linesToRemove) {
        // Create a copy of the content to avoid modifying the original list
        List<String> strippedContent = new ArrayList<>(content);

        // Sort the indices in descending order to prevent shifting issues during removal
        linesToRemove.stream()
                .sorted((a, b) -> b - a) // Sort in reverse order
                .forEach(index -> {
                    if (index >= 0 && index < strippedContent.size()) {
                        strippedContent.remove((int) index - 1); //Lines are starting from 1 while array from 0
                    }
                });

        return strippedContent;
    }

    private List<Integer> getLinesToRemove(List<Node> notRequiredDependencies) {
        List<Integer> linesToRemove = new ArrayList<>();

        for (Node node : notRequiredDependencies) {
            Integer fromLine = Integer.valueOf(node.get("fromLine").asString());
            Integer toLine = Integer.valueOf(node.get("toLine").asString());

            IntStream.range(fromLine, toLine + 1).forEach(linesToRemove::add);
        }

        return linesToRemove;
    }

    private List<String> getParentContent(Node parent) {
        String fileUri = parent.get("fileUri").asString();
        Path pathToFile = Paths.get(fileUri);
        try {
            return Files.readAllLines(pathToFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get parentNode
     * and for parent node gets all the childNodes with 'configures' connection type
     *
     * @return
     */
    private ParentAndNotRequiredDependencies findNodeBlocksRequiredForJavaMethodBlockExplanation(String id) {
        String query = "MATCH (currentNode:java_method {id: $id})<-[connection]-(parentNode)\n" +
                       "WHERE connection.connection_type = 'contains'\n" +
                       "OPTIONAL MATCH (parentNode)-[parentConnection]->(parentConnectionNode)\n" +
                       "WHERE parentConnection.connection_type <> 'configures'\n" +
                       "AND parentConnectionNode.id <> $id \n" +
                       "RETURN parentNode, collect(parentConnectionNode) AS parentConnectionNodes";

        ParentAndNotRequiredDependencies parentAndDependencies = driver.session().executeRead(tx -> {
            Result result = tx.run(query, Map.of("id", id));
            ParentAndNotRequiredDependencies parentAndDependenciesInternal = null;
            while (result.hasNext()) {
                Record next = result.next();
                Node parentNode = next.get("parentNode").asNode();
                List<Node> parentConnectionNodes = next.get("parentConnectionNodes").asList(t -> t.asNode());
                parentAndDependenciesInternal = new ParentAndNotRequiredDependencies(parentNode, parentConnectionNodes);
            }
            return parentAndDependenciesInternal;
        });

        //List<Node> nodes = queryNodes(query, );
        return parentAndDependencies;
    }

    record ParentAndNotRequiredDependencies(Node parent, List<Node> notRequiredDependencies) {
    }


    private List<KnowledgeBaseInformationProvider.KnowledgeBasePageNode> getRootBlocksKBPages(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return switch (extension) {
            case "java" -> List.of(knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_class_lines.md"));
            default -> throw new IllegalStateException("Unexpected value: " + extension);
        };
    }

    private String callLLM(JsonSchema jsonSchema, String fileContent) throws JsonProcessingException {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(jsonSchema)
                .build();


        dev.langchain4j.data.message.SystemMessage systemMessage = dev.langchain4j.data.message.SystemMessage.from(
                "You are a tool for extracting blocks of code that are present in a provided file according to provided json_schema.");

        PromptTemplate promptTemplate = PromptTemplate.from("Provided this file content extract blocks according to json_schema <file-content>{{it}}</file-content>" +
                                                            "Important notes: for `declared_methods` a property `method_closing_brace_line_number` should capture the line where a method closing brace symbol `}` is located ");
        String apply = promptTemplate.apply(fileContent).text();
        UserMessage userMessage = UserMessage.from(apply);


        ChatRequest chatRequest = ChatRequest.builder()
                .messages(systemMessage, userMessage)
                .responseFormat(responseFormat)
                .build();
        //3. Convert KB to JsonSchema
        //4. Pass File content + JsonSchema to LLM

        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder() // see [1] below
                .apiKey("AIzaSyC5kgYlvopY75ESjoCT9V46u73ZgKDQXlg")
                .modelName("gemini-1.5-flash")
                .responseFormat(ResponseFormat.JSON) // see [2] below
                //.strictJsonSchema(true) // see [2] below
                .logRequestsAndResponses(true)
                .temperature(0.1)
                //.topP(0.1)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();


        return output;
    }

    record Blocks(List<Block> blocks) {
    }

    @Description("A block of text")
    record Block(@Description("Id of this block") String id,
                 @Description("Name") String name,
                 @Description("Field values") Map<String, Object> fields, Map<String, List<Dependency>> dependencies) {
    }

    record Dependency(String connection_type,
                      String dependency_type,
                      String id,
                      String from_line,
                      String to_line) {

        @JsonAnySetter
        public void setUnknownField(String name, Object value) {
            log.info("Unknown property {" + name + ":" + value + "}");
        }

    }


    record Field(@Description("Block field name") String name, @Description("Field values") Object value) {
    }

    record Range(int fromLine, int toLine) {
    }


    @GetMapping("test_parse_using_kb")
    public ResponseEntity<JsonNode> testFileParseUsingKb(@RequestParam String filePath) throws IOException {
        //TODO Choose model based on file size? Larger files extraction doing better on 4o while smaller blocks can be used with 4o-mini
        //I'm changing approach to HOW TO ANALYZE <BLOCK_NAME>

        //1. Split file into root blocks
        //What to extract from java FILE ? = (Node:java_class {id: $id}) - [edge:$declares_method] > (Node:depepdency.name {id:$depepdency.method_signature} ),


        // 2. Iterating though root entities.
        // Iterate though notRequiredDependencies of rootBlock (imports, methods, inner_classes etc. ) and expand + split those recursively according to KB page of block name
        // IF dependency content is in current file (has content lines) then Expand it/ Analyze and create notRequiredDependencies to placeholder nodes
        // ELSE quit

        //Notes
        // Placeholder node is a dependency which cannot be expanded yet but we can see it when analyzing current content.
        //For example File1 has a Method1 block. Analyzing this method you can see its calling another method from class2.method2.
        //But that method contained in another file thus the content will be filled when  analyzing that file. But at this point we need to create a dependency to indicate relations between blocks.


        //1. Search available Knowledge Base Graphs by subject KG (Knowledge Graph) based on File language and Main Frameworks
        Path pathToFile = Paths.get(filePath);
        FileType fileType = getFileType(pathToFile.getFileName().toString());
        List<String> lines = Files.readAllLines(pathToFile);
        String content = merge(prependLineNumbers(lines));

        //2. Get Blocks description from KB
        String classesContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_class_lines.md").content();
        String methodsContent = knowledgeBaseInformationProvider.getPageContent("knowledge-graph\\java\\java_method_lines.md").content();

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
                .modelName(GPT_4_O_MINI)
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                //.temperature(0.1)
                .topP(0.1)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();


        JsonNode response = new ObjectMapper().readTree(output);

        //storeBlockToNeo4J(response);


        return ResponseEntity.ok(response);
    }

    @GetMapping("/splitBlock")
    public ResponseEntity<String> splitNodeBlock(@RequestParam String id) {
        String s = parseBlock(id);
        return ResponseEntity.ok(s);
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


    private void storeBlocksToNeo4J(Blocks blocks, String parentNodeId) {
        // URI examples: "neo4j://localhost", "neo4j+s://xxx.databases.neo4j.io"

        try (var driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD))) {
            driver.verifyConnectivity();
            Session session = driver.session();
            storeBlocks(session, blocks, parentNodeId);
        }
    }

    private void storeBlocks(Session session, Blocks blocks, String fileNodeId) {
        for (Block block : blocks.blocks) {
            String blockId = storeBlock(session, block, fileNodeId);
            createConnection(session, fileNodeId, blockId, "contains", Map.of());
        }
    }

    private String storeBlock(Session session, Block block, String fileNodeId) {
        String id = block.id;
        String name = block.name;

        Map<String, Object> fields = block.fields;


        Map<String, List<Dependency>> dependencies = block.dependencies;


        //Create node for block
        createNode(session, id, name, fields, Map.of("fileUri", fileNodeId));


        for (Map.Entry<String, List<Dependency>> dependency : dependencies.entrySet()) {
            String dependencyName = dependency.getKey();//declares_method

            for (Dependency dependencyValue : dependency.getValue()) {
                String connectionType = StringUtils.defaultIfBlank(dependencyValue.connection_type, "");////TODO change connection_type to read from KB rather then putting extra effort and tokens to LLM

                String dependencyType = dependencyValue.dependency_type;
                String dependencyId = dependencyValue.id;
                String fromLine = dependencyValue.from_line;
                String toLine = dependencyValue.to_line;

                //Create dependency node placeholder and connection
                createNode(session, dependencyId, dependencyType, Map.of("fromLine", fromLine, "toLine", toLine, "fileUri", fileNodeId));
                createConnection(session, id, dependencyId, dependencyName, Map.of("connection_type", connectionType));
            }


        }
        /*//Handle notRequiredDependencies
        for (Map.Entry<String, List<String>> dependency : notRequiredDependencies) {
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

        }*/
        return id;
    }

    private void createNode(Session session, String id, String label, Map<String, Object>... fields) {
        createNode(session, id, List.of(label), fields);
    }

    private void createNode(Session session, String id, List<String> label, Map<String, Object>... fields) {
        Map<String, Object> parameters = new HashMap<>();
        Arrays.stream(fields).forEach(parameters::putAll);

        String createNode = "MERGE (source {id: $id}) " +
                            "SET source += $fields " +
                            "SET source:" + String.join(":", label) + " " +
                            "RETURN source;";

        log.info("Creating node id: {}, label: {}", id, label);
        runSave(session, createNode, Map.of("id", id,
                "fields", parameters));
    }

    private void createConnection(Session session, String fromId, String toId, String label, Map<String, Object> properties) {
        String query = "MATCH (from {id: $fromId}), (to {id: $toId}) " +
                       "MERGE (from)-[r:" + label + "]->(to) " +
                       "SET r += $properties ";

        log.info("Creating connection from: {} to: {} label: {}", fromId, toId, label);
        runSave(session, query, Map.of("fromId", fromId, "toId", toId, "properties", properties));
    }

    private void runSave(Session session, String query, Map<String, Object> parameters) {
        session.executeWrite(tx -> {
            tx.run(query, parameters);
            return null;
        });
    }

/*    private void storeBlock_v1(Session session, JsonNode node) {
        String id = node.get("id").asText();
        String name = node.get("name").asText();
        Map<String, List<String>> fields = extractFieldsAndValues(node, "fields", null);

        Map<String, List<String>> notRequiredDependencies = extractFieldsAndValues(node, "notRequiredDependencies", null);
        List<Map<String, String>> dependenciesParameter = new ArrayList<>();
        for (Map.Entry<String, List<String>> block : notRequiredDependencies.entrySet()) {
            block.getValue().forEach(v -> dependenciesParameter.add(Map.of("type", block.getKey(), "targetId", v)));
        }

        Map<String, List<String>> dependenciesNodeParameters = extractFieldsAndValues(node, "notRequiredDependencies", "notRequiredDependencies");



*//*        Map<String, Object> classifiers = objectMapper.convertValue(node.get("classifiers"), new TypeReference<Map<String, Object>>() {
        });*//*

        String mergeQuery = "MERGE (source {id: $id}) " +
                            "SET source += $fields, source += $dependenciesParameters " +
                            "SET source:" + name + " ";
        String query = mergeQuery + """
                WITH source
                UNWIND $notRequiredDependencies AS dependency
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
                            "notRequiredDependencies", dependenciesParameter,
                            "dependenciesParameters", dependenciesNodeParameters));
            return null;
        });
    }*/

    public Map<String, Object> extractFieldsAndValues(JsonNode jsonNode, String fieldToExtract, String prefix) {
        Map<String, Object> fields = new HashMap<>();

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

    @GetMapping("test_pojo_openai")
    public ResponseEntity<KBSchemaModels.JavaClassBlocks> testPojo(@RequestParam String filePath)  throws IOException{
        //#1
        Path pathToFile = Paths.get(filePath);
        List<String> lines = Files.readAllLines(pathToFile);
        String contentWithLines = merge(prependLineNumbers(lines));

        ChatLanguageModel chatModel = OpenAiChatModel.builder() // see [1] below
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .responseFormat("json_schema") // see [2] below
                .strictJsonSchema(true) // see [2] below
                .logRequests(true)
                .logResponses(true)
                .build();

        KBSchemaModels.JavaBlocksExtractor blocksExtractor = AiServices.create(KBSchemaModels.JavaBlocksExtractor.class, chatModel);

        KBSchemaModels.JavaClassBlocks blocks = blocksExtractor.extractBlocks(contentWithLines);
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("test_pojo_gemini")
    public ResponseEntity<KBSchemaModels.JavaClassBlocks> testPojoGemini(@RequestParam String filePath) throws IOException {
        //get file type
        //get root nodes according to file type
        //expand root nodes recursively

        //#1
        Path pathToFile = Paths.get(filePath);
        List<String> lines = Files.readAllLines(pathToFile);
        String contentWithLines = merge(prependLineNumbers(lines));


        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder() // see [1] below
                .apiKey("")
                .modelName("gemini-1.5-flash")
                .responseFormat(ResponseFormat.JSON) // see [2] below
                //.strictJsonSchema(true) // see [2] below
                .logRequestsAndResponses(true)
                .temperature(0.1)
                //.topP(0.1)
                .build();

        KBSchemaModels.JavaBlocksExtractor blocksExtractor = AiServices.create(KBSchemaModels.JavaBlocksExtractor.class, chatModel);

        KBSchemaModels.JavaClassBlocks blocks = blocksExtractor.extractBlocks(contentWithLines);
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


    interface BlocksExtractor {

        @SystemMessage("You are a tool for processing a code. Provided a file content please extract blocks. Available blocks are follows: {{available_blocks}}. Each block has its own set of notRequiredDependencies and classifications. For example a class cannot have dependency type 'CALLS' because its owned by method")
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
