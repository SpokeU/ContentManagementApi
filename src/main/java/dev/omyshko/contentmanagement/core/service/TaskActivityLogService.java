package dev.omyshko.contentmanagement.core.service;

import j2html.TagCreator;
import j2html.tags.Text;
import j2html.tags.specialized.DivTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static j2html.TagCreator.*;

@Slf4j
@Component
public class TaskActivityLogService {

    private final String storagePath;

    private final String LOG_FOLDER_NAME = "logs";

    private final String TASKS_LOG_FOLDER_NAME = "tasks";

    private final String LOG_ENTRY_STYLE = "border: 1px solid; margin-bottom: 20px";

    private final String METADATA_STYLE = "background-color: rgba(156,151,164,0.2); padding: 10px; margin-bottom: 20px";

    public TaskActivityLogService(@Value("${app.storage.path:}") String storagePath) {
        this.storagePath = storagePath;
    }

    public void writeLLMInteraction(String taskId, Action action, String request, String response, String... additionalMetadata) {
        String LLM_REQUEST_STYLE = "background-color: rgba(16,164,0,0.1)";
        String LLM_RESPONSE_STYLE = "background-color: rgba(0,3,164,0.1)";
        String logContent = div(
                div(rawHtml(request + "\n")).attr("style", LLM_REQUEST_STYLE),
                div(rawHtml(response + "\n")).attr("style", LLM_RESPONSE_STYLE)
        ).render();

        write(taskId, action, logContent, additionalMetadata);
    }

    public void write(String taskId, Action action, String content, String... additionalMetadata) {
        Path filePath = Paths.get(storagePath, LOG_FOLDER_NAME, TASKS_LOG_FOLDER_NAME, taskId + ".md");
        try {
            ActivityLogMetadata activityLogMetadata = generateMetadata(action);

            DivTag metadata = convertMetadataToHeader(activityLogMetadata, Arrays.stream(additionalMetadata).toList());
            DivTag logContent = div().with(rawHtml(content));
            String logEntry = div(metadata, logContent).attr("style", LOG_ENTRY_STYLE).render();


            Files.createDirectories(filePath.getParent());
            Files.write(filePath, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write Activity Log: '{}' to {}", content, filePath, e);
        }
    }

    private DivTag convertMetadataToHeader(ActivityLogMetadata activityLogMetadata, List<String> additionalMetadata) {
        return div(text("Date:" + activityLogMetadata.localDateTime.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                br(),
                text("Action:" + activityLogMetadata.action),
                br()
        )
                .attr("style", METADATA_STYLE)
                .with(additionalMetadata.stream().map(TagCreator::text));
    }

    private ActivityLogMetadata generateMetadata(Action action) {
        return new ActivityLogMetadata(LocalDateTime.now(), action);
    }

    private record ActivityLogMetadata(LocalDateTime localDateTime, Action action) {
    }

    public enum Action {
        TASK_CREATED, TASK_STATUS_CHANGE, INTERMEDIATE_PROCESSING_RESULT, LLM_INTERACTION, USER_INPUT
    }

}
