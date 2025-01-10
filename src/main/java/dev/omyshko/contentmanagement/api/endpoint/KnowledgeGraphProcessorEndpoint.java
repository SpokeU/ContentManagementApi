package dev.omyshko.contentmanagement.api.endpoint;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.source.FileSystemSource;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.omyshko.contentmanagement.core.utils.ContentUtils;
import dev.omyshko.contentmanagement.knowledgegraph.entity.BlockNeo4jEntity;
import dev.omyshko.contentmanagement.knowledgegraph.repository.BlockNeo4jRepository;
import dev.omyshko.contentmanagement.knowledgegraph.repository.KnowledgeGraphRepository;
import dev.omyshko.contentmanagement.knowledgegraph.schema.JavaClassSchema;
import dev.omyshko.contentmanagement.knowledgegraph.transformer.JavaClassKGTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/processing")
public class KnowledgeGraphProcessorEndpoint {

    public static final String JAVA_FILE_BLOCK_TYPE = "java_file";
    public static final String JAVA_CLASS_BLOCK_TYPE = "java_class";
    public static final String JAVA_METHOD_BLOCK_TYPE = "java_method";
    public static final String JAVA_INNER_CLASS_BLOCK_TYPE = "java_inner_class";
    public static final String FILE_TO_BLOCKS_CONNECTION_PROPERTY = "CONTAINS";

    private final ChatLanguageModel jsonSchemaModel;
    private final JavaClassKGTransformer javaClassKGTransformer;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final BlockNeo4jRepository blockNeo4jRepository;
    private final ContentUtils contentUtils;

    public KnowledgeGraphProcessorEndpoint(@Qualifier("geminiJsonSchemaModel") ChatLanguageModel jsonSchemaModel, JavaClassKGTransformer javaClassKGTransformer, KnowledgeGraphRepository knowledgeGraphRepository, BlockNeo4jRepository blockNeo4jRepository, ContentUtils contentUtils) {
        this.jsonSchemaModel = jsonSchemaModel;
        this.javaClassKGTransformer = javaClassKGTransformer;
        this.knowledgeGraphRepository = knowledgeGraphRepository;
        this.blockNeo4jRepository = blockNeo4jRepository;
        this.contentUtils = contentUtils;
    }

    /**
     * <pre>
     * Stage 1: Structure split.
     * Split file into blocks and add only structural connections.
     * I'm changing approach to HOW TO ANALYZE <BLOCK_NAME>
     *
     * 1. Get file_type
     * 2. Based on filetype get RootBlocks to extract
     * 3. Split file into root blocks
     *  What to extract from java FILE ? = (Node:java_class {id: $id}) - [edge:$declares_method] > (Node:depepdency.name {id:$depepdency.method_signature} ),
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
     * @throws IOException
     */
    @PostMapping("/file")
    public ResponseEntity<List<BlockNeo4jEntity>> processFile(@RequestBody FileProcessingRequest fileProcessingRequest) throws IOException {
        Path pathToFile = Paths.get(fileProcessingRequest.filePath);

        //create file block
        //TODO based on file extension pick up proper file block
        BlockNeo4jEntity javaFileNode = BlockNeo4jEntity.builder()
                .id(pathToFile.toFile().getAbsolutePath())
                .name(JAVA_FILE_BLOCK_TYPE)
                .type(JAVA_FILE_BLOCK_TYPE)
                .contentUri(pathToFile.toFile().getAbsolutePath())
                .build();
        blockNeo4jRepository.save(javaFileNode);

        List<BlockNeo4jEntity> fileProcessingResult = processBlock(javaFileNode.getId());

        return ResponseEntity.ok(fileProcessingResult);
    }

    private List<BlockNeo4jEntity> processBlock(String id) {
        log.info("Processing block {}", id);
        BlockNeo4jEntity blockNeo4jEntity = blockNeo4jRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cannot find block with Id:" + id));

        List<BlockNeo4jEntity> processedKnowledgeGraphBlocks = switch (blockNeo4jEntity.getType()) {
            case JAVA_FILE_BLOCK_TYPE -> processJavaFileBlockType(blockNeo4jEntity);
            case JAVA_INNER_CLASS_BLOCK_TYPE -> processJavaInnerClassBlockType(blockNeo4jEntity);
            default -> {
                log.info("No processor found for {}", blockNeo4jEntity.getType());
                yield List.of();
            }
        };

        processedKnowledgeGraphBlocks.forEach(
                kgb -> kgb.getChildBlocks().forEach(
                        childBlockConnections -> processBlock(childBlockConnections.getChildBlock().getId())));

        //according to BlockType - structure split
        //Default split steps:
        //1. get schema
        //2 get transformer

        //get parent context + current context TODO only for ID generation
        log.info("Processed block {}", blockNeo4jEntity.getId());
        return processedKnowledgeGraphBlocks;
    }

    private List<BlockNeo4jEntity> processJavaInnerClassBlockType(BlockNeo4jEntity javaFileBlock) {
        log.info("Processing Java inner class block {}", javaFileBlock.getId());
        return List.of();
    }

    private List<BlockNeo4jEntity> processJavaFileBlockType(BlockNeo4jEntity javaFileBlock) {
        String contentUri = javaFileBlock.getContentUri();

        Path pathToFile = Paths.get(contentUri);
        Document load = DocumentLoader.load(FileSystemSource.from(pathToFile), new TextDocumentParser());

        String contentWithLines = ContentUtils.getLocalContentWithLineNumbers(pathToFile);

        //schema
        JavaClassSchema.JavaClassBlocksExtractor blocksExtractor = AiServices.create(JavaClassSchema.JavaClassBlocksExtractor.class, jsonSchemaModel);
        //LLM
        JavaClassSchema.JavaClassBlocks blocks = blocksExtractor.extractBlocks(contentWithLines);


        //2 transform LLM result into KG model
        //because field names play huge role we cannot have unified names like `id` or `fromLine`. This way we need additional step to make KnowledgeGraph object from GPT response
        //Transform to My Framework Knowledge graph models
        //TODO blocks.classBlocks().get(0)
        //TODO CopyMetadata
        BlockNeo4jEntity transformedBlock = javaClassKGTransformer.transform(blocks.classBlocks().get(0), Map.of("contentUri", pathToFile.toFile().getAbsolutePath()));

        //Temp solution to fill in contentLocationUri
        transformedBlock.setContentUri(pathToFile.toFile().getAbsolutePath());

        javaFileBlock.addChildBlock(transformedBlock, FILE_TO_BLOCKS_CONNECTION_PROPERTY);
        blockNeo4jRepository.save(javaFileBlock);

        //Save to neo4j
        BlockNeo4jEntity savedKnowledgeGraphBlock = blockNeo4jRepository.save(transformedBlock);

        return List.of(savedKnowledgeGraphBlock);
    }

    private record FileProcessingRequest(String filePath) {
    }


}
