package dev.omyshko.contentmanagement.instructions.classification;

import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;

public interface InstructionClassificator {

    INSTRUCTIONS_TYPE classify(String instructions);

}
