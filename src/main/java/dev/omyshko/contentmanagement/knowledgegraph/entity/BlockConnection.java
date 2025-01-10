package dev.omyshko.contentmanagement.knowledgegraph.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.List;

@Builder
@Data
@RelationshipProperties
public class BlockConnection {

    @Id
    @GeneratedValue
    private Long id;

    private String connectionType;

    @TargetNode
    private BlockNeo4jEntity childBlock;

}
