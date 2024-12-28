package dev.omyshko.contentmanagement.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ContentService {

    public static final Pattern LINK_REGEX = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String unwrapLinks(String content) {
        return unwrapLinks(content, null);
    }

    public String unwrapLinks(String content, Path rootPath) {
        Matcher contentMatcher = LINK_REGEX.matcher(content);
        StringBuilder resultContent = new StringBuilder();

        while (contentMatcher.find()) {
            String linkName = contentMatcher.group(1);
            String linkPath = contentMatcher.group(2);

            String linkContent = isWebLink(linkPath) ?
                    getWebContent(linkPath) :
                    getLocalContent(Paths.get(rootPath.toString(), linkPath));


            String wrapperTag = ContentStringUtils.toDashCase(linkName);
            String wrappedContent = ContentStringUtils.wrap(Matcher.quoteReplacement(linkContent), wrapperTag);

            contentMatcher.appendReplacement(resultContent, wrappedContent);
        }

        contentMatcher.appendTail(resultContent);

        return resultContent.toString();
    }

    /**
     * Todo Multithreading etc..
     */
    public String getWebContent(String urlString) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching web content from {} ", urlString, e);
            throw new RuntimeException(e);
        }
    }

    public String getLocalContent(Path path) {
        try {
            String fileContent = new String(Files.readAllBytes(path));
            return fileContent;
        } catch (IOException e) {
            log.error("Error getting content from local path {}", path, e);
            return "";
        }
    }

    private boolean isWebLink(String link) {
        return link.startsWith("http");
    }

}
