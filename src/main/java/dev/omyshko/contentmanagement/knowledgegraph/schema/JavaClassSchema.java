package dev.omyshko.contentmanagement.knowledgegraph.schema;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * A models that are used as json_schema for llm to extract blocks information
 */
public class JavaClassSchema {

    public interface JavaClassBlocksExtractor {

        @SystemMessage("""
                You are a tool for processing a code and splitting it into structured blocks. Provided a file content with line numbers in format `001:` please extract blocks according to schema.
                Fields value instructions:
                
                - `classBlocks.declaredInnerClasses` - Capture only first level inner classes
                - `classBlocks.declaredTopLevelMethods.id` - A fully qualified method signature with fully qualified parameter names.
                    Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.GitContentManager(java.util.List,int)`
                    Should NOT contain return type and generics
                - ``
                """)
        @dev.langchain4j.service.UserMessage("Here is the file content <file-content>]\n" +
                                             "{{file_content}}" +
                                             "\n</file-content>")
        JavaClassBlocks extractBlocks(@V("file_content") String fileContent);
    }

    /**
     * When I removed this and returned JavaClassBlock everything went po puzdi
     *
     * @param classBlocks
     */
    public record JavaClassBlocks(List<JavaClassBlock> classBlocks) {
    }

    @Description("A block which represents java Class")
    public record JavaClassBlock(@Description("Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`") String id,
                                 @Description("Simple java class name like: `GitContentManager`") String name,
                                 @Description("Methods from inner classes should NOT be captured. e.g. Methods with $") List<JavaClassMethodBlock> declaredTopLevelMethods,
                                 @Description("First level inner records/classes/interfaces should be captured here") List<JavaInnerClass> declaredInnerClasses) {
    }

    @Description("Only toplevel methods should be captured. Do not capture methods from inner classes ")
    public record JavaClassMethodBlock(
            @Description("""
                    A fully qualified method signature with fully qualified parameter names.
                    Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.GitContentManager(java.util.List,int)`
                    Should NOT contain return type and generics
                    """)
            String id,
            @Description("If method has any comment or javadoc this should contain its beginning line number")
            String methodCommentStartingLine,
            @Description("Starts with JavaDoc, Annotations or any Comment before method declaration")
            String fromLineNumber,
            @Description("A line number where method closing brace `}` is located.")
            String toLineNumber) {

        public String connectionType() {
            return "contains";
        }

        public String dependencyEntityType() {
            return "java_method";
        }

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
            @Description("A line number of the last import statement in the file.")
            String toLineNumber) {
    }

    @Description("""
            A declaration of Inner `class`, `interface`, `record`. Only first level inner declarations should be extracted. 
            For example:
            public class JavaClass {
                public static class InnerClass { <-- This should be captured
                
                    public static class DoubleInnerExample { <-- This should NOT be captured 
                    
                    }
                }
            } 
            """)
    public record JavaInnerClass(
            @Description("""
                    `$` separated
                    Value is created by pattern {outer_class}`$`{inner_class}
                    Examples: `dev.example.TestEndpoint$BlocksExtractor``
                    """)
            String fullyQualifiedInnerClassName,
            @Description("A line number where inner class declaration starts. If annotations are present it is counted as declaration")
            String fromLineNumber,
            @Description("A line number of consing brace `}` of inner class declaration")
            String toLineNumber) {

        public String connectionType() {
            return "contains";
        }

        public String dependencyEntityType() {
            return "java_inner_class";
        }
    }


}
