## Technology overview
Language: Java 17  
Technologies: Spring Boot, JPA, Flyway
Build tools: Maven
Build process: 
- `openapi-generator-maven-plugin` Generates controllers based on open_api_spec.yaml into ${project.basedir}/src/main/resources/api/open_api_spec.yaml.
  - Generates API controllers into the dev.example.api package and models into the dev.example.api.model package
  - Uses delegatePattern option 
- Highlight other possible build specifics like code generation, test execution, commands how to run each and other

## Project Functionalities

### Overview
List of functionalities which can be triggered (Jobs, Api, etc.) in this project. Without functionality a project doesn't do anything thus is useless.
Based on build file and technologies find out what type of project is this what entry points are there (project functionalities).

### REST API 

#### [create-connection](api/create-connection.md)

### Messaging
* Contract details
* Description of API
* Example RQ/RS


### Scheduled Jobs
* Cron expression
* Possible configuration
* Overview and description of functionality
* Example of data transformation Input/Output (Job selects something (Input), Job does something to that data (Output))


### Strategies to find functionality
* Option 1 - Scan project build file + structure and then analyze only files which seems to have some functionality that can be triggered (UserController, DataMessageConsumer etc.)
  * By having project structure figure out where are the possible entry points. this relies on meaningful names of a packages (.controller) or files (UserController) but allows to not scan every individual file_
  * Pros: 
    * No need to scan entire project and waste LLM resources on model and abstraction files. if there is a requirement to make a change in specific place. This makes it more like humans discover a project when they join on new one. Only discover what is needed for a task to not waste resources. And with AI specifics this might be actual as well because AI is slow and costly  
  * Cons:
    * Easy to miss something as if packages or files are not meaningful then no luck 
* Option 2 - Scan each file (like in genbee) and assign each file classification (Entrypoint, Service, DB etc.)
  * Pros: Nothing is missed and no guesses are made. More consistent approach. Also in any case we would need to scan every file to figure out its purpose to describe package structure and add documentation
  * Cons: Need to scan EVERY file which might be too much for large projects.

## Project folder structure

### Sources
- `/scr`
  - `/main`
    - `/java`
      - `/dev/omyshko/contentmanagement`
        - `/api` Code related to HTTP Api
            - `/endpoint`: Contains classes that handle HTTP requests. Controllers or delegate classes in case of OpenApi code generation
          - `/model`: Defines Data Transfer Objects (DTOs) that are used to structure the data sent to or received from the API endpoints. These models are tailored for external communication.
    - `/resources`
      - `/api`: Everything related to API definition (mostly api spec)
        - `open_api_spec.yaml` - Open api spec for current service
        - `/paths` Paths that are externalized from main open_api_spec file
        - `/schemas` Schema objects that are externalized from main open_api_spec file
  - `/test` Folder containing tests
  - `pom.xml` - Maven build file


## Workflows
Process of introducing changes to project

### Adding new endpoint

1. Modify Open API spec _//TODO define location_
2. Run generation plugin
3. Implement endpoint