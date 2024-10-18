package dev.omyshko.contentmanagement.tasks;

import dev.omyshko.contentmanagement.api.model.TaskStatus;
import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.Project;
import dev.omyshko.contentmanagement.core.service.ProjectsRepository;
import dev.omyshko.contentmanagement.core.service.TaskActivityLogService;
import dev.omyshko.contentmanagement.tasks.entity.TaskEntity;
import dev.omyshko.contentmanagement.tasks.repository.TasksLocalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static dev.omyshko.contentmanagement.core.service.TaskActivityLogService.Action.TASK_STATUS_CHANGE;

@Slf4j
@Service
public class TaskService {

    private final TaskActivityLogService taskActivityLogService;


    private final TasksLocalRepository tasksRepository;
    private final TaskProcessor taskProcessor;

    private final ProjectsRepository projectsRepository;

    public TaskService(TaskActivityLogService taskActivityLogService, TasksLocalRepository tasksRepository, TaskProcessor taskProcessor, ProjectsRepository projectsRepository) {
        this.taskActivityLogService = taskActivityLogService;
        this.tasksRepository = tasksRepository;
        this.taskProcessor = taskProcessor;
        this.projectsRepository = projectsRepository;
    }

    /**
     * Receives a task in free text format.
     */
    public String startTask(String taskContent) {
        TaskEntity task = tasksRepository.save(TaskEntity.builder()
                .taskStatus(TaskStatus.READY_FOR_PROCESSING)
                .content(taskContent)
                .build()
        );

        CompletableFuture<String> taskResult = taskProcessor.processAsync(task.getId());


        /*//Get content of all links and replace it in original task
        String fullTask = contentService.unwrapLinks(task);

        //Using knowledge base searches relevant info to complete a task
        String knowledgeBaseMaterials = knowledgeBaseService.search(fullTask);

        //Merge task and knowledge base info into single string
        String taskWrapped = ContentStringUtils.wrap(fullTask, "task");
        String kbMaterialsWrapped = ContentStringUtils.wrap(knowledgeBaseMaterials, "knowledge-base");
        String taskWithKnowledge = String.join("\n", taskWrapped, kbMaterialsWrapped);


        //Send task + knowledge to LLM processing for text result
        //Stores all LLM logs and activity under TaskStartResponse.taskId
        String aiResponse = chatLanguageModel.generate(taskWithKnowledge);

        //Process result of LLM (Apply change log to push content to git)
        ResponseProcessor.ProcessingResult processingResult = instructionsProcessingService.processInstructions(aiResponse);

        return String.join("\n ======================= \n", taskWithKnowledge, aiResponse, processingResult.message());*/
        return taskResult.join();
    }

    /**
     * This will be async in future
     */
    public TaskEntity createTask(String task, String projectName, String componentName) {
        Project project = projectsRepository.find(projectName);
        Component component = projectsRepository.find(projectName, componentName);

        TaskEntity taskEntity = TaskEntity.builder()
                .taskStatus(TaskStatus.OPEN)
                .content(task)
                .project(project)
                .component(component)
                .build();

        TaskEntity savedTask = tasksRepository.save(taskEntity);

        //Store task to a folder with id of taskId
        //It will be later picked up by processor
        taskActivityLogService.write(savedTask.getId(), TaskActivityLogService.Action.TASK_CREATED, task);

        return savedTask;
    }

    public void changeTaskStatus(String taskId, TaskStatus status, String comment) {
        switch (status) {
            case OPEN:
                log.info("Open");
                break;
            case READY_FOR_PROCESSING:
                taskActivityLogService.write(taskId, TASK_STATUS_CHANGE, status + "   \nComment:" + comment);
                tasksRepository.find(taskId).setTaskStatus(status);//Piece of shit but works for POC
                processTask(taskId);
                break;
            case WAITING_FOR_USER_INPUT:
                log.info("Waiting for user input");
        }
    }

    /**
     * Execute task synchronously and return result. This method should be called by task processor job after it has been submitted.
     */
    public TaskProcessingResult processTask(String taskId) {
        CompletableFuture<String> taskResult = taskProcessor.processAsync(taskId);
        return new TaskProcessingResult("OK");
    }

    /**
     *
     */
    public record TaskProcessingResult(String result) {
    }

}
