package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.instructions.classification.InstructionClassificator;
import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InstructionsProcessingService {

    private final InstructionClassificator classificator;

    private final InstructionProcessorRegistry registry;

    public ResponseProcessor.ProcessingResult processInstructions(String instructions) {
        RESPONSE_FORMAT type = classificator.classify(instructions);
        ResponseProcessor processor = registry.getProcessor(type);
        ResponseProcessor.ProcessingResult processingResult = processor.process(instructions);

        return processingResult;
    }


}
