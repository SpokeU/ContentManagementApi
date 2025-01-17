### 1. Root OpenAPI Specification

**File:** `./open_api_spec.yaml`

```yaml
openapi: 3.0.3
info:
  title: Text Processing API
  description: API for processing text generated by LLM
  version: 1.0.0

paths:
  /text-processing:
    $ref: './paths/text-processing.yaml'
```

### 2. Path for `text-processing` Endpoint

**File:** `./paths/text-processing.yaml`

```yaml
post:
  operationId: processText
  summary: Process text generated by LLM
  requestBody:
    $ref: '../requestBodies/text-processing-request.yaml'
  responses:
    200:
      $ref: '../responses/text-processing-response.yaml'
  tags:
    - text-processing
```

### 3. Request Body for `text-processing`

**File:** `./requestBodies/text-processing-request.yaml`

```yaml
description: Request to process text generated by LLM
required: true
content:
  application/json:
    schema:
      $ref: '../schemas/InstructionsProcessingRequest.yaml'
    examples:
      example-1:
        value:
          text: "Any response from LLM"
```

### 4. Response for `text-processing`

**File:** `./responses/text-processing-response.yaml`

```yaml
description: Successful response indicating the status of text processing
content:
  application/json:
    schema:
      $ref: '../schemas/InstructionsProcessingResponse.yaml'
    examples:
      example-1:
        value:
          status: "PROCESSED"
          message: "2 entities were created as a result of processing LLM response"
```

### 5. Schema for Request Body

**File:** `./schemas/TextProcessingRequest.yaml`

```yaml
type: object
required:
  - text
properties:
  text:
    type: string
    description: Raw text generated by LLM
    example: "Any response from LLM"
```

### 6. Schema for Response Body

**File:** `./schemas/TextProcessingResponse.yaml`

```yaml
type: object
required:
  - status
  - message
properties:
  status:
    $ref: './ProcessingStatusEnum.yaml'
  message:
    type: string
    description: Human-readable result of processing
    example: "2 entities were created as a result of processing LLM response"
```

### 7. ENUM for Status

**File:** `./schemas/ProcessingStatusEnum.yaml`

```yaml
type: string
enum: [ "PROCESSED", "FAILED" ]
```

---

This structure follows the conventions you outlined, externalizing the schemas, paths, request bodies, responses, and enums.