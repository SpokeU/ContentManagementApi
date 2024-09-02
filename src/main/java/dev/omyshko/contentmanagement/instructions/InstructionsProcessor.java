package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;

public interface InstructionsProcessor {


    ProcessingResult process(String text);

    INSTRUCTIONS_TYPE getProcessedType();


    record ProcessingResult(String message) {
    }

}
