package dev.omyshko.contentmanagement.knowledgegraph.repository;

import dev.omyshko.contentmanagement.knowledgegraph.entity.BlockNeo4jEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface BlockNeo4jRepository extends Neo4jRepository<BlockNeo4jEntity, String> {

    @Query("MATCH (b:Block)-[r:CONTAINS_CONTENT]->(c:Block) WHERE b.id = $id RETURN b, r, collect(c)")
    Optional<BlockNeo4jEntity> findByIdWithChildren(String id);


}
