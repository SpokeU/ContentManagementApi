package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;

public interface ResponseProcessor {


    ProcessingResult process(String text);

    RESPONSE_FORMAT getProcessedType();


    record ProcessingResult(String message) {
    }

}
