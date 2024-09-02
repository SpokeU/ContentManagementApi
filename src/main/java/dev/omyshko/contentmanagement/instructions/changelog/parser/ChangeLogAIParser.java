package dev.omyshko.contentmanagement.instructions.changelog.parser;

import dev.langchain4j.service.UserMessage;
import dev.omyshko.contentmanagement.instructions.changelog.ChangeLogParser;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;

/**
 * This one is veeery slow (~15 seconds) and very unreliable.
 * Also it can change a data while doing transformation (because its Generative AI)
 */
public interface ChangeLogAIParser extends ChangeLogParser {

    @UserMessage("Extract Change Log from: {{it}}")
    ChangeLog extract(String text);

}
