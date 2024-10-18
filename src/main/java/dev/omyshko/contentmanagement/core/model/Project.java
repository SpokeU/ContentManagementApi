package dev.omyshko.contentmanagement.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 *
 */
@AllArgsConstructor
@Data
public class Project {

    private String name;
    private String description;

    private List<Component> components;
    //Any additional field


    //TODO Implement
    public String getAbbreviatedName() {
        return "TEST";
    }
}
