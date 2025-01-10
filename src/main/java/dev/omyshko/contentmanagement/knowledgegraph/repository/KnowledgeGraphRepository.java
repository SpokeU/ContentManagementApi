package dev.omyshko.contentmanagement.knowledgegraph.repository;

import dev.omyshko.contentmanagement.knowledgegraph.model.KnowledgeGraphModels;
import dev.omyshko.contentmanagement.knowledgegraph.model.KnowledgeGraphModels.Block;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class KnowledgeGraphRepository {

    public static final String DB_URI = "neo4j+s://6a2ca604.databases.neo4j.io";
    public static final String DB_USER = "neo4j";
    public static final String DB_PASSWORD = "CeCkmRS4il8LJyZpxj-6M2AgF10K8gTfQb3R2qqT4R4";

    private Driver driver;

    @PostConstruct
    public void init() {
        driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD));
    }

    public void storeBlock(Block knowledgeGraphBlock, String parentBlockId) {
        try (var driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD))) {
            driver.verifyConnectivity();
            Session session = driver.session();

            Map<String, Object> properties = extractBlockProperties(knowledgeGraphBlock);


            createNode(session, knowledgeGraphBlock.getId(), knowledgeGraphBlock.getType(), properties);
            createConnection(session, parentBlockId, knowledgeGraphBlock.getId(), "contains", Map.of());

            for (KnowledgeGraphModels.ChildBlock childBlock : knowledgeGraphBlock.getChildBlocks()) {
                Block childBlockNode = childBlock.childBlock();

                createNode(session, childBlockNode.getId(), childBlockNode.getType(), extractBlockProperties(childBlockNode));
                createConnection(session, knowledgeGraphBlock.getId(), childBlockNode.getId(), childBlock.connectionLabel(), Map.of("connectionProp", "test"));
            }
        }
    }

    private static @NotNull Map<String, Object> extractBlockProperties(Block knowledgeGraphBlock) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", knowledgeGraphBlock.getName());
        properties.put("contentType", knowledgeGraphBlock.getType());
        properties.put("contentFromLine", knowledgeGraphBlock.getContentFromLine());
        properties.put("contentToLine", knowledgeGraphBlock.getContentToLine());
        properties.put("content", knowledgeGraphBlock.getContent());
        properties.put("contentUri", knowledgeGraphBlock.getContentUri());
        properties.putAll(knowledgeGraphBlock.getProperties());
        return properties;
    }

    public void createNode(String id, String label, Map<String, Object>... fields) {
        try (var driver = GraphDatabase.driver(DB_URI, AuthTokens.basic(DB_USER, DB_PASSWORD))) {
            Session session = driver.session();
            createNode(session, id, List.of(label), fields);
        }
    }

    private void createNode(Session session, String id, String label, Map<String, Object>... fields) {
        createNode(session, id, List.of(label), fields);
    }

    public void createNode(Session session, String id, List<String> label, Map<String, Object>... fields) {
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

    public void createConnection(Session session, String fromId, String toId, String label, Map<String, Object> properties) {
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


    public List<Node> getTerminalNodes(TransactionContext tx) {
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

    public Node getNodeById(String id) {
        String query = "MATCH (N) WHERE N.id = $id RETURN N";
        return queryNodes(query, Map.of("id", id)).get(0);
    }

    public List<Node> queryNodes(String query, Map<String, Object> parameters) {
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
}
