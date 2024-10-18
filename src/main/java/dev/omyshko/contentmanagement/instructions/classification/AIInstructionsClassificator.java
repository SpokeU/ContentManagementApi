package dev.omyshko.contentmanagement.instructions.classification;

import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.TextClassifier;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.instructions.ResponseInformationProvider;
import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gets raw text instructions as an input and returns which RESPONSE_FORMAT it is (ChangeLog, ScriptExecution)
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class AIInstructionsClassificator implements InstructionClassificator {

    private final ResponseInformationProvider responseInformationProvider;

    TextClassifier<RESPONSE_FORMAT> classifier;

    @PostConstruct
    public void initialize() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Map<RESPONSE_FORMAT, List<String>> instructionsTypeListMap = responseInformationProvider.loadExamples();
        classifier = new EmbeddingModelTextClassifier<>(embeddingModel, instructionsTypeListMap, 1, 0.65, 0.5);
    }

    @Override
    public RESPONSE_FORMAT classify(String instructions) {
        List<RESPONSE_FORMAT> classified = classifier.classify(instructions);

        if (classified.isEmpty()) {
            throw new ApiException("No classifiers were found", HttpStatus.BAD_REQUEST);
        }

        if (classified.size() > 1) {
            throw new ApiException("More that one classifier were found. Currently not supported", HttpStatus.BAD_REQUEST);
        }

        return classified.get(0);
    }
}
