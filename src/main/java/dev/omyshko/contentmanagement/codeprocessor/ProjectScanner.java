package dev.omyshko.contentmanagement.codeprocessor;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.omyshko.contentmanagement.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Service
public class ProjectScanner {
    private static final long MAX_FILE_SIZE_BYTES = 100_000;// 100 KB

    private final ChatLanguageModel chatLanguageModel;

    public ProjectScanner(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public void scanProject(Path projectPath) throws IOException {//TODO change to Project

    }

    private String analyzeTechnologyOverview(Path projectPath) {
        try {
            Path buildFile = findBuildFile(projectPath);
            String buildFileContent = Files.readString(buildFile);

            return "project tech overview";
        } catch (IOException e) {
            log.error("Error reading build file {}", projectPath, e);
            throw new ApiException("Error reading build file" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }


    }

    private String analyzeProjectStructure(Path projectPath) {
        return "project structure";
    }

    private Path findBuildFile(Path projectPath) {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths.filter(file -> Files.isRegularFile(file) && isBuildFile(file))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No build file found in " + projectPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isBuildFile(Path file) {
        return file.getFileName().toString().equals("pom.xml");
    }

}
