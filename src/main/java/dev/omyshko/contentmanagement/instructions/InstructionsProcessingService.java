package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.instructions.classification.InstructionClassificator;
import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InstructionsProcessingService {

    private final InstructionClassificator classificator;

    private final InstructionProcessorRegistry registry;

    public InstructionsProcessor.ProcessingResult processInstructions(String instructions) {
        INSTRUCTIONS_TYPE type = classificator.classify(instructions);
        InstructionsProcessor processor = registry.getProcessor(type);
        InstructionsProcessor.ProcessingResult processingResult = processor.process(instructions);

        return processingResult;
    }


}
