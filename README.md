## Progress

DONE: POC of file creation is done

1. Rewrite instructions for AI  (OpenApi) using example first approach
2. Refactor code - add logging + more visibility

# Content Management API

The project primarily focused so cater LLM by

1. Providing a context (aka RAG API) with its `Discovery API`
2. Allowing LLM to form instructions and passing it to `Instructions` API for execution

# Vocabulary

### Project 
### Component
### Domain

## LLM

### Subject
Все про що можна говорити і описувати. OpeanApi, Java Backned, IATA Spec etc...
Має ідентифікатор і набір властивостей. ЇЇ можна описати багатьма способами.

### Conventions
Conventions це про те як саме я хочу спілкуватись про Subject

### Response Format
Те в якому форматі я очікую відповідь від LLM


# Usage example


## Notes
Current Task - Quality of life
    - Записувати ActivityLog у гарному зручному форматі для читабельності (HTML ir Styled MD)
    - Кожен таск має бути прикріплений до проекту і таким чином передаєтсья інфа Project Component 
    - GPT 3.5 Не годен дістати список кодів тем з повідомлення користувача
        - Можна застосувати Vector Search TODO
    - додати файлові логи - //Stores all LLM logs and activity under TaskStartResponse.taskId
    - Додати тести на критичні речі search, unwrapContent,

## Startup parameters
OPENAI_API_KEY - OpenAI Api key