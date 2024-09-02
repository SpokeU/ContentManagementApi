package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.core.model.ComponentType;
import org.springframework.stereotype.Service;

/**
 * CRUD operations for a content that is managed by content management api
 * <br/>
 * Project -Top level entity which is created just to group a different resources under one name which hold a single purpose. Project might consist of different components such as UI, Backend, documentation, sales-reports etc.
 * <br/>
 * Component - A part of a project such as UI, OpenApi, Backend, Diagrams etc.
 * <br/>
 * Resource -
 */
@Service
public interface ResourceService {

    void create(String project, String componentName, String resource, String content);

    String read(String project, String componentName, String resource);

    void update(String project, String componentName, String resource, String content);

    void delete(String project, String componentName, String resource, String content);

}
