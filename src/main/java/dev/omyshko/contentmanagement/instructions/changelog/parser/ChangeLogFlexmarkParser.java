package dev.omyshko.contentmanagement.instructions.changelog.parser;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import dev.omyshko.contentmanagement.core.model.ComponentType;
import dev.omyshko.contentmanagement.instructions.changelog.ChangeLogParser;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;
import dev.omyshko.contentmanagement.instructions.changelog.model.Operation;
import dev.omyshko.contentmanagement.instructions.changelog.model.OperationType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * User flexmark library to parse changelog
 */
public class ChangeLogFlexmarkParser implements ChangeLogParser {

    Parser parser;

    @PostConstruct
    public void init() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
    }

    @Override
    public ChangeLog extract(String texta) {
        Node document = parser.parse(texta);
        ChangeLog.ChangeLogBuilder changeLogBuilder = ChangeLog.builder();

        for (Node node : document.getChildren()) {
            if (node instanceof Paragraph) {
                String text = ((Paragraph) node).getChars().toString();

                // Check for headers or important fields
               /* if (text.startsWith("**Change Summary:**")) {
                    changeLogBuilder.changeSummary(extractValueFromMarkdown(text));
                } else if (text.startsWith("**Project:**")) {
                    changeLogBuilder.project(extractValueFromMarkdown(text));
                } else if (text.startsWith("**Component:**")) {
                    changeLogBuilder.component(ComponentType.valueOf(extractValueFromMarkdown(text).toUpperCase()));
                }*/
            } else if (node instanceof BulletList) {
                for (Node item : node.getChildren()) {
                    String text = ((Paragraph) item.getFirstChild()).getChars().toString();
                    // Handle bullet list items
                }
            } else if (node instanceof ThematicBreak) {
                // Separator between operations (---)

                Operation operation = parseOperation(node);
                changeLogBuilder.operation(operation);

            }
        }

        return changeLogBuilder.build();
    }

    private static String extractValueFromMarkdown(String text) {
        return text.split(":", 2)[1].trim();
    }

    private static Operation parseOperation(Node node) {
        Operation operation = new Operation();

        // Look through sibling nodes for content related to this operation
        for (Node sibling = node.getNext(); sibling != null; sibling = sibling.getNext()) {
            if (sibling instanceof Paragraph) {
                String text = ((Paragraph) sibling).getChars().toString();

                // Extract operation details (type, resource, fromLine, toLine)
                if (text.startsWith("Operation:")) {
                    operation.setType(OperationType.valueOf(extractValueFromOperation(text)));
                } else if (text.startsWith("Resource:")) {
                    operation.setResource(extractValueFromMarkdown(text));
                } else if (text.startsWith("fromLine:")) {
                    operation.setFromLine(Integer.parseInt(extractValueFromMarkdown(text)));
                } else if (text.startsWith("toLine:")) {
                    operation.setToLine(Integer.parseInt(extractValueFromMarkdown(text)));
                }
            }
            // Parse content block (code block or other content)
            else if (sibling instanceof FencedCodeBlock) {
                String codeContent = ((FencedCodeBlock) sibling).getContentChars().toString();
                operation.setContent(codeContent);
            }
            // Stop at the next ThematicBreak
            else if (sibling instanceof ThematicBreak) {
                break; // Exit as the next operation is starting
            }
        }

        return operation;
    }

    private static String extractValueFromOperation(String text) {
        return text.split("`")[1].trim();
    }
}
