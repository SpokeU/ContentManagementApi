package dev.omyshko.contentmanagement.knowledgegraph.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * * @param id              Example dev.omyshko.contentmanagement.api.endpoint.ProcessingEndpoint
 * * @param type            java_class, java_method
 * * @param name            Exists just for readability of neo4j. Because it shows Name property as display
 * * @param contentFromLine
 * * @param contentToLine
 * * @param contentShortened  - A content which relates to this block. Might be shortenedContent and not necessarily full. For example for java_class It might be class declaration without methods pr with simple placeholders
 * * @param properties / TODO all rest properties
 */
@Builder
@Data
@Node("Block")
public class BlockNeo4jEntity {

    @Id
    String id;

    @Property
    String type;

    @Property
    String name;

    @Property
    String contentFromLine;

    @Property
    String contentToLine;

    @Property
    String contentShortened;

    @Property
    String contentUri;

    //TODO all other properties
    //Map<String, Object> properties;

    @Setter(AccessLevel.NONE)
    @Relationship(type = "CONTAINS_CONTENT", direction = Relationship.Direction.OUTGOING)
    private List<BlockConnection> childBlocks = new ArrayList<>();

/*
    @Setter(AccessLevel.NONE)
    @Relationship(type = "CONTAINS_CONTENT", direction = Relationship.Direction.INCOMING)
    private BlockConnection parentBlockConnection;
*/


    public void addChildBlock(BlockNeo4jEntity childBlock, String label) {
        if (childBlocks == null) {
            childBlocks = new ArrayList<>();
        }

        childBlocks.add(BlockConnection.builder().connectionType(label).childBlock(childBlock).build());
    }

    public void addChildrenBlocks(List<BlockNeo4jEntity> childBlocks, String label) {
        childBlocks.forEach(childBlock -> addChildBlock(childBlock, label));
    }

/*    public void setParentBlock(BlockNeo4jEntity parentBlock, String label) {
        this.parentBlockConnection = BlockConnection.builder().connectionType(label).childBlock(parentBlock).build();

    }*/
}
