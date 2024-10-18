package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;
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

    private final Map<RESPONSE_FORMAT, ResponseProcessor> processorMap = new EnumMap<>(RESPONSE_FORMAT.class);

    private final List<ResponseProcessor> processors;

    @PostConstruct
    public void init() {
        // Register each processor with its corresponding enum type
        for (ResponseProcessor processor : processors) {
            processorMap.put(processor.getProcessedType(), processor);
        }
    }

    public ResponseProcessor getProcessor(RESPONSE_FORMAT type) {
        ResponseProcessor instructionProcessor = processorMap.get(type);

        if (instructionProcessor == null) {
            throw new ApiException("No such instruction processor type: " + type, HttpStatus.BAD_REQUEST);
        }

        return instructionProcessor;
    }
}
