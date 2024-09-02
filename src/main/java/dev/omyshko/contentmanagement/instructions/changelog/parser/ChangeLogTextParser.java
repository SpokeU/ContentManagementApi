package dev.omyshko.contentmanagement.instructions.changelog.parser;

import dev.omyshko.contentmanagement.core.model.ComponentType;
import dev.omyshko.contentmanagement.instructions.changelog.ChangeLogParser;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLogHeader;
import dev.omyshko.contentmanagement.instructions.changelog.model.Operation;
import dev.omyshko.contentmanagement.instructions.changelog.model.OperationType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Regular java string parsing
 */
@Component
public class ChangeLogTextParser implements ChangeLogParser {
    @Override
    public ChangeLog extract(String text) {
        List<String> sections = Arrays.asList(text.split("\r\n---"));
        ChangeLog.ChangeLogBuilder changeLogBuilder = ChangeLog.builder();

        String firstSection = sections.get(0).trim();
        ChangeLogHeader changeLogHeader = parseHeaderInfo(firstSection);
        changeLogBuilder.header(changeLogHeader);

        for (int i = 1; i < sections.size(); i++) {
            String section = sections.get(i).trim();
            if (!section.isEmpty()) {
                Operation operation = parseOperation(section);
                changeLogBuilder.operation(operation);
            }
        }


        return changeLogBuilder.build();
    }

    private static ChangeLogHeader parseHeaderInfo(String section) {
        ChangeLogHeader.ChangeLogHeaderBuilder changeLogHeader = ChangeLogHeader.builder();

        String[] lines = section.split("\n");
        StringBuilder summary = new StringBuilder();
        boolean isSummarySection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.contains("Summary:")) {
                isSummarySection = true;
            } else if (line.contains("Project:")) {
                changeLogHeader.project(parseFieldValue(line));
                isSummarySection = false;
            } else if (line.contains("Component:")) {
                changeLogHeader.component(parseFieldValue(line));
                isSummarySection = false;
            } else if (line.contains("Branch:")) {
                changeLogHeader.branch(parseFieldValue(line));
                isSummarySection = false;
            } else if (isSummarySection) {
                summary.append(line);
            }
        }

        return changeLogHeader.changeSummary(summary.toString()).build();
    }

    private static Operation parseOperation(String section) {
        String[] lines = section.split("\n");
        Operation operation = new Operation();
        StringBuilder contentBuilder = new StringBuilder();
        boolean isContentSection = false;

        for (String line : lines) {
            line = line;

            if (isContentSection && !line.startsWith("```")) {
                contentBuilder.append(line).append("\n"); // Keep adding to content
            } else if (line.contains("Operation:")) {
                operation.setType(OperationType.valueOf(toEnumValue(line)));
            } else if (line.contains("Resource:")) {
                operation.setResource(parseFieldValue(line));
            } else if (line.contains("fromLine:")) {
                operation.setFromLine(Integer.parseInt(parseFieldValue(line)));
            } else if (line.contains("toLine:")) {
                operation.setToLine(Integer.parseInt(parseFieldValue(line)));
            } else if (line.contains("Content:")) {
                isContentSection = true;  // Start of the content block
            }
        }

        if (!contentBuilder.isEmpty()) {
            operation.setContent(contentBuilder.toString().trim());  // Set trimmed content
        }

        return operation;
    }

    private static String parseFieldValue(String line) {
        String value = line.split(":")[1];
        value = StringUtils.stripStart(value, "*").trim();
        value = StringUtils.strip(value, "`");
        return value;
    }

    private static String toEnumValue(String line) {
        return parseFieldValue(line).toUpperCase();
    }
}
