package dev.omyshko.contentmanagement.knowledgebase;

import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.core.utils.ContentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service class to provide information from Knowledge Base
 */
@Slf4j
@Component
public class KnowledgeBaseInformationProvider {

    public static final Pattern LINK_REGEX = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");

    private final String knowledgeBasePath;

    private final ContentService contentService;

    /**
     * Folder is an absolute path
     */
    private Map<String, Path> kbTopicCodeToFolder = new HashMap<>();

    public KnowledgeBaseInformationProvider(@Value("${app.knowledge-base.path:}") String knowledgeBasePath, ContentService contentService) {
        this.knowledgeBasePath = knowledgeBasePath;
        this.contentService = contentService;
    }

    @PostConstruct
    public void init() {
        Map<String, Path> kbTopicCodeToFolder = new HashMap<>();

        try (Stream<Path> paths = Files.walk(getBaseFolderPath())) {
            // Filter for 'index.md' files and process them
            paths.filter(Files::isRegularFile)            // Select only regular files
                    .filter(path -> path.getFileName().toString().equals("index.md")) // Look for 'index.md'
                    .forEach(p -> {
                        String topicCode = extractTopicCode(p);
                        if (StringUtils.isNotBlank(topicCode)) {
                            kbTopicCodeToFolder.put(topicCode, p.getParent());
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.kbTopicCodeToFolder = kbTopicCodeToFolder;
    }

    public String getTableOfContent() {
        Path rootPath = getBaseFolderPath();


        try (Stream<Path> paths = Files.list(rootPath)) {
            String knowledgeBaseInstructions = readIndexFile(rootPath);

            // Filter for directories in the top level and check for 'index.md'
            String directoryContent = paths.filter(Files::isDirectory) // Select only directories
                    .map(KnowledgeBaseInformationProvider::processMainDirectory)
                    .collect(Collectors.joining("\n"));// Process each top-level directory

            return String.join("\n", knowledgeBaseInstructions, directoryContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Reads index.md file in folder that is related to topic code.
     * Replaces all the links in index.md file with actual content.
     * All other files in a folder is ignored
     */
    public String getTopicInfo(String topicCode) {
        StringBuffer result = new StringBuffer();
        //1. Read index file
        Path topicFolder = kbTopicCodeToFolder.get(topicCode);

        if (topicFolder == null) {
            throw new ApiException("No topic found with code:" + topicCode, HttpStatus.BAD_REQUEST);
        }

        String indexFileContent = readIndexFile(topicFolder);

        //2. Replace links with actual content
        return contentService.unwrapLinks(indexFileContent, topicFolder);
    }

    /**
     * TODO Refactor KB and Create KnowledgeNode
     *
     * <pre>
     * public class KnowledgeNode {
     *     private String code;
     *     private Map<String, KnowledgeNode> children;
     *     public void addChild(String name, KnowledgeNode child) {
     *         this.children.put(name, child);
     *     }
     * }
     * </pre>
     *
     * @param pageCode
     * @return
     */
/*    public String getPageContent(String pageCode) {
        return null;
    }*/

    /**
     * Temporary solution until KnowledgeNodes are implemented
     *
     * @param filePath
     * @return
     */
    public KnowledgeBasePageNode getPageContent(String filePath) {
        Path path = Paths.get(getBaseFolderPath().toString(), filePath);
        return new KnowledgeBasePageNode(contentService.getLocalContent(path));
    }

    private @NotNull Path getBaseFolderPath() {
        try {
            return Paths.get(this.getClass().getClassLoader().getResource(knowledgeBasePath).toURI());
        } catch (URISyntaxException e) {
            log.error("Cannot find knowledge base folder", e);
            throw new RuntimeException(e);
        }
    }

    private static String processMainDirectory(Path dir) {

        String result = readIndexFile(dir);
        try (Stream<Path> paths = Files.list(dir)) {
            String secondaryDirectoryContent = paths.filter(Files::isDirectory)
                    .map(KnowledgeBaseInformationProvider::readIndexFile)
                    .collect(Collectors.joining("\n"));
            result = String.join("\n", result, secondaryDirectoryContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Finds index.md file and adds returns its content
     *
     * @param dir
     */
    private static String readIndexFile(Path dir) {
        Path indexMdPath = dir.resolve("index.md"); // Get path to 'index.md'
        if (Files.exists(indexMdPath) && Files.isRegularFile(indexMdPath)) {
            log.debug("Found index.md at: " + indexMdPath.toString());
            try {
                List<String> fileContent = Files.readAllLines(indexMdPath);
                String concatenatedContent = String.join("\n", fileContent);
                return concatenatedContent;
            } catch (IOException e) {
                log.error("Error reading file: " + indexMdPath);
                e.printStackTrace();
            }
        } else {
            log.warn("No index.md found in: " + dir.toString());
        }

        return "";
    }

    private String extractTopicCode(Path filePath) {
        try {
            return Files.readAllLines(filePath)
                    .stream()
                    .filter(line -> line.contains("code:"))
                    .map(line -> line.split(":")[1])
                    .findFirst()
                    .map(s -> s.replace("`", ""))
                    .orElse("")
                    .trim();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record KnowledgeBasePageNode(String content){

    }

}
