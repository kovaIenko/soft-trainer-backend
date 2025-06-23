# Skills Management API Documentation

## Overview

The Skills Management API provides endpoints for creating, retrieving, updating, and managing skills within the SoftTrainer platform. All endpoints require authentication and implement organization-based access control.

**Base URL**: `/api/admin/skills`

## Authentication

All endpoints require a valid JWT Bearer token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Access Control

- **ROLE_ADMIN**: Can only access skills associated with their organization
- **ROLE_OWNER**: Can access all skills across all organizations (for viewing), but creates skills for their own organization

## Endpoints

### 1. Get All Skills
Retrieves a list of all active (non-archived) skills accessible to the user.

**Endpoint**: `GET /api/admin/skills`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Response
```json
{
  "success": true,
  "data": [
    {
      "id": 1452,
      "name": "Customer Service Excellence",
      "description": "Advanced training for customer service representatives focusing on communication and problem-solving skills"
    },
    {
      "id": 752,
      "name": "<Onboarding> English",
      "description": "onboarding eng"
    }
  ]
}
```

#### Example Request
```bash
curl -X GET "https://localhost:8443/api/admin/skills" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -k
```

---

### 2. Get Skill by ID
Retrieves detailed information about a specific skill, including materials metadata.

**Endpoint**: `GET /api/admin/skills/{id}`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Path Parameters
- `id` (Long): The unique identifier of the skill

#### Response
```json
{
  "success": true,
  "data": {
    "id": 1452,
    "name": "Customer Service Excellence",
    "description": "Advanced training for customer service representatives focusing on communication and problem-solving skills",
    "type": "DEVELOPMENT",
    "behavior": "DYNAMIC",
    "simulationCount": 3,
    "simulations": [
      {
        "id": 2001,
        "name": "Customer Complaint Handling",
        "avatar": "https://example.com/avatar1.png",
        "complexity": "MEDIUM",
        "created_at": "2024-06-23T10:30:00",
        "is_open": true,
        "hearts": 100.0,
        "order": 1
      },
      {
        "id": 2002,
        "name": "Upselling Techniques",
        "avatar": "https://example.com/avatar2.png",
        "complexity": "HARD",
        "created_at": "2024-06-23T11:15:00",
        "is_open": true,
        "hearts": 120.0,
        "order": 2
      }
    ],
    "materials": [
      {
        "fileName": "customer-service-guide.pdf",
        "tag": "manual"
      }
    ]
  }
}
```

#### Error Responses
- **403 Forbidden**: User doesn't have access to this skill
- **404 Not Found**: Skill doesn't exist

#### Example Request
```bash
curl -X GET "https://localhost:8443/api/admin/skills/1452" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -k
```

---

### 3. Create Skill
Creates a new skill with optional file materials.

**Endpoint**: `POST /api/admin/skills`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Request Body
```json
{
  "name": "Test API Skill",
  "description": "A test skill created via API to verify functionality",
  "type": "ASSESSMENT",
  "behavior": "STATIC",
  "simulationCount": 2,
  "materials": [
    {
      "fileName": "test-document.pdf",
      "tag": "reference",
      "fileContent": "VGVzdCBmaWxlIGNvbnRlbnQ="
    }
  ]
}
```

#### Field Descriptions
- `name` (String, required): The name of the skill
- `description` (String, required): A detailed description of the skill
- `type` (Enum, required): Skill type - `DEVELOPMENT`, `ASSESSMENT`, or `MIX`
- `behavior` (Enum, required): Skill behavior - `STATIC` or `DYNAMIC`
- `simulationCount` (Integer, required): Number of simulations for this skill
- `materials` (Array, optional): List of learning materials
  - `fileName` (String): Name of the file
  - `tag` (String): Category/tag for the material
  - `fileContent` (String): Base64-encoded file content

#### Response
```json
{
  "success": true,
  "message": "Skill created successfully",
  "data": {
    "id": 1502,
    "name": "Test API Skill",
    "description": "A test skill created via API to verify functionality",
    "type": "ASSESSMENT",
    "behavior": "STATIC",
    "simulationCount": 2,
    "simulations": [],
    "materials": [
      {
        "fileName": "test-document.pdf",
        "tag": "reference"
      }
    ]
  }
}
```

#### Example Request
```bash
curl -X POST "https://localhost:8443/api/admin/skills" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test API Skill",
    "description": "A test skill created via API to verify functionality",
    "type": "ASSESSMENT",
    "behavior": "STATIC",
    "simulationCount": 2,
    "materials": [
      {
        "fileName": "test-document.pdf",
        "tag": "reference",
        "fileContent": "VGVzdCBmaWxlIGNvbnRlbnQ="
      }
    ]
  }' \
  -k
```

---

### 4. Update Skill Visibility
Updates the visibility status of a skill (hidden/visible).

**Endpoint**: `PATCH /api/admin/skills/{id}/visibility`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Path Parameters
- `id` (Long): The unique identifier of the skill

#### Request Body
```json
{
  "isHidden": true
}
```

#### Response
```json
{
  "success": true,
  "message": "Skill visibility updated successfully"
}
```

#### Example Request
```bash
curl -X PATCH "https://localhost:8443/api/admin/skills/1502/visibility" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"isHidden": true}' \
  -k
```

---

### 5. Archive Skill (Soft Delete)
Archives a skill by marking it as admin-hidden. The skill data is preserved but hidden from normal views.

**Endpoint**: `DELETE /api/admin/skills/{id}`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Path Parameters
- `id` (Long): The unique identifier of the skill

#### Response
```json
{
  "success": true,
  "message": "Skill archived successfully"
}
```

#### Error Responses
- **403 Forbidden**: 
  - User doesn't have access to this skill
  - Skill is protected and cannot be deleted
- **404 Not Found**: Skill doesn't exist

#### Example Request
```bash
curl -X DELETE "https://localhost:8443/api/admin/skills/1502" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -k
```

---

### 6. Get Archived Skills
Retrieves a list of all archived (soft-deleted) skills accessible to the user.

**Endpoint**: `GET /api/admin/skills/archived`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Response
```json
{
  "success": true,
  "message": "Archived skills retrieved successfully",
  "data": [
    {
      "id": 1502,
      "name": "Test API Skill",
      "description": "A test skill created via API to verify functionality"
    }
  ]
}
```

#### Example Request
```bash
curl -X GET "https://localhost:8443/api/admin/skills/archived" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -k
```

---

### 7. Restore Archived Skill
Restores an archived skill back to active status.

**Endpoint**: `POST /api/admin/skills/{id}/restore`  
**Authorization**: `ROLE_ADMIN` or `ROLE_OWNER`

#### Path Parameters
- `id` (Long): The unique identifier of the archived skill

#### Response
```json
{
  "success": true,
  "message": "Skill restored successfully"
}
```

#### Error Responses
- **403 Forbidden**: User doesn't have access to this skill
- **404 Not Found**: Skill doesn't exist

#### Example Request
```bash
curl -X POST "https://localhost:8443/api/admin/skills/1502/restore" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -k
```

---

## Data Models

### SkillSummaryDto
```json
{
  "id": 1452,
  "name": "Customer Service Excellence",
  "description": "Advanced training for customer service representatives"
}
```

### SkillDetailDto
```json
{
  "id": 1452,
  "name": "Customer Service Excellence",
  "description": "Advanced training for customer service representatives focusing on communication and problem-solving skills",
  "type": "DEVELOPMENT",
  "behavior": "DYNAMIC",
  "simulationCount": 3,
  "simulations": [
    {
      "id": 2001,
      "name": "Customer Complaint Handling",
      "avatar": "https://example.com/avatar1.png",
      "complexity": "MEDIUM",
      "created_at": "2024-06-23T10:30:00",
      "is_open": true,
      "hearts": 100.0,
      "order": 1
    }
  ],
  "materials": [
    {
      "fileName": "customer-service-guide.pdf",
      "tag": "manual"
    }
  ]
}
```

### SimulationSummaryDto
```json
{
  "id": 2001,
  "name": "Customer Complaint Handling",
  "avatar": "https://example.com/avatar1.png",
  "complexity": "MEDIUM",
  "created_at": "2024-06-23T10:30:00",
  "is_open": true,
  "hearts": 100.0,
  "order": 1
}
```

### NewSkillPayload
```json
{
  "name": "Skill Name",
  "description": "Skill description",
  "type": "DEVELOPMENT|ASSESSMENT|MIX",
  "behavior": "STATIC|DYNAMIC",
  "simulationCount": 3,
  "materials": [
    {
      "fileName": "document.pdf",
      "tag": "reference",
      "fileContent": "base64-encoded-content"
    }
  ]
}
```

### UpdateSkillVisibilityDto
```json
{
  "isHidden": true
}
```

## Error Handling

All endpoints return consistent error responses:

### Validation Errors (400 Bad Request)
```json
{
  "timestamp": 1750692086866,
  "status": 400,
  "error": "Bad Request",
  "path": "/api/admin/skills"
}
```

### Permission Errors (403 Forbidden)
```json
{
  "success": false,
  "message": "You don't have access to this skill"
}
```

### Not Found Errors (404 Not Found)
```json
{
  "success": false,
  "message": "Skill not found"
}
```

## Enums

### SkillType
- `DEVELOPMENT`: Skills focused on learning and development
- `ASSESSMENT`: Skills for evaluation and testing
- `MIX`: Combination of development and assessment

### BehaviorType
- `STATIC`: Fixed, unchanging skill content
- `DYNAMIC`: Adaptive skill content that changes based on interaction

## Important Notes

1. **Organization Isolation**: Admin users can only access skills associated with their organization
2. **Soft Delete**: Deleted skills are archived, not permanently removed
3. **File Uploads**: Materials support base64-encoded file content for uploads
4. **Material Metadata**: Only file names and tags are returned in skill details to avoid large object loading issues
5. **Protected Skills**: Some skills may be marked as protected and cannot be deleted
6. **Automatic Association**: Created skills are automatically associated with the creator's organization

## Rate Limiting and Best Practices

- Use pagination for large skill sets (if implemented)
- Cache skill lists when possible
- Use the archived endpoint to access soft-deleted skills
- Always handle 403 responses gracefully for organization-restricted access
- Validate file sizes before base64 encoding for material uploads 