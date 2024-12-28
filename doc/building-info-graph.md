## Overview
Two possible solutions to scan a project. 
1. Start with a random file and recurvisely go though all dependencies
2. Scan all files and only categorize it and figure out content_type. Then start building dependencies iterating though files with 'Functionality' category.

Below is the solution for option 2

## Step 1: Code Scanning and Block Identification
Goal: Scan the codebase and break down files into logical blocks.

Solution: Block can be a whole while as it is, or it can be split to class definitions, methods etc.
But the overall abstraction is that there are blocks of information and its relations. Based on content and relations, we will provide RAG for LLM request.
_Note: Challenge will be to narrow context_

File: Scan file one by one and build nodes without connections

## Step 2: Classification of Blocks
Goal: Classify blocks into categories based on their functionality.  


**File**:
At this stage we are getting only category + content_type. Summary can be provided only when all dependencies are scanned 

```json5
{
  //source
  "file_path" : "",

  //Based on language get the blocks/classifiers from KB and provide to the next prompt to extract it!!!!!!!!!!!!!!!
  "language": "java",

  //Alternative field if you want to categorize a file by some custom criteria. 
  "tag": "Functionality",
  
  //Technical role of this file API, MODEL, SERVICE,  Html Template, SQL_Migration, Angular Component. As we are scanning file there might be multiple roles for this file 
  "tech_role": ["API"],

  //Basically how to read the content inside Java, HTML, TypeScript
  "type": "java",
  
  //Raw content of a block
  "content": "public List<User> getUsers() {\n    // Logic to fetch users\n}",
  
  //High level explanation of this content. LLM based
  "summary": "Not defined on this stage",
}
```

## Step 3: Identify Dependencies
Goal: Track dependencies between blocks.

**File:**  
As all files are scanned now and have tag + category  

**Block identification**
To build dependencies I need to clearly identify calling part of dependency.  
Because we are parsing code if one method is calling some other method - this other method call have to be clear based on import method signature etc.
Otherwise compiler/interpreter would not know what to call either

Package: com.example   
Class: UserService  
Method Signature: User getAllUsers();  

Thus when analyzing current file we can try to build dependencies to non existing nodes yet. 
The requirement is that those nodes should have same identification as    
some field which identifies a block 

**The dependency generated while analyzing current file has to contain everything to be able to find it later when analyzing other file which it depends on**

## Step 4: Summarize 
Summarization can be done only when you are able to track + describe all underlying dependencies. Without knowing what serviceA and serviceB do you cannot describe what controller which calls these two services is doing. 

## Step 5: Storing Blocks and Dependencies in Neo4j
Goal: Use Neo4j to store and manage blocks and their dependencies in a graph database.

## Step 6: Generate Documentation

### Input
File path to analyze
`D:\Development\Projects\ContentManagementApi\UserController.java`

### Output
List of blocks along with all the dependencies 

```json5
[
  {
    //Identifier of a code block. Used to 1.Search it in file  2.Reference block between graph nodes
    "id": "UserController#getUsers",
    "filename": "UserController.java",
    "path": "/src/main/java/com/project/controller/UserController.java",
    //Identifies type of information (SQL, HTML, java etc.)
    "language": "java",
    //Functionality, Service, Model etc. 
    "category": "Functionality",
    // Identifies blocks of code that are triggered from outside the system (e.g., via an API call, a job scheduler, or a message listener). This is needed to further classify information and be able to select all API endpoints etc.
    "trigger_type": "API Endpoint",
    //Raw content of a block
    "content": "public List<User> getUsers() {\n    // Logic to fetch users\n}",
    //High level explanation of this content
    "summary": "Fetches a list of users from the database",
    "dependencies": [
      {
        "block": "UserService#getUsers",
        //Type of dependency. Callee(if this a calls another), Caller, Input Argument, Output, Dependency(Import statement, A type used inside method)
        "type": "callee"
      },
      {
        "block": "UserModel",
        "type": "output"
      }
    ]
  }
]
```