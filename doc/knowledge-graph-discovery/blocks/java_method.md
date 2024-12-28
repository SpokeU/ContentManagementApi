## Overview
Represents java methods

**Name**: Method  
**Id**: Fully qualified method signature name. For example `dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity, Long)`

## Fields

### Return type 
Example - `dev.omyshko.usermanagementsystem.api.model.UserApiModel`

### Method signature
Example - `toApiModel(UserEntity, Long)`

### Content
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
This is in addition to main block name to be able to navigate graph using different concepts (like spring or other framework)

### EntryPoint
Any functionality that can be triggered. For example APIs, Jobs, Messaging can be treated as EntryPoint.

## Dependencies

#### Dependencies
Type: CALLS  
Description: If this method calls other method  
Value: {Fully_qualified_class}#{method_signature}  
Example: `['dev.omyshko.usermanagementsystem.service.UserService#saveUser(UserEntity)', 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(UserEntity)']`  

Type: INPUT_ARGUMENT
Description: Input arguments for this method
Value: List of fully qualified argument names  
Example: [`dev.omyshko.usermanagementsystem.entity.UserEntity`]

Type: RETURN_TYPE  
Value: Fully qualified return type. Might be an array because of generics
Example: [`org.springframework.http.ResponseEntity`, `dev.omyshko.usermanagementsystem.api.model.UserApiModel`]
