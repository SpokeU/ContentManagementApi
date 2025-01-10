## Overview
Two possible solutions to scan a project. 
1. Start with a random file and recurvisely go though all dependencies
2. Scan all files and only categorize it and figure out content_type. Then start building dependencies iterating though files with 'Functionality' category.

Below is the solution for option 2

## Step 1: Code Scanning and Block Identification - Стректурний Аналіз
Goal: Scan the codebase and break down files into logical blocks based on file extension

Solution: Block can be a whole while as it is, or it can be split to class definitions, methods etc.
But the overall abstraction is that there are blocks of information and its relations. Based on content and relations, we will provide RAG for LLM request.
_Note: Challenge will be to narrow context_

File: Scan file one by one and build nodes without connections

```markdown
-block --parsed
--childBlock - nonParsed
--childBlock
--childBlock
```

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

# Issues

# Important notes

## Importance of array description
Example:  
`@Description("Any inner records/classes/interfaces should be captured here") List<JavaInnerClass> declaredInnerClasses`

Without description on array it Gemini would not capture inner records although inner item 
`@Description("A declaration of Inner 'class', 'interface', 'record'.")  
public record JavaInnerClass`   
has a description to capture records.

## Importance of proper field naming !!!
Field name is much more important than description. In fact its so important I'm going to write it twice.  
Also if you have two same field names in different entities (For example `id` of a user and address and those have different rules of forming)  it might mess those meaning the format of one `id` will mess up completely other field

Examples:
`declaredMethods` - Selects all declared methods including inner class methods although in description I specified that only topLevel method should be extracted
`declaredTopLevelMethods` - And it selected only topLevel method without inner class methods which is what I wanted

## Putting important instructions to general System message
It seems Gemini doesn't give a damn about json_schema field description. But by putting instructions into a system or user message it starts to react. For example with innerClasses

## Avoid nesting
Try as flat structure of your JSON schema as possible.
For example having below structure produces much better results than

```json5
{
  "classBlocks": [
    {
      "id": "dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider",
      "name": "KnowledgeBaseInformationProvider",
      "declaredMethods": [
        {
          "fullyQualifiedSignature": "dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider.extractTopicCode(java.nio.file.Path)",
          "javaDocStartingLine": "188",
          "fromLine": "188",
          "toLine": "202"
        }
      ],
      "importBlock": {
        "importId": "dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider_imports",
        "fromLine": "3",
        "toLine": "23}, "
      }
    }
  ]
}
```

Then this

```json5
{
  "classBlocks": [
    {
      "id": "dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider",
      "name": "KnowledgeBaseInformationProvider",
      "dependencies": {
        "declaredMethods": [
          {
            "fullyQualifiedSignature": "java.lang.String dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider.getTableOfContent()",
            "javaDocStartingLine": "69",
            "fromLine": "70",
            "toLine": "88"
          }
        ],
        "importBlock": {
          "importId": "dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseInformationProvider_imports",
          "fromLine": "3",
          "toLine": "23}}, "
        }
      }
    }
  ]
}

```

You can see it added a return type to fullyQualifiedSignature already while it shouldn't. And it doesn't do it when structure is more flat

## Importance of formatting before processing
LLM seems to perform much worse if a code is formatted in specific way. 
For example if a throws in method declaration is declared in separate line it cannot figure out proper start & end of the method block.

This leads to a preprocessing phase with formatting a code by specific formatting rules for best LLM results which are yet to find for each specific language and case

## Different Models
Different models produce different result. And where one may struggle the other won't have those issue but will have other.

### Gemini
#### Pros:
More Consistent output. Even on larger files I get same output by calling it multiple times

#### Cons:
Completely ignores description of a field when using json_schema. This way any important instructions should be separate in PROMPT
`
"responseMimeType": "application/json",
"responseSchema": {
`

### OpenAI
#### Cons:
Not consistent
Skips closing brace of a method to capture