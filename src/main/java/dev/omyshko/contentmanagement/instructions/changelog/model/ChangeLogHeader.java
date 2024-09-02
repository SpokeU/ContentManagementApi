package dev.omyshko.contentmanagement.instructions.changelog.model;

import dev.omyshko.contentmanagement.core.model.ComponentType;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChangeLogHeader {

    public String changeSummary;

    public String project;

    public String component;

    public String branch;

}
