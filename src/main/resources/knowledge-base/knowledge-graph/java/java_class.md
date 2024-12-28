## Overview
A block which represents java Class. Only classes that are declared as 'public class {class_name}' in provided file should be extracted. If there is only one declared class in a file then return array with single item 

### name
Should always be a string 'java_class'

### id
Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`

## Fields

### package
Example - `dev.omyshko.contentmanagement.core.service`  

### class_name
Example - GitContentManager  

### shortened_content
Important! It should not include all class content.
**All methods should be replaced with `...`**
Should include only package declaration, annotations,class declaration, variables and static blocks.

Example -
```java
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

## Classifiers
This is in addition to main block name to be able to navigate graph using different concepts (like spring or other framework)

### controller
### spring_bean
### entity
### repository

## Dependencies

### declares_method
If this class declares a method then fully qualified signature has to used. List all declared methods here 
For example: 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(Long, UserEntity)'

### inherits
All interfaces or classes that this class extends or implements. 
Value should be fully qualified class name
For example: 'dev.omyshko.contentmanagement.instructions.changelog.ChangeLogParser'
