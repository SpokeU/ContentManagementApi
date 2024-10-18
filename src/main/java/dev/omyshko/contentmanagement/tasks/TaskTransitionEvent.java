package dev.omyshko.contentmanagement.tasks;

import dev.omyshko.contentmanagement.api.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TaskTransitionEvent {

    private Integer taskId;
    private TaskStatus status;
    private String comment;

}
