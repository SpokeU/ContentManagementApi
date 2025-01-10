package dev.omyshko.contentmanagement.knowledgegraph.schema;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public class JavaInnerClassSchema {

    /**
     * When I removed this and returned JavaClassBlock everything went po puzdi
     * @param classBlocks
     */
    public record JavaInnerClassBlocks(List<JavaInnerClassBlock> classBlocks) {
    }

    @Description("A block which represents java inner Class")
    public record JavaInnerClassBlock(@Description("Fully qualified inner class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager$InnerClass`") String id,
                                      @Description("Simple java class name like: `GitContentManager`") String name,
                                      @Description("Methods from inner classes should NOT be captured. e.g. Methods with $") List<JavaClassMethodBlock> declaredTopLevelMethods) {
    }

    @Description("Only toplevel methods should be captured. Do not capture methods from inner classes ")
    public record JavaClassMethodBlock(
            @Description("""
                    A fully qualified method signature with fully qualified parameter names. 
                    Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.GitContentManager(java.util.List,int)`
                    Should NOT contain return type and generics
                    """)
            String fullyQualifiedSignature,
            @Description("If method has any comment or javadoc this should contain its beginning line number")
            String methodCommentStartingLine,
            @Description("Starts with JavaDoc, Annotations or any Comment before method declaration")
            String fromLineNumber,
            @Description("A line number where method closing brace `}` is located.")
            String toLineNumber) {

        public String connectionType() {
            return "contains";
        }

        public String dependencyEntityType(){
            return "java_method";
        }

    }

}
