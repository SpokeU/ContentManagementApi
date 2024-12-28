## Progress

DONE: POC of file creation is done

1. Rewrite instructions for AI  (OpenApi) using example first approach
2. Refactor code - add logging + more visibility

# Content Management API

The project primarily focused to power LLM by

1. Providing a context (aka RAG API)
2. Allowing LLM to form instructions and passing it to `Instructions` API for execution

# Vocabulary

## Project
Is designed to hold  all related info under some name.  
For example - This project "Content management API" has its code repository, technical documentation, business plan documentation or any other source information that is related to this effort.  
Thus when you are referring to "Project A" you can identify specific effort and be able to view and access related resources  

## Component
Is a part of a [Project](#project).    
Deigned to hold reference to specific part of a project (backend-code, frontend-code, documentation etc.) and provide R/W access to that information 

## Domain
Not yet implemented.   
Idea is to be able to pinpoint specific parts of information by specifying the domain which you are interested.  
For example: You have Ecommerce system and you want to get all related info about Pricing and how it works. Furthermore by specifying component + domain you can select information more specifically.  
This might help to search and navigate by information to RAG required pieces of knowledge for LLM

## Knowledge base

Is a concept of structured general knowledge storage about any kind of topics.
Primarily designed to be used by LLM to search for "WHAT IS {topic}" and "HOW TO {do stuff}" kind of information  
Usually contains examples, conventions and information about specific technologies and topics 

### Topic
Все про що можна говорити і описувати. OpeanApi, Java Backned, IATA Spec etc...
Має ідентифікатор і набір інформації.

### Response Format
The format which is expected from LLM as an output. 
Usually contains short explanation of its usage and examples

## Project documentation

Extensive documentation of a project that will be used by LLM to search for required functionality and code based on user input (or task definition).  
Think of it as project high level description having reference to all [functionalities](#project-functionalities) and project structure. 
Which can be used as entry point to search for required code based on user input. 
After entering code domain to navigate by code a dependency graph + generated javadocs are used.  
Project documentation is also minimal spec - Example here:  [Project info example](project-info/project-info.md)

### Project functionalities
A detailed overview of all executable functionalities within the project, categorized by their trigger mechanisms. (REST APIs, Scheduled Jobs, Messaging etc.)

## File documentation 
Can be implemented as extensive documentation for each method (Like javadoc).   
The Pros of this approach is that it can be used just for generating javadocs for existing project and is human readable which eases up the understanding of a solution. 
Also by having some tags and references in this javadoc - native gitsearch or other built in functionality can be used compared to having own DB which references files and contains its description.

# Points to follow during development
* Any creation of content should be according to some conventions. Thus every prompt for creation should have example of format. This can be done by feeding existing similar content or example of conventions

## Notes
Current Task - Quality of life- Додати Нове поле до Існуючого API
  - Таска має оперувати прикладами а не кодом всередині. Тобто як для створення ми давали ТІЛЬКИ приклади а ЛЛМ вже вирішував куди то все пхати так само для апдейту ми апдейтимо приклади 
  - Таска має звучати як: Update codebase according to http://linktoexample.md
    - LLM сам має визначити різницю від існуючого до бажаного
  - PROCESS: 
    - Почитати таску, уявити кінцевий резултат і сформувати завдання відповідно до технологій() - 1 
    - Зайти в код і глянути шо з того вже є всередині - 2 code/search 
      - Всього не потрібно для контексту. Для цого у нас буде project-info.md як індекс файл проекту для пошуку потрібного коду
        - Під час аналізу потрібно буде також подивитись Find Usages 
      - String code = code/search(task)
    - Скласти план змін 1-2
    
    
    
- Можливість Review PR і відправляння коментів в LLM
- GPT 3.5 Не годен дістати список кодів тем з повідомлення користувача
  - Можна застосувати Vector Search TODO
- Додати тести на критичні речі search, unwrapContent,


Neo4j pass: 2G-iSlY4rlfxJf3zMI6fb1bkNgKtTtPeMZ2yRGprX3Q

## Startup parameters
OPENAI_API_KEY - OpenAI Api key

