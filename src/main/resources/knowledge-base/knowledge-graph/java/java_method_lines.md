## Overview
Represents java methods

### name
Should always be a string 'java_method'  
### id
Fully qualified method signature name. For example `dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity, Long)`

## Fields

### return_type
Might be an array because if contains generics
Example - `dev.omyshko.usermanagementsystem.api.model.UserApiModel`

### method_signature
Example - `toApiModel(UserEntity, Long)`

### content
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
## Dependencies

### content_lines

#### from_line
Line number where this method starts. javadoc or annotations should be captured as well

#### to_line
Line where this method ends.

### input_argument
Input arguments for this method. Might be an array because if contains generics
Value: List of fully qualified argument names  
Example: [`dev.omyshko.usermanagementsystem.entity.UserEntity`]

### return_types
type:array
Fully qualified return types of each type you can extract. If return type is using generics then array should contain each type separately
! Example: `java.util.Optional<com.webdev.dataviewer.entity.ConnectionEntity>` will be extracted to [`java.util.Optional`, `com.webdev.dataviewer.entity.ConnectionEntity`]

