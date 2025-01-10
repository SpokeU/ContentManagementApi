package dev.omyshko.contentmanagement.knowledgegraph.transformer;

import dev.omyshko.contentmanagement.knowledgegraph.entity.BlockNeo4jEntity;
import dev.omyshko.contentmanagement.knowledgegraph.schema.JavaClassSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static dev.omyshko.contentmanagement.api.endpoint.KnowledgeGraphProcessorEndpoint.*;

@Component
public class JavaClassKGTransformer {




    public static final String DECLARES_METHOD_RELATION = "declares_method";
    public static final String CONTAINS_INNER_CLASS_RELATION = "declares_inner_class";

    /**
     * How to transform LLM response model into Code Knowledge Graph Model
     *
     * @param javaClassBlock
     * @return
     */
    public BlockNeo4jEntity transform(JavaClassSchema.JavaClassBlock javaClassBlock, Map<String, Object> additionalContext) {
        List<BlockNeo4jEntity> methodNodes = javaClassBlock.declaredTopLevelMethods().stream().map(this::convertMethod).toList();
        List<BlockNeo4jEntity> innerClassNodes = javaClassBlock.declaredInnerClasses().stream().map(this::convertInnerClass).toList();


        BlockNeo4jEntity javaClassNode = BlockNeo4jEntity.builder()
                .id(javaClassBlock.id())
                .name("java_class")
                .type(JAVA_CLASS_BLOCK_TYPE)
                //fromLine - To be extracted by LLM. Or simply line of package declaration position
                //toLine - To be extracted by LLM. Or simply line last line of the file. Because java file can contain only one class
                //content - TODO shortened content without methods?
                //properties - No extra properties yet
                .build();

        methodNodes.forEach(cb -> javaClassNode.addChildBlock(cb, DECLARES_METHOD_RELATION));

        innerClassNodes.forEach(cb -> javaClassNode.addChildBlock(cb, CONTAINS_INNER_CLASS_RELATION));


        return javaClassNode;
    }

    private BlockNeo4jEntity convertInnerClass(JavaClassSchema.JavaInnerClass javaInnerClass) {
        BlockNeo4jEntity javaInnerClassBlock = BlockNeo4jEntity.builder()
                .id(javaInnerClass.fullyQualifiedInnerClassName())
                .type(JAVA_INNER_CLASS_BLOCK_TYPE)
                .contentFromLine(javaInnerClass.fromLineNumber())
                .contentToLine(javaInnerClass.toLineNumber())
                .build();

        return javaInnerClassBlock;
    }

    private BlockNeo4jEntity convertMethod(JavaClassSchema.JavaClassMethodBlock javaClassMethod) {
        BlockNeo4jEntity javaMethodBlock = BlockNeo4jEntity.builder()
                .id(javaClassMethod.id())
                .type(JAVA_METHOD_BLOCK_TYPE)
                .contentFromLine(javaClassMethod.methodCommentStartingLine()) //TODO if method has no comment
                .contentToLine(javaClassMethod.toLineNumber())
                .build();

        return javaMethodBlock;
    }

}
