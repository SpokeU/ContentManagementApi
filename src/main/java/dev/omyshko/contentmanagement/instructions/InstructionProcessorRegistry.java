package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class InstructionProcessorRegistry {

    private final Map<INSTRUCTIONS_TYPE, InstructionsProcessor> processorMap = new EnumMap<>(INSTRUCTIONS_TYPE.class);

    private final List<InstructionsProcessor> processors;

    @PostConstruct
    public void init() {
        // Register each processor with its corresponding enum type
        for (InstructionsProcessor processor : processors) {
            processorMap.put(processor.getProcessedType(), processor);
        }
    }

    public InstructionsProcessor getProcessor(INSTRUCTIONS_TYPE type) {
        InstructionsProcessor instructionProcessor = processorMap.get(type);

        if (instructionProcessor == null) {
            throw new ApiException("No such instruction processor type: " + type, HttpStatus.BAD_REQUEST);
        }

        return instructionProcessor;
    }
}
