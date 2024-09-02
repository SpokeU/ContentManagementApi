package dev.omyshko.contentmanagement.instructions;

import dev.omyshko.contentmanagement.instructions.model.INSTRUCTIONS_TYPE;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Easy access to instruction files and its content
 */
@Slf4j
@Service
public class InstructionInformationProvider {

    private final String basePath;


    public InstructionInformationProvider(@Value("${app.instructions.path:}") String basePath) {
        this.basePath = basePath;
    }

    public Map<INSTRUCTIONS_TYPE, List<String>> loadExamples() {
        Map<INSTRUCTIONS_TYPE, List<String>> examples = Arrays.stream(INSTRUCTIONS_TYPE.values())
                .collect(Collectors.toMap(
                        type -> type,                        // Key: Enum constant
                        type -> loadExamplesFromFolder(type.getKnowledgeFolder())  // Value: List with single string
                ));

        return examples;
    }

    public List<String> loadExamplesFromFolder(String folder) {
        List<String> fileContents = new ArrayList<>();
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            Path folderPath = Paths.get(classLoader.getResource("doc/instructions/" + folder).toURI());

            try (Stream<Path> paths = Files.list(folderPath)) {
                List<Path> exampleFiles = paths
                        .filter(path -> path.getFileName().toString().startsWith("example"))
                        .toList();

                for (Path path : exampleFiles) {
                    String content = Files.readString(path);
                    fileContents.add(content);
                }

            }

        } catch (URISyntaxException | IOException | NullPointerException e) {
            // Log the error and return an empty list in case of any issue
            log.info("Error reading files from folder: {}", folder, e);
        }

        return fileContents;
    }
}
