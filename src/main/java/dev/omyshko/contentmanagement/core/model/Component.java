package dev.omyshko.contentmanagement.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents part of a project (MicroserviceA, Frontend, Documentation etc.)
 */
@AllArgsConstructor
@Data
public class Component {

    private String name;
    private String description;
    private ComponentType type;
    private String location;

}
