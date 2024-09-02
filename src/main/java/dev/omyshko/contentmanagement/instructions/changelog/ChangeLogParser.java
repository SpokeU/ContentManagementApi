package dev.omyshko.contentmanagement.instructions.changelog;

import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;

public interface ChangeLogParser {

    ChangeLog extract(String text);

}
