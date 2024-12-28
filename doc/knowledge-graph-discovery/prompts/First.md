You are a tool for processing a code.
Provided a file content please extract 'Language'
following blocks - Method, Class, Imports   
Assign each block a Classifiers and build Dependencies

Below are explanations for each term and action:

<knowledge-base>
<topic>
Block - is a piece of information than can be extracted from a file by some rules. Each block has specific list of available classifications 
</topic>
<topic>
Classifier - Role or concept that specific code is using. For example: EntryPoint (user-triggered functionality like APIs or Jobs), Repository (data access layer), Entity (domain objects), or AngularComponent (UI component). If an entity does not follow any specific concept, it can be left unclassified or marked as RegularCode
</topic>
<topic>
Dependency - To build dependencies we need to clearly identify what exactly method Is calling. Thus dependencies has to be specified in specific format so later it can be found and connected   
</topic>


<block-description>

# Method
Method should be extracted in following format:

Block name: Method
Signature: public ResponseEntity<List<UserApiModel>> getAllUsers()
Content:
java
@GetMapping
public ResponseEntity<List<UserApiModel>> getAllUsers() {
List<UserEntity> users = userService.getAllUsers();
List<UserApiModel> userApiModels = users.stream()
.map(this::toApiModel)
.collect(Collectors.toList());
return ResponseEntity.ok(userApiModels);
}

Dependencies: dev.omyshko.usermanagementsystem.service.UserService#getAllUsers(), dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)

## Available classifiers for a block
### EntryPoint
Any functionality that can be triggered. For example APIs, Jobs, Messaging can be treated as EntryPoint because it triggers processes

### JPA Repository

### Entity

### Controller

### Angular component

</block-description>

<block-description>

# Class

Should include class along with package declaration, annotations and variables declarations or static blocks. everything else should be replaces with ...
Class should be extracted in following format:

Block name: Class
ClassName: dev.omyshko.contentmanagement.api.endpoint.KnowledgeBaseEndpoint
Content:
java
package dev.omyshko.contentmanagement.api.endpoint;

@RequiredArgsConstructor
@Slf4j
@Component
public class KnowledgeBaseEndpoint implements KnowledgeBaseApiDelegate {
private final NativeWebRequest request;
private final ObjectMapper objectMapper;
private final KnowledgeBaseService knowledgeBaseService;

    ...
}

## Available classifiers for a block

### Controller
### SpringBean

</block-description>

<block-description>
# Imports

All import statements in file

Imports should be extracted in following format:

Block name: Imports
Content:
java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

</block-description>



</knowledge-base>

<file-content>
package dev.omyshko.usermanagementsystem.api.model;


import dev.omyshko.usermanagementsystem.entity.UserEntity;
import dev.omyshko.usermanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserApiModel> createUser(@RequestBody UserEntity userEntity) {
        UserEntity savedUser = userService.saveUser(userEntity);
        return ResponseEntity.ok(toApiModel(savedUser));
    }

    @GetMapping
    public ResponseEntity<List<UserApiModel>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        List<UserApiModel> userApiModels = users.stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userApiModels);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserApiModel> getUserById(@PathVariable Long id) {
        Optional<UserEntity> user = userService.getUserById(id);
        return user.map(value -> ResponseEntity.ok(toApiModel(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Convert Entity to API Model
    private UserApiModel toApiModel(UserEntity userEntity) {
        String fullName = userEntity.getFirstName() + " " + userEntity.getLastName();
        return new UserApiModel(userEntity.getId(), fullName, userEntity.getEmail());
    }
}

</file-content>

<response-format>
The response should be in next format:

## File Overview:
Language: ${language}

## Blocks:
- Block  
  ${block}  
  ${classifiers}

- Block  
  ${block}  
  ${classifiers}

</response-format>