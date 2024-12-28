package dev.omyshko.contentmanagement.api.endpoint.test;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

import java.util.List;

public class BlocksModelSchemas {

    interface JavaBlocksExtractor {

        @SystemMessage("You are a tool for processing a code. Provided a file content please extract blocks according to schema. Extract only entities present in provided file.")
        @dev.langchain4j.service.UserMessage("Here is the file content <file-content>{{file_content}}</file-content>")
        JavaBlocks extractBlocks(@V("file_content") String fileContent);

    }

    public record JavaBlocks(List<ClassBlock> classes, List<MethodBlock> methods) {

    }

    @Description("A block which represents java Class")
    record ClassBlock(@Description("Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`") String id,
                      @Description("Should be always equal to `class`") String name,
                      @Description("Field values") ClassFields fields,
                      @Description("Additional classifiers for text block. Can be one of: ### Controller\n" +
                              "### SpringBean\n" +
                              "### Entity\n" +
                              "### Repository") List<String> classifiers, @Description("List all dependencies based on schema") ClassDependencies dependencies) {
    }

    record ClassFields(@Description("Example - `dev.omyshko.contentmanagement.core.service`") String class_package,
                       @Description("Class name. Example - GitContentManager") String className,
                       @Description("""
            Important! Class content with All methods should be replaced with `...` 
            Should include only package declaration, annotations,class declaration, variables and static blocks. 
            Example -
                    ```
                    package dev.omyshko.contentmanagement.core.service;
                    import dev.omyshko.contentmanagement.core.model.Component;
                    import dev.omyshko.contentmanagement.core.model.Project;

                    @Slf4j
                    @Service
                    public class GitContentManager {

                        String username = "SpokeU";

                        @Value("${GIT_API_KEY}")
                        String password;

                        private final String storagePath;
                        private final ProjectsRepository projectsRepo;

                        public GitContentManager(@Value("${app.storage.path:}") String storagePath, ProjectsRepository projects) {
                            this.storagePath = storagePath;
                            this.projectsRepo = projects;
                        }

                        ...

                    }
                    ```
             """) String content) {
    }

    record ClassDependencies(@Description("""
            Declared methods fully qualified signatures
            For example: 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(Long, UserEntity)'
            """) List<String> declaresMethod) {
    }


    /*----------------------------------------*/


    @Description("A block which represents java method")
    record MethodBlock(@Description("Fully qualified method signature name. For example `dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity, Long)`") String id,
                       @Description("Should be always equal to `method`") String name,
                       @Description("Field values") MethodFields fields,
                       @Description("Additional classifiers for text block. Can be one of: ### EntryPoint - Any functionality that can be triggered. For example APIs, Jobs, Messaging can be treated as EntryPoint.\n" +
                               "### RequestHandler\n" +
                               "### getter\n" +
                               "### setter") List<String> classifiers, MethodDependencies dependencies) {
    }

    record MethodFields(@Description("""
            Might be an array because if contains generics
            "Example - `dev.omyshko.usermanagementsystem.api.model.UserApiModel`"""
    ) List<String> return_type,
                        @Description("Example - `toApiModel(UserEntity, Long)`") String method_signature,
                        @Description("""
                                       Should include a method with javadoc if available
                                          ```java
                                          /**
                                           * Creating a user
                                           */
                                          @PostMapping
                                          public ResponseEntity<UserApiModel> createUser(@RequestBody UserEntity userEntity) {
                                              UserEntity savedUser = userService.saveUser(userEntity);
                                              return ResponseEntity.ok(toApiModel(savedUser));
                                          }
                                          ```
                                """) String content) {
    }

    record MethodDependencies(@Description("""
                        Description: Input arguments for this method. Might be an array because if contains generics
                        Value: List of fully qualified argument names
            """) List<String> inputArgument,
                              @Description("""
                                                   Fully qualified return type. Might be an array because if return type contains generics.
                                                   Example: [`org.springframework.http.ResponseEntity`, `dev.omyshko.usermanagementsystem.api.model.UserApiModel`]
                                      """) List<String> returnType,
                              @Description("""
                                                   If this method calls other method. Should be {Fully_qualified_class}#{method_signature}. Example: `['dev.omyshko.usermanagementsystem.service.UserService#saveUser(UserEntity)', 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)']`
                                      """) List<String> calls) {
    }

}
