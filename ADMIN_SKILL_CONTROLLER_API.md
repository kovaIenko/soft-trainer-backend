# SoftTrainer Admin API: Skill Management Endpoints

This document outlines the administrative API endpoints for managing skills within the SoftTrainer platform. These endpoints are intended for administrative clients and provide full CRUD functionality.

**Base Path:** `/api/admin/skills`

---

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

*   **Endpoint:** `POST /api/admin/skills`
*   **Permissions:** `Authenticated Admin`
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

### 2. Get All Skills (Admin)

Retrieves a complete summary list of all skills in the system.

*   **Endpoint:** `GET /api/admin/skills`
*   **Permissions:** `Authenticated Admin`
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

### 3. Get a Single Skill (Admin)

Retrieves the detailed information for a single skill by its ID.

*   **Endpoint:** `GET /api/admin/skills/{id}`
*   **Permissions:** `Authenticated Admin`
*   **Success Response:** `200 OK`
*   **Error Response:** `404 Not Found` if the skill does not exist.

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

*   **Endpoint:** `PATCH /api/admin/skills/{id}/visibility`
*   **Permissions:** `Authenticated Admin`
*   **Success Response:** `200 OK`
*   **Error Response:** `404 Not Found` if the skill does not exist.

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

### 5. Archive a Skill (Soft Delete)

Archives a skill by hiding it from admin view. The skill is not permanently deleted and can be restored later.

*   **Endpoint:** `DELETE /api/admin/skills/{id}`
*   **Permissions:** `Authenticated Admin`
*   **Success Response:** `200 OK`
*   **Error Responses:**
    *   `403 Forbidden`: If the skill is protected.
    *   `404 Not Found`: If the skill does not exist.

**Successful Response Body (`ApiResponseDto<Void>`)**

```json
{
    "success": true,
    "message": "Skill archived successfully"
}
```

### 6. Get Archived Skills

Retrieves a list of all archived (soft deleted) skills.

*   **Endpoint:** `GET /api/admin/skills/archived`
*   **Permissions:** `Authenticated Admin`
*   **Success Response:** `200 OK`

**Successful Response Body (`ApiResponseDto<List<SkillSummaryDto>>`)**

```json
{
  "success": true,
  "message": "Archived skills retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Archived Skill Name",
      "description": "Description of the archived skill."
    }
  ]
}
```

### 7. Restore a Skill

Restores an archived skill by making it visible in admin view again.

*   **Endpoint:** `POST /api/admin/skills/{id}/restore`
*   **Permissions:** `Authenticated Admin`
*   **Success Response:** `200 OK`
*   **Error Response:** `404 Not Found` if the skill does not exist.

**Successful Response Body (`ApiResponseDto<Void>`)**

```json
{
    "success": true,
    "message": "Skill restored successfully"
}
``` 