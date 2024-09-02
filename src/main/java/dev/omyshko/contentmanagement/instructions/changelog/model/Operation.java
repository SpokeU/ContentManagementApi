package dev.omyshko.contentmanagement.instructions.changelog.model;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class Operation {
    public OperationType type;
    public String resource;
    public String content;
    public int fromLine;
    public int toLine;
}
