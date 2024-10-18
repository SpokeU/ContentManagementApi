package dev.omyshko.contentmanagement.tasks.repository;

import dev.omyshko.contentmanagement.core.model.Project;
import dev.omyshko.contentmanagement.tasks.entity.TaskEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TasksLocalRepository {

    Map<String, TaskEntity> tasks = new HashMap<>();

    public TaskEntity save(Project project, dev.omyshko.contentmanagement.core.model.Component component, TaskEntity task) {
        return null;
    }

    public TaskEntity save(TaskEntity task) {

        String abbreviatedName = task.getProject().getAbbreviatedName();
        int id = ThreadLocalRandom.current().nextInt(10000);
        String taskId = String.format("%s-%04d", abbreviatedName, id);
        task.setId(taskId);

        tasks.put(taskId, task);
        return task;
    }

    public TaskEntity find(String id) {
        return tasks.get(id);
    }

}
