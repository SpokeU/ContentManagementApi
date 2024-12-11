package dev.omyshko.contentmanagement.tasks;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.omyshko.contentmanagement.api.model.TaskStatus;
import dev.omyshko.contentmanagement.core.service.TaskActivityLogService;
import dev.omyshko.contentmanagement.core.utils.ContentService;
import dev.omyshko.contentmanagement.core.utils.ContentStringUtils;
import dev.omyshko.contentmanagement.instructions.InstructionsProcessingService;
import dev.omyshko.contentmanagement.instructions.ResponseProcessor;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseService;
import dev.omyshko.contentmanagement.tasks.entity.TaskEntity;
import dev.omyshko.contentmanagement.tasks.repository.TasksLocalRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class TaskProcessor {

    private final TasksLocalRepository tasksRepository;

    private final TaskActivityLogService taskActivityLogService;

    private final ContentService contentService;

    private final KnowledgeBaseService knowledgeBaseService;

    private final ChatLanguageModel chatLanguageModel;

    private final InstructionsProcessingService instructionsProcessingService;

    public TaskProcessor(TasksLocalRepository tasksRepository, TaskActivityLogService taskActivityLogService, ContentService contentService, KnowledgeBaseService knowledgeBaseService, ChatLanguageModel chatLanguageModel, InstructionsProcessingService instructionsProcessingService) {
        this.tasksRepository = tasksRepository;
        this.taskActivityLogService = taskActivityLogService;
        this.contentService = contentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.chatLanguageModel = chatLanguageModel;
        this.instructionsProcessingService = instructionsProcessingService;
    }

    @Async
    public CompletableFuture<String> processAsync(String taskId) {
        TaskEntity taskEntity = tasksRepository.find(taskId);
        if (!TaskStatus.READY_FOR_PROCESSING.equals(taskEntity.getTaskStatus())) {
            return CompletableFuture.completedFuture("Cannot process task in status " + taskEntity.getTaskStatus());
        }

        return CompletableFuture.completedFuture(processTask(taskEntity));
    }

    private String processTask(TaskEntity taskEntity) {
        //Get content of all links and replace it in original task
        String fullTask = contentService.unwrapLinks(taskEntity.getContent());
        taskActivityLogService.write(taskEntity.getId(), TaskActivityLogService.Action.INTERMEDIATE_PROCESSING_RESULT, fullTask, "Links unwrapped");

        //Using knowledge base searches relevant info to complete a task
        String knowledgeBaseMaterials = knowledgeBaseService.search(fullTask);
        taskActivityLogService.write(taskEntity.getId(), TaskActivityLogService.Action.INTERMEDIATE_PROCESSING_RESULT, knowledgeBaseMaterials, "Knowledge base materials");

        //Merge task and knowledge base info into single string
        String projectInfo = ContentStringUtils.wrap(generateTaskProjectInfo(taskEntity), "project-info");//TODO rename
        String taskWrapped = ContentStringUtils.wrap(fullTask, "task");
        String kbMaterialsWrapped = ContentStringUtils.wrap(knowledgeBaseMaterials, "knowledge-base");
        String taskWithKnowledge = String.join("\n", projectInfo, taskWrapped, kbMaterialsWrapped);

        //Send task + knowledge to LLM processing for text result
        //Stores all LLM logs and activity under TaskStartResponse.taskId
        String aiResponse = chatLanguageModel.generate(taskWithKnowledge);
        taskActivityLogService.writeLLMInteraction(taskEntity.getId(), TaskActivityLogService.Action.LLM_INTERACTION, taskWithKnowledge, aiResponse, "Task solving");

        //Process result of LLM (Apply change log to push content to git)
        ResponseProcessor.ProcessingResult processingResult = instructionsProcessingService.processInstructions(aiResponse);

        return String.join("\n ======================= \n", taskWithKnowledge, aiResponse/*, processingResult.message()*/);
    }

    //TODO rename
    private String generateTaskProjectInfo(TaskEntity taskEntity) {
        return String.join("\n",
                "Project: " + taskEntity.getProject().getName(),
                "Component: " + taskEntity.getComponent().getName(),
                "Branch: " + generateBranchName(taskEntity));

    }

    private String generateBranchName(TaskEntity taskEntity) {
        return "feature/" + taskEntity.getId();
    }

}
