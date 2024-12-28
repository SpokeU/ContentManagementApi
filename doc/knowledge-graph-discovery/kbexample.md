# Java

## Blocks
Below are the list of blocks that can be extracted from provided file. Each block has available classifiers and dependencies  

### Class

Should include class along with constructor, package declaration, annotations and variables declarations or static blocks. Imports, Methods etc should be replaces with ...
Class should be extracted in following format:


Block name: Class
Block Id: #{Refer to concept of identification}
ClassName: dev.omyshko.contentmanagement.api.endpoint.KnowledgeBaseEndpoint
Content:
```java
package dev.omyshko.contentmanagement.api.endpoint;

@RequiredArgsConstructor
@Slf4j
@Component
public class KnowledgeBaseEndpoint implements KnowledgeBaseApiDelegate {
    private final NativeWebRequest request;
    private final ObjectMapper objectMapper;
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseEndpoint(NativeWebRequest request, ObjectMapper objectMapper, KnowledgeBaseService knowledgeBaseService) {
        this.request = request;
        this.objectMapper = objectMapper;
        this.knowledgeBaseService = knowledgeBaseService;
    }
    
    ...
}
```

#### Classifiers

- Controller
- SpringBean
- Entity
- Repository


### Imports

All import statements in file

Imports should be extracted in following format:

Block name: Imports
Content:
```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
```

### Method
Method should be extracted in following format:

Block name: Method
Signature: public ResponseEntity<List<UserApiModel>> getAllUsers()
Content:
```java
@PostMapping
public ResponseEntity<UserApiModel> createUser(@RequestBody UserEntity userEntity) {
    UserEntity savedUser = userService.saveUser(userEntity);
    return ResponseEntity.ok(toApiModel(savedUser));
}
```
Dependencies: `dev.omyshko.usermanagementsystem.service.UserService#getAllUsers()`, `dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)`

#### Dependencies
Type: CALLS  
Value: {Fully_qualified_class}#{method_signature}  
Example: `dev.omyshko.usermanagementsystem.service.UserService#getAllUsers()`, `dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)`

Type: INPUT_ARGUMENT  
Value: List of fully qualified argument names  
Example: dev.omyshko.usermanagementsystem.entity.UserEntity

Type: RETURN_TYPE  
Value: List of fully qualified class names 
Example: `org.springframework.http.ResponseEntity`, `dev.omyshko.usermanagementsystem.api.model.UserApiModel`

#### Classifiers
- EntryPoint 
  - Any functionality that can be triggered. For example APIs, Jobs, Messaging can be treated as EntryPoint.




Declares which dependency types this class have to more effectively build knowledge graph.
Each dependency has a type and id.   
**type** will bee used as a label when creating relation
**id** will be used to determine which node to connect to after processing of all files. Refer to concept of `id` generation as its very important
