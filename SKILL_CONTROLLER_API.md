## SoftTrainer API: Skill Management Endpoints

This document outlines the API endpoints for managing skills within the SoftTrainer platform.

**Base Path:** `/api/skills`

### 1. Create a New Skill

Creates a new skill with associated metadata and optional file materials.

*   **Endpoint:** `POST /api/skills`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `201 Created`

**Request Body (`NewSkillPayload`)**

```json
{
  "name": "string (required)",
  "description": "string (required)",
  "type": "string (required, enum)",
  "behavior": "string (required, enum)",
  "simulationCount": "integer (optional)",
  "materials": [
    {
      "fileName": "string",
      "tag": "string",
      "fileContent": "string (base64-encoded)"
    }
  ]
}
```

**Field Descriptions:**

*   `name`: The name of the skill.
*   `description`: A detailed description of the skill.
*   `type`: The category of the skill. Must be one of `DEVELOPMENT`, `ASSESSMENT`, or `MIX`.
*   `behavior`: The skill's behavior type. Must be one of `STATIC` or `DYNAMIC`.
*   `simulationCount`: The number of simulations associated with this skill.
*   `materials`: An optional array of files to be uploaded.
    *   `fileName`: The original name of the uploaded file.
    *   `tag`: A user-defined tag to categorize the material.
    *   `fileContent`: The file content, encoded in Base64.

**Successful Response Body (`SkillDetailDto`)**

Returns the full skill object that was created.

```json
{
  "id": 1,
  "name": "Advanced Sales Techniques",
  "description": "A course on modern sales methodologies.",
  "type": "DEVELOPMENT",
  "behavior": "DYNAMIC",
  "simulationCount": 5,
  "simulations": [],
  "materials": [
    {
      "fileName": "course_outline.pdf",
      "tag": "syllabus"
    }
  ]
}
```

### 2. Get All Skills

Retrieves a summary list of all available skills.

*   **Endpoint:** `GET /api/skills`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`

**Successful Response Body (`List<SkillSummaryDto>`)**

Returns a list of skill summaries.

```json
[
  {
    "id": 1,
    "name": "Advanced Sales Techniques",
    "description": "A course on modern sales methodologies."
  },
  {
    "id": 2,
    "name": "Customer Support Essentials",
    "description": "Training for providing excellent customer service."
  }
]
```

### 3. Get a Single Skill

Retrieves the detailed information for a single skill by its ID.

*   **Endpoint:** `GET /api/skills/{id}`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`
*   **Error Response:**
    *   `404 Not Found`: If a skill with the specified `id` does not exist.

**Successful Response Body (`SkillDetailDto`)**

Returns the full skill object. The `simulations` field is included for future use but is currently empty.

```json
{
  "id": 1,
  "name": "Advanced Sales Techniques",
  "description": "A course on modern sales methodologies.",
  "type": "DEVELOPMENT",
  "behavior": "DYNAMIC",
  "simulationCount": 5,
  "simulations": [],
  "materials": [
    {
      "fileName": "course_outline.pdf",
      "tag": "syllabus"
    }
  ]
}
```

### 4. Update Skill Visibility

Toggles the visibility of a skill, allowing it to be hidden or shown.

*   **Endpoint:** `PATCH /api/skills/{id}/visibility`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `204 No Content`
*   **Error Response:**
    *   `404 Not Found`: If a skill with the specified `id` does not exist.

**Request Body**

```json
{
  "isHidden": true
}
```

**Field Descriptions:**

*   `isHidden`: Set to `true` to hide the skill, `false` to make it visible.

### 5. Delete a Skill

Permanently deletes a skill, provided it is not protected.

*   **Endpoint:** `DELETE /api/skills/{id}`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `204 No Content`
*   **Error Responses:**
    *   `403 Forbidden`: If the skill is protected (`isProtected` flag is true) and cannot be deleted.
    *   `404 Not Found`: If a skill with the specified `id` does not exist. 