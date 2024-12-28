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
If this method calls other method
Value: {Fully_qualified_class}#{method_signature}  
Example: `['dev.omyshko.usermanagementsystem.service.UserService#saveUser(UserEntity)', 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)']`

### input_argument
Input arguments for this method. Might be an array because if contains generics
Value: List of fully qualified argument names  
Example: [`dev.omyshko.usermanagementsystem.entity.UserEntity`]

### return_type
Fully qualified return type. Might be an array because if return type contains generics
Example: [`org.springframework.http.ResponseEntity`, `dev.omyshko.usermanagementsystem.api.model.UserApiModel`]

