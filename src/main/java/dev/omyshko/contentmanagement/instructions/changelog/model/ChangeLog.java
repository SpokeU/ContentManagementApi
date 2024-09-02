package dev.omyshko.contentmanagement.instructions.changelog.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Data
public class ChangeLog {

    private ChangeLogHeader header;

    @Singular
    public List<Operation> operations;
}
