package dev.omyshko.contentmanagement.instructions.classification;

import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;

public interface InstructionClassificator {

    RESPONSE_FORMAT classify(String instructions);

}
