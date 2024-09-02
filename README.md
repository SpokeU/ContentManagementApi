# Content Management API

The project primarily focused so cater LLM by

1. Providing a context (aka RAG API) with its `Discovery API`
2. Allowing LLM to form instructions and passing it to `Instructions` API for execution

## Progress

DONE: POC of file creation is done  

IN-PROGRESS: Add instructions to discovery API so LLM can auto discover any new type instruction
- Add new operation - GET /instructions that will return all available instructions
- Add new operation instructions/{type}/process - as LLM wil know all supported types
- Change /instructions-processing endpoint to /instructions/process - Just keep its because I want

**!!! Do above change purely by using AI with openAPI tools + with new file update instructions and process it with existing API. This wiil help to run it on real use case**


# Vocabulary

### Project 
### Component
### Domain