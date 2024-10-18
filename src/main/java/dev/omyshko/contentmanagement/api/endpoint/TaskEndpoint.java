package dev.omyshko.contentmanagement.api.endpoint;

import dev.omyshko.contentmanagement.api.TasksApiDelegate;
import dev.omyshko.contentmanagement.api.model.ChangeTaskStatusRequest;
import dev.omyshko.contentmanagement.api.model.Task;
import dev.omyshko.contentmanagement.core.model.Project;
import dev.omyshko.contentmanagement.core.service.ProjectsRepository;
import dev.omyshko.contentmanagement.tasks.TaskService;
import dev.omyshko.contentmanagement.tasks.entity.TaskEntity;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@AllArgsConstructor
@Component
public class TaskEndpoint implements TasksApiDelegate {

    private final NativeWebRequest request;

    private final TaskService taskService;

    /**
     * Receives a task in free text format.
     * Unwrap links and add its content to a task
     * Using knowledge base searches relevant info to complete a task
     * Stores all LLM logs and activity under TaskStartResponse.taskId
     */
    @Override
    public ResponseEntity<String> startTask(String body) {
        String taskExecutionResult = taskService.startTask(body);

        return ResponseEntity.ok(taskExecutionResult);
    }

    @Override
    public ResponseEntity<Task> createTask(String body, String projectName, String componentName) {
        TaskEntity task = taskService.createTask(body, projectName, componentName);

        return ResponseEntity.ok(new Task().id(task.getId()));
    }

    @Override
    public ResponseEntity<Task> changeTaskStatus(String id, ChangeTaskStatusRequest changeTaskStatusRequest) {
        taskService.changeTaskStatus(id, changeTaskStatusRequest.getStatus(), changeTaskStatusRequest.getComment());
        return TasksApiDelegate.super.changeTaskStatus(id, changeTaskStatusRequest);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }
}
