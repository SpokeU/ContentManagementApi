package dev.omyshko.contentmanagement.instructions.classification;

import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.TextClassifier;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.instructions.InstructionInformationProvider;
import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * How this works
 * 1.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class AIClassificator implements InstructionClassificator {

    private final InstructionInformationProvider instructionInformationProvider;

    TextClassifier<INSTRUCTIONS_TYPE> classifier;

    @PostConstruct
    public void initialize() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Map<INSTRUCTIONS_TYPE, List<String>> instructionsTypeListMap = instructionInformationProvider.loadExamples();
        classifier = new EmbeddingModelTextClassifier<>(embeddingModel, instructionsTypeListMap, 1, 0.65, 0.5);
    }

    @Override
    public INSTRUCTIONS_TYPE classify(String instructions) {
        List<INSTRUCTIONS_TYPE> classified = classifier.classify(instructions);

        if (classified.isEmpty()) {
            throw new ApiException("No classifiers were found", HttpStatus.BAD_REQUEST);
        }

        if (classified.size() > 1) {
            throw new ApiException("More that one classifier were found. Currently not supported", HttpStatus.BAD_REQUEST);
        }

        return classified.get(0);
    }
}
