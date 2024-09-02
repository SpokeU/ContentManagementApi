# List available instructions

Returns available instructions supported by API

---
**GET** `/instructions`

```json5
[
  {
    "type": "CONTENT_UPDATE",
    "description": "Specifies the format of LLM output to update files (resources).",
    "formatRequirements": "", //Free form text, usually bullet points to 
    "examples": [
      "# Content update instructions\\n\\n**Change Summary:**\\nAdd user model into domain. Removing deprecated account functionality\\n**Project:** DataHub\\n**Component:** Backend"
    ]
  },
  {
    "type": "SCRIPT_EXECUTION",
    "description": "Specifies the format of LLM output to Execute scripts",
    "formatRequirements": "",
    "examples": [
      "# Script execution instructions\\n\\n## Description\\nSpecifies the format of LLM output to Execute scripts\\n\\n---\\nLanguage: `Python`\\nCode:\\n`{Content goes here}`"
    ]
  }
]
```

