package dev.omyshko.contentmanagement.knowledgegraph.transformer;

import dev.omyshko.contentmanagement.api.endpoint.KnowledgeGraphProcessorEndpoint;
import dev.omyshko.contentmanagement.knowledgegraph.entity.BlockNeo4jEntity;
import dev.omyshko.contentmanagement.knowledgegraph.schema.v2.SingleBlocks;

import static dev.omyshko.contentmanagement.api.endpoint.KnowledgeGraphProcessorEndpoint.*;

public class JavaSingleBlocksTransformer {

    public BlockNeo4jEntity transform(SingleBlocks.JavaClassBlock javaClassBlock) {
        BlockNeo4jEntity javaClassNode = BlockNeo4jEntity.builder()
                .id(javaClassBlock.id())
                .name(KnowledgeGraphProcessorEndpoint.JAVA_CLASS_BLOCK_TYPE)
                .type(KnowledgeGraphProcessorEndpoint.JAVA_CLASS_BLOCK_TYPE)
                .contentFromLine(javaClassBlock.contentFromLine())
                .contentToLine(javaClassBlock.contentToLine())
                //content - TODO shortened content without methods?
                //properties - No extra properties yet
                .build();

        return javaClassNode;
    }

    public BlockNeo4jEntity transform(SingleBlocks.JavaMethodBlock javaMethodBlock) {
        BlockNeo4jEntity javaMethodNode = BlockNeo4jEntity.builder()
                .id(javaMethodBlock.id())
                .name(KnowledgeGraphProcessorEndpoint.JAVA_CLASS_BLOCK_TYPE)
                .type(KnowledgeGraphProcessorEndpoint.JAVA_CLASS_BLOCK_TYPE)
                .contentFromLine(javaMethodBlock.contentFromLine())
                .contentToLine(javaMethodBlock.contentToLine())
                .build();

        return javaMethodNode;
    }

    public BlockNeo4jEntity transform(SingleBlocks.JavaInnerClass javaInnerClassBlock) {
        BlockNeo4jEntity javaInnerClassNode = BlockNeo4jEntity.builder()
                .id(javaInnerClassBlock.fullyQualifiedInnerClassName())
                .name(JAVA_INNER_CLASS_BLOCK_TYPE)
                .type(JAVA_INNER_CLASS_BLOCK_TYPE)
                .contentFromLine(javaInnerClassBlock.contentFromLine())
                .contentToLine(javaInnerClassBlock.contentToLine())
                .build();

        return javaInnerClassNode;
    }

}
