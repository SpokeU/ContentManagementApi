package dev.omyshko.contentmanagement.tasks.entity;

import dev.omyshko.contentmanagement.api.model.TaskStatus;
import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.Project;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class TaskEntity {

    private String id;

    private TaskStatus taskStatus;

    private String content;
    
    private Project project;

    private Component component;

    @Singular
    private List<Comment> comments;


    public record Comment(LocalDateTime createdDate, String author, String content) {
    }

}
