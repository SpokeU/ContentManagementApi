package dev.omyshko.contentmanagement.knowledgegraph.schema;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

import java.util.List;

public class KBSchemaModels {

    public interface JavaBlocksExtractor {

        @SystemMessage("You are a tool for processing a code. Provided a file content with line numbers please extract blocks according to schema. Extract only entities present in provided file.")
        @dev.langchain4j.service.UserMessage("Here is the file content <file-content>{{file_content}}</file-content>")
        JavaClassBlocks extractBlocks(@V("file_content") String fileContent);
    }

    public record JavaClassBlocks(List<JavaClassBlock> classBlocks) {
    }

    @Description("A block which represents java Class")
    public record JavaClassBlock(@Description("Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`") String id,
                                 @Description("Simple java class name like: `GitContentManager`") String name,
                                 List<JavaMethodBlock> declaredMethods, JavaClassImportBlock importBlock, List<JavaInnerClass> declaredInnerClasses) {
    }

    public record JavaClassDependencies(JavaClassImportBlock importBlock, List<JavaMethodBlock> declaredMethods, List<JavaInnerClass> declaredInnerClasses) {

    }

    @Description("If this class declares any method")
    public record JavaMethodBlock(
            @Description("""
                    A fully qualified method signature with fully qualified parameter names. 
                    Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.GitContentManager(java.lang.String,int)`
                    Should NOT contain return type
                    """)
            String fullyQualifiedSignature,
            String javaDocStartingLine,
            @Description("Starts with JavaDoc, Annotations or any Comment before method declaration")
            String fromLineNumber,
            @Description("A line number where method closing brace `}` is located.")
            String toLineNumber) {
    }

    @Description("Captures fromLine and toLine of imports block")
    public record JavaClassImportBlock(
            @Description("""
                    Fully qualified class name + '_imports'
                    Example: `dev.omyshko.contentmanagement.core.service.GitContentManager_imports`
                    """)
            String importId,
            @Description("A line number where imports starts")
            String fromLineNumber,
            @Description("A line number of the last import statement. Usually before class declaration")
            String toLineNumber) {
    }

    @Description("A declaration of Inner `class`, `interface`, `record`.")
    public record JavaInnerClass(
            @Description("""
                    `$` separated
                    Value is created by pattern {outer_class}`$`{inner_class}
                    Examples: `dev.example.TestEndpoint$BlocksExtractor`, `com.example.GitContentManager$InnerClass$NestedInnerClass`
                    """)
            String fullyQualifiedInnerClassName,
            @Description("A line number where inner class declaration starts. If annotations are present it is counted as declaration")
            String fromLineNumber,
            @Description("A line number of consing brace `}` of inner class declaration")
            String toLineNumber) {
    }

}
