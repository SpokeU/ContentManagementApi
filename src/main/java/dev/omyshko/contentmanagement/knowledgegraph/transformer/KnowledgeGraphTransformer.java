package dev.omyshko.contentmanagement.knowledgegraph.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import dev.omyshko.contentmanagement.knowledgegraph.model.KnowledgeGraphModels;

/**
 * Interface to implement for transformation from KnowledgeBase->json_schema->LLMResponse to Internal KnowledgeGraph model
 */
public interface KnowledgeGraphTransformer {

    KnowledgeGraphModels.Block transform(JsonNode llmJsonResponse);

}
