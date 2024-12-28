## Overview
Represents idea of java class

**Name**: Class  
**Id**: Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`

## Fields

### Package 
Example - `dev.omyshko.contentmanagement.core.service`  

### Class name
Example - GitContentManager  

### Content
Should include package declaration, annotations,class declaration, variables and static blocks. Imports, Methods and else should be replaces with Imports... or Methods...
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
    
    //[METHODS BLOCK]

}
```

## Classifiers
This is in addition to main block name to be able to navigate graph using different concepts (like spring or other framework)

### Controller
### SpringBean
### Entity
### Repository

## Dependencies

Declares which dependency types this class have to more effectively build knowledge graph. 
Each dependency has a type and id.   
**type** will bee used as a label when creating relation
**id** will be used to determine which node to connect to after processing of all files. Refer to concept of `id` generation as its very important

### Declares
When class declares a method then fully qualified signature has to used 
For example: 'dev.omyshko.usermanagementsystem.service.UserController#toApiModel(Long, UserEntity)'
