## SoftTrainer API: Skill Management Endpoints (V2)

This document outlines the API endpoints for managing skills within the SoftTrainer platform, updated to reflect the standardized API response wrapper.

**Base Path:** `/api/skills`

### Standard Response Wrapper

All responses from these endpoints are wrapped in a standard JSON object:

```json
{
  "success": "boolean",
  "message": "string (optional)",
  "data": "object | array (contains the actual response payload)"
}
```

---

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
  "type": "string (required, enum: DEVELOPMENT | ASSESSMENT | MIX)",
  "behavior": "string (required, enum: STATIC | DYNAMIC)",
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

**Successful Response Body (`ApiResponseDto<SkillDetailDto>`)**

```json
{
  "success": true,
  "message": "Skill created successfully",
  "data": {
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
}
```

### 2. Get All Skills

Retrieves a summary list of all available skills.

*   **Endpoint:** `GET /api/skills`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`

**Successful Response Body (`ApiResponseDto<List<SkillSummaryDto>>`)**

```json
{
  "success": true,
  "data": [
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
}
```

### 3. Get a Single Skill

Retrieves the detailed information for a single skill by its ID.

*   **Endpoint:** `GET /api/skills/{id}`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`
*   **Error Response:** `404 Not Found`

**Successful Response Body (`ApiResponseDto<SkillDetailDto>`)**

```json
{
  "success": true,
  "data": {
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
}
```

### 4. Update Skill Visibility

Toggles the visibility of a skill.

*   **Endpoint:** `PATCH /api/skills/{id}/visibility`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`
*   **Error Response:** `404 Not Found`

**Request Body**

```json
{
  "isHidden": true
}
```

**Successful Response Body (`ApiResponseDto<Void>`)**

```json
{
  "success": true,
  "message": "Skill visibility updated successfully"
}
```

### 5. Delete a Skill

Permanently deletes a skill.

*   **Endpoint:** `DELETE /api/skills/{id}`
*   **Permissions:** `Authenticated User`
*   **Success Response:** `200 OK`
*   **Error Responses:**
    *   `403 Forbidden`: If the skill is protected.
    *   `404 Not Found`: If the skill does not exist.

**Successful Response Body (`ApiResponseDto<Void>`)**

```json
{
    "success": true,
    "message": "Skill deleted successfully"
}
``` 