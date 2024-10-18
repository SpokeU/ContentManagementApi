### Conventions
Below is example of perfect Open API spec. Please use it at as guidance when generating your own

#### **Reuse and externalize schemas**

Externalise all schema objects into separate files, so it can be reused using $ref

`open_api_spec.yaml`
```yaml
schema:
  $ref: "./schemas/connection.yaml"
```

`schemas/connection.yaml`

```yaml
type: object
required:
  - id
  - name
properties:
id:
  type: integer
  format: int64
name:
  type: string
```
---
#### Reuse and externalize paths

`open_api_spec.yaml`
```yaml
paths:
  /connections/{connectionId}:
    $ref: './paths/connections_{connectionId}.yaml'
```

`paths/connections_{connectionId}.yaml`
```yaml
get:
  operationId: getConnection
  parameters:
    - $ref: '../parameters/path/connectionId.yaml'
  tags:
    - connections
  summary: Get connection by id
  responses:
    200:
      description: Success
      content:
        application/json:
          schema:
            $ref: "../schemas/Connection.yaml"
```

* Important: File name should be same as path where '/' is replaced to '_'
* All paths should externalized from root:
```yaml
/connections:
  $ref: './paths/connections
```


---
#### Reuse and externalize parameters

`open_api_spec.yaml`
```yaml
paths:
  /connections/{connectionId}:
    get:
      operationId: getConnection
      parameters:
        - $ref: '../parameters/path/connectionId.yaml'
```

`/parameters/path/connectionId.yaml`
```yaml
name: connectionId
in: path
description: ID of connection to be tested
required: true
schema:
  type: integer
  format: int64
```
---
#### Externalize ENUMs

Definition  
`schemas/ConnectionTypeEnum.yaml`

```yaml
type: string
enum: [ "POSTGRES", "MONGO" ]
```

Usage

```yaml
title: Connection Request
type: object
properties:
  connectionType:
    $ref: "./schemas/ConnectionType.yaml"
```
---

Key Points:
- Main file is named - `open_api_spec.yaml`
General instructions
- All 'requestBody' and 'response' 'schema' from paths should be externalized and referenced using '$ref'
- If you find a comment in JSON example use it as instructions about object name or other properties
- Add examples to every field
- Don't add 'schemas' to 'components:' section as they are referenced when used
- The output should be in format: {filepath}/{filename} {content}.  
  For example ./schemas/ConnectionType.yaml
  {content}
- Use `src/main/resources/api` as base folder