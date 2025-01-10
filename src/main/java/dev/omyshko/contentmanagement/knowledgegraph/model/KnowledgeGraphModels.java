package dev.omyshko.contentmanagement.knowledgegraph.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.neo4j.driver.types.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * This is the core models that my framework will use to build knowledge graph.
 *
 * It will specify some fields that will be used by framework such as
 * id, fromLine, toLine
 * </pre>
 */
public class KnowledgeGraphModels {

    /**
     * @param id              Example dev.omyshko.contentmanagement.api.endpoint.ProcessingEndpoint
     * @param type            java_class, java_method
     * @param name            Exists just for readability of neo4j. Because it shows Name property as display
     * @param contentFromLine
     * @param contentToLine
     * @param content         - A content which relates to this block. Might be shortenedContent and not necessarily full. For example for java_class It might be class declaration without methods pr with simple placeholders
     * @param properties
     * @param childBlocks
     */
    @Builder
    @Data
    public static class Block {

        String id;
        String type;
        String name;
        String contentFromLine;
        String contentToLine;
        String content;
        String contentUri;
        Map<String, Object> properties;
        List<ChildBlock> childBlocks;

        public Map<String, Object> getProperties() {
            if (properties == null) {
                properties = new HashMap<>();
            }
            return properties;
        }

        public static Block fromNode(Node node) {

            Map<String, Object> nodeProperties = node.asMap();

            Block block = Block.builder()
                    .id(nodeProperties.get("id").toString())
                    .type(nodeProperties.getOrDefault("contentType", "").toString())
                    .name(nodeProperties.getOrDefault("name", "").toString())
                    .contentFromLine(nodeProperties.getOrDefault("contentFromLine", "").toString())
                    .contentToLine(nodeProperties.getOrDefault("contentToLine", "").toString())
                    .content(nodeProperties.getOrDefault("content", "").toString())
                    .contentUri(nodeProperties.getOrDefault("contentUri", "").toString()).build();

            return block;
        }
    }


    /**
     * @param connectionLabel - declares_method, declares_inner_class. Describe semantic relation between this child block and its parent:  java_class(parent) -> declares -> java_method(this child). So later it can be used by LLM as well to create automated queries
     * @param childBlock
     */
    @Builder
    public record ChildBlock(String connectionLabel,
                             //String connectionType, TODO //do we need this block when getting parent block? If yes then maybe just not extract it from parent block.
                             Block childBlock) {

        @JsonAnySetter
        public void setUnknownField(String name, Object value) {
            //log.info("Unknown property {" + name + ":" + value + "}");
        }

    }

}
