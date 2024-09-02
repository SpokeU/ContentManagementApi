package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.api.exception.ApiException;
import dev.omyshko.contentmanagement.core.model.Component;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ідуя не дуже бо працює з одним файлон за раз
 * This service supports access only to local file system
 */
@AllArgsConstructor
@Service
public class LocalFileResourceService implements ResourceService {

    private final ProjectsRepository projects;

    @Override
    public void create(String project, String componentName, String resource, String content) {
        Component component = projects.find(project, componentName);
        String basePath = component.getLocation();
        try {
            Path filePath = Paths.get(basePath, resource);

            Path parentDir = filePath.getParent();

            if (parentDir != null) {
                Files.createDirectories(parentDir);  // Creates directories if they don't exist
            }

            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            throw new ApiException("Error during file creation. Project: " + project + " Component: " + componentName + " Resource:" + resource, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public String read(String project, String componentName, String resource) {
        return "NOT_IMPLEMENTED";
    }

    @Override
    public void update(String project, String componentName, String resource, String content) {

    }

    @Override
    public void delete(String project, String componentName, String resource, String content) {

    }
}
