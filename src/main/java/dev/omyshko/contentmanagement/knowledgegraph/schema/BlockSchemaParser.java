package dev.omyshko.contentmanagement.knowledgegraph.schema;

import com.google.gson.JsonObject;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.output.structured.Description;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider.KnowledgeBasePageNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to convert Knowledge page page into JsonSchema.
 * This is later to be used by LLM to extract response
 */
public class BlockSchemaParser {

    public JsonSchema convertToSchema(List<KnowledgeBasePageNode> knowledgeBasePage) {
        List<JsonObjectSchema> languageBlockSchemas = knowledgeBasePage.stream().map(p -> convert(p.content())).toList();

        JsonObjectSchema.Builder rootElement = JsonObjectSchema.builder();

        for (JsonObjectSchema languageBlockSchema : languageBlockSchemas) {
            JsonSchemaElement blocksList = JsonArraySchema.builder()
                    .description("A list of blocks parsed from a file. A **block** is a unit of information with a specific purpose and a defined structure.")
                    .items(languageBlockSchema)
                    .build();

            rootElement.addProperty("blocks", blocksList); //TODO Will be Critical. preferable to have proper name like declared_classes as its better for LLM
        }


        JsonSchema jsonSchema = JsonSchema.builder()
                .name("LanguageBlocks") // OpenAI requires specifying the name for the schema
                .rootElement(rootElement.build())
                .build();

        return jsonSchema;
    }

    public JsonSchema convertToSchema(KnowledgeBasePageNode knowledgeBasePage) {
        JsonObjectSchema languageBlockSchema = convert(knowledgeBasePage.content());

        JsonSchemaElement classesArraySchema = JsonArraySchema.builder()
                .description("A list of blocks parsed from a file. A **block** is a unit of information with a specific purpose and a defined structure.")
                .items(languageBlockSchema)
                .build();

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("LanguageBlocks") // OpenAI requires specifying the name for the schema
                .rootElement(JsonObjectSchema.builder()
                        .addProperty("blocks", classesArraySchema) //TODO preferable to have proper name like declared_classes as its better for LLM
                        .build()
                ).build();

        return jsonSchema;

    }

    public JsonObjectSchema convert(String knowledgeBasePage) {
        MarkdownSection overview = getSection(knowledgeBasePage, "Overview");

        MarkdownSection name = getSection(overview.allContent, "name");
        MarkdownSection id = getSection(overview.allContent, "id");

        MarkdownSection fieldsSection = getSection(knowledgeBasePage, "Fields");
        List<MarkdownSection> fields = getSectionlist(fieldsSection.allContent).stream().map(s -> getSection(fieldsSection.allContent(), s.name())).toList();

        MarkdownSection dependenciesSection = getSection(knowledgeBasePage, "Dependencies");
        List<MarkdownSection> dependencies = getSectionlist(dependenciesSection.allContent, dependenciesSection.level + "#").stream().map(s -> getSection(dependenciesSection.allContent(), s.name())).toList();


        //1 get Overview
        //2 get fields
        //3 get concepts
        //get dependencies

        JsonObjectSchema.Builder fieldsBuilder = JsonObjectSchema.builder().description("");//Add description what fields are
        for (MarkdownSection field : fields) {
/*            JsonArraySchema fieldArray = JsonArraySchema.builder().description()
                    .items(JsonStringSchema.builder().build())
                    .build();*/
            fieldsBuilder.addStringProperty(field.name, field.textContent);
        }
        fieldsBuilder.required(fields.stream().map(f -> f.name).toList());//TODO check for proper type : Array, String, Int

        JsonObjectSchema.Builder dependenciesBuilder = JsonObjectSchema.builder().description("List all dependencies based on schema");//Add description what fields are
        for (MarkdownSection dependency : dependencies) {
            //### calls
            //figure out if dependency is an
            JsonSchemaElement dependencyItem = JsonStringSchema.builder().build();

            List<MarkdownSection> dependencySections = getSectionlist(dependency.allContent);
            if (dependencySections.size() > 0) {
                JsonObjectSchema.Builder dependencyItemBuilder = JsonObjectSchema.builder().description(dependency.textContent);

                for (MarkdownSection dependencySection : dependencySections) {
                    String dependencySectionDescription = getSection(dependency.allContent, dependencySection.name).textContent;
                    JsonSchemaElement dependencySectionItem = JsonStringSchema.builder().description(dependencySectionDescription).build();


                    if (dependencySectionDescription.contains("type:array")) {
                        dependencySectionItem = JsonArraySchema.builder().description(dependencySectionDescription)
                                .items(JsonStringSchema.builder().build()) //The item of the dependency
                                .build();
                    }

                    dependencyItemBuilder.addProperty(dependencySection.name, dependencySectionItem);
                }

                dependencyItemBuilder.required(dependencySections.stream().map(f -> f.name).toList());
                dependencyItem = dependencyItemBuilder.build();
            }


            JsonArraySchema dependenciesArray = JsonArraySchema.builder().description(dependency.textContent)
                    .items(dependencyItem) //The item of the dependency
                    .build();
            dependenciesBuilder.addProperty(dependency.name, dependenciesArray);
        }
        dependenciesBuilder.required(dependencies.stream().map(f -> f.name).toList());


        JsonObjectSchema blockJsonSchemaObject = JsonObjectSchema.builder()
                .description(overview.textContent())
                .addStringProperty("id", id.textContent())
                .addStringProperty("name", name.textContent())
                .addProperty("fields", fieldsBuilder.build())
                //.addProperty("classifiers", classifiersArraySchema)
                .addProperty("dependencies", dependenciesBuilder.build())
                .required("id", "name", "fields", "classifiers", "dependencies")
                .build();

        return blockJsonSchemaObject;
    }

    List<Field> getFields(String knowledgeBasePage) {
        MarkdownSection fieldsSection = getSection(knowledgeBasePage, "Fields");
        List<MarkdownSection> markdownFields = getSections(fieldsSection.allContent);
        List<Field> fields = markdownFields.stream().map(s -> new Field(s.name, "string", s.allContent)).toList();
        return fields;
    }

    private List<MarkdownSection> getSections(String content) {
        return null;
    }

    private MarkdownSection getSection(String knowledgeBasePage, String sectionName) {
        List<MarkdownSection> sectionlist = getSectionlist(knowledgeBasePage);
        MarkdownSection startSection = null;
        MarkdownSection endSection = null;
        for (MarkdownSection section : sectionlist) {

            //Means we have found a section
            //And now we are checking if its precedence '###' is lower or equeal than startSection '####'
            if (startSection != null) {
                if (section.level.length() <= startSection.level.length()) {
                    endSection = section;
                    break;
                }
                //Find a section that starts with same or lower level
            }

            //We have found a section so we are recording its start startPosition
            if (section.name.trim().equals(sectionName.trim())) {
                startSection = section;
            }

        }

        if (startSection == null) {
            throw new IllegalArgumentException("No section found for " + sectionName);
        }

        //Get allContent without section header
        int sectionContentStartPosition = startSection.startPosition + startSection.label().length();
        int sectionContentEndPosition = sectionlist.indexOf(startSection) == sectionlist.size() - 1 ?
                knowledgeBasePage.length() : sectionlist.get(sectionlist.indexOf(startSection) + 1).startPosition;
        int sectionEndPosition = endSection != null && endSection.startPosition >= 0 ? endSection.startPosition() : knowledgeBasePage.length();

        String sectionContent = knowledgeBasePage.substring(sectionContentStartPosition, sectionEndPosition).trim();
        String sectionTextContent = knowledgeBasePage.substring(sectionContentStartPosition, sectionContentEndPosition).trim();

        return new MarkdownSection(startSection.level(), startSection.name(), startSection.label(), startSection.startPosition(), sectionTextContent, sectionContent);
    }

    record MarkdownSection(String level, String name, String label, Integer startPosition, String textContent, String allContent) {
    }

    private List<MarkdownSection> getSectionlist(String knowledgeBasePage) {
        List<MarkdownSection> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(#+) (.*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(knowledgeBasePage);

        while (matcher.find()) {
            sections.add(new MarkdownSection(matcher.group(1), matcher.group(2).trim(), matcher.group(0), matcher.start(), "", ""));
        }

        return sections;
    }

    private List<MarkdownSection> getSectionlist(String knowledgeBasePage, String sectionLevel) {
        List<MarkdownSection> sections = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(" + sectionLevel + ") (.*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(knowledgeBasePage);

        while (matcher.find()) {
            sections.add(new MarkdownSection(matcher.group(1), matcher.group(2).trim(), matcher.group(0), matcher.start(), "", ""));
        }

        return sections;
    }

    //getSection

/*    @Description("A block of text")
    record Block(@Description("Id of this block") String id,
                 @Description("Name") String name,
                 @Description("Field values") List<TestEndpoint.Field> fields,
                 List<String> classifiers, List<TestEndpoint.Dependency> dependencies) {
    }*/

    record Dependency(@Description("Dependency type") String type, @Description("Dependency generated id") String id) {
    }


    record Field(String name, String type, String description) {
    }

    public static String getContentByHeader(String markdown, String headerName) {
        String patternString = String.format(
                "^(#+ )%s\\n((?:(?!#+ ).*?\\n)*)",
                Pattern.quote(headerName) // Escape special characters in headerName
        );

        Pattern pattern = Pattern.compile(patternString, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(markdown);

        if (matcher.find()) {
            return matcher.group(2).trim();
        } else {
            return ""; // Or handle the case where the header is not found
        }
    }

    public static void extractHeadings(Node node) {
        // If the node is a heading, extract and print its allContent
        if (node instanceof Heading) {
            Heading heading = (Heading) node;
            String headingText = getHeadingText(heading); // Extract full allContent of the heading
            int level = heading.getLevel();
            System.out.println("Heading Level " + level + ": " + headingText);
        }

        // Recurse through child nodes
        for (Node child : node.getChildren()) {
            extractHeadings(child);
        }
    }

    // Helper method to extract full allContent from a Heading node (including any nested elements)
    public static String getHeadingText(Heading heading) {
        StringBuilder sb = new StringBuilder();
        for (Node child : heading.getChildren()) {
            sb.append(child.getChars().toString());
        }
        return sb.toString().trim(); // Combine the text of all child nodes and trim excess whitespace
    }


    /*-----------------------*/


    public static List<String> getHeaderContent(Node document, String headerName) {
        List<String> contentList = new ArrayList<>();

        // Traverse the document tree
        for (Node node : document.getChildren()) {
            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                if (heading.getText().equals(headerName)) {
                    // Get the allContent under the header
                    StringBuilder content = new StringBuilder();
                    collectContent(heading.getNext(), content, heading.getLevel());
                    contentList.add(StringUtils.trim(content.toString()));
                }
            }
        }

        return contentList;
    }

    private static void collectContent(Node node, StringBuilder content, int currentHeaderLevel) {
        if (node == null) {
            return;
        }

        if (node instanceof Heading) {
            Heading nextHeading = (Heading) node;
            if (nextHeading.getLevel() < currentHeaderLevel) {
                // Stop collecting allContent if a higher-level header is encountered
                return;
            }
        }

        // Recujjively collect allContent from child nodes and siblings
        for (Node child : node.getChildren()) {
            collectContent(child, content, currentHeaderLevel);
        }

        // Collect allContent from the current node itself
        if (node instanceof Text) {
            Text textNode = (Text) node;
            content.append(textNode.getChars());
        }


        // If the current node has a next sibling, recursively collect allContent from it
        Node nextSibling = node.getNext();
        if (nextSibling != null) {
            collectContent(nextSibling, content, currentHeaderLevel);
        }
    }


    /*asdsssssssssssssssssssssss*/

    private static void extractSection(String text, String sectionName) {
        String regex = "## " + sectionName + "\\s*(.*?)\\s*(?=##|$)";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            System.out.println(matcher.group(1).trim());
        } else {
            System.out.println("No match found for section: " + sectionName);
        }
    }

}
