package dev.omyshko.contentmanagement.knowledgegraph.schema.v2;

import dev.omyshko.contentmanagement.knowledgegraph.schema.JavaClassSchema;

import java.util.List;

public class SingleBlocks {

    /**
     * На що розбивається `java_file` block
     * java_file -> List<java_class>
     */
    public record JavaFileChildBlocks(List<JavaClassSchema> javaClasses) {
    }


    public record JavaClassBlock(String id,
                                 String name,
                                 String contentFromLine,
                                 String contentToLine) {
    }

    /**
     * На що розбивається `java_class` block
     * java_file -> List<java_class>
     */
    public record JavaClassChildBlocks(List<JavaMethodBlock> methods, List<JavaInnerClass> innerClasses) {
    }

    public record JavaMethodBlock(
            String id,
            String methodCommentStartingLine,
            String contentFromLine,
            String contentToLine) {
    }

    public record JavaInnerClass(
            String fullyQualifiedInnerClassName,
            String contentFromLine,
            String contentToLine) {
    }

}
