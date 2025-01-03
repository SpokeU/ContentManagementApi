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

## Classifiers
Is used for tagging information by some concepts.

### entry_point
Any functionality that can be triggered by user. All SpringController handler methods should be classified as entry_point, Jobs, Messaging can be treated as entry_point.

## Dependencies

### calls
If this method calls other method then extract below values for single called method. Each called method should be in separate array item

#### call_statement
A call statement content

#### fully_qualified_signature
Fully qualified signature of called method
Example: `dev.omyshko.usermanagementsystem.service.UserService#saveUser(UserEntity)`

#### fully_qualified_input_arguments
type:array
Fully qualified class name of input parameters for called method
Example: `['dev.omyshko.usermanagementsystem.entity.UserEntity', 'dev.omyshko.usermanagementsystem.api.model.UserApiModel']`

#### fully_qualified_return_type
type:array
Fully qualified return type. If return type using generics then array should contain each type separately
Example: `dev.omyshko.usermanagementsystem.entity.UserEntity`

### input_argument
Input arguments for this method. Might be an array because if contains generics
Value: List of fully qualified argument names  
Example: [`dev.omyshko.usermanagementsystem.entity.UserEntity`]

### return_types
type:array
Fully qualified return types of each type you can extract. If return type is using generics then array should contain each type separately
! Example: `java.util.Optional<com.webdev.dataviewer.entity.ConnectionEntity>` will be extracted to [`java.util.Optional`, `com.webdev.dataviewer.entity.ConnectionEntity`]

