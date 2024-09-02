package dev.omyshko.contentmanagement.api.endpoint;


import dev.omyshko.contentmanagement.api.InstructionsProcessingApiDelegate;
import dev.omyshko.contentmanagement.api.model.InstructionsProcessingResponse;
import dev.omyshko.contentmanagement.api.model.ProcessingStatusEnum;
import dev.omyshko.contentmanagement.instructions.InstructionsProcessingService;
import dev.omyshko.contentmanagement.instructions.InstructionsProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
public class InstructionProcessingEndpoint implements InstructionsProcessingApiDelegate {

    private final NativeWebRequest request;

    private final InstructionsProcessingService instructionsProcessingService;

    @Override
    public ResponseEntity<InstructionsProcessingResponse> processInstructions(String body) {
        InstructionsProcessor.ProcessingResult processingResult = instructionsProcessingService.processInstructions(body);
        return ResponseEntity.ok(new InstructionsProcessingResponse().status(ProcessingStatusEnum.PROCESSED).message(processingResult.message()));
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }
}
