# List available projects

Returns all projects

---
**GET** `/projects`

```json5
  {
  "name": "DataHub",
  "description": "Centralized platform for managing database connections, sharing queries, executing them, and presenting results in a user-friendly manner.",
  "components": [
    {
      "name": "DataHub Backend",
      "description": "Java Backend service",
      "type": "BACKEND",
      "location": "https://github.com/SpokeU/DataHub.git"
    },
    {
      "name": "DataHub Frontend",
      "description": "Angular frontend application",
      "type": "UI",
      "location": "https://github.com/SpokeU/dataviewer-ui.git"
    }
  ]
}
```

