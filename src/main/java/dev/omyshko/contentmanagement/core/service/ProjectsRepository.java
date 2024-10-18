package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.ComponentType;
import dev.omyshko.contentmanagement.core.model.Project;

import java.util.List;

public interface ProjectsRepository {

    List<Project> findAll();

    Project find(String projectName);

    Component find(String projectName, String componentName);
}
