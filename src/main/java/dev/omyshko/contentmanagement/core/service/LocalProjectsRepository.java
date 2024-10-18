package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.ComponentType;
import dev.omyshko.contentmanagement.core.model.Project;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class LocalProjectsRepository implements ProjectsRepository {

    private List<Project> projects = new ArrayList<>();

    @PostConstruct
    public void init() {
        projects.add(new Project(
                "DataHub",
                "Centralized platform for managing database connections, sharing queries, executing them, and presenting results in a user-friendly manner.",
                List.of(
                        new Component("DataHub API", "Java Backend service", ComponentType.BACKEND, "https://github.com/SpokeU/DataHub.git")
                )));

        projects.add(new Project(
                "Content Management API",
                "Centralized platform for managing database connections, sharing queries, executing them, and presenting results in a user-friendly manner.",
                List.of(
                        new Component("Content Management API", "Java Backend service", ComponentType.BACKEND, "https://github.com/SpokeU/ContentManagementApi.git")
                )));


        projects.add(new Project(
                "AI Playground",
                "Sample Repository for AI commits",
                List.of(
                        new Component("Backend", "Java Backend service", ComponentType.BACKEND, "https://github.com/SpokeU/AiPlayground.git")
                )));
    }

    @Override
    public List<Project> findAll() {
        return projects;
    }

    @Override
    public Project find(String projectName) {
        return projects.stream().filter(p -> p.getName().equals(projectName))
                .findFirst()
                .orElseThrow(() -> new ApiException("Project not found - " + projectName, HttpStatus.BAD_REQUEST));
    }

    @Override
    public Component find(String projectName, String componentName) {
        return find(projectName).getComponents().stream()
                .filter(p -> p.getName().equals(componentName)).findFirst()
                .orElseThrow(() -> new ApiException("Component not found - " + componentName, HttpStatus.BAD_REQUEST));
    }
}
