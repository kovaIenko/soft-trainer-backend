# 🚀 Modern Flow Rules Schema - Migration Guide

## Overview

This document shows how to migrate from the legacy `show_predicate` system to modern, structured flow rules that are maintainable, testable, and transparent.

## 📊 **Legacy vs Modern Comparison**

### ❌ Legacy show_predicate (Hard to maintain)
```java
// Complex, opaque predicate string
"message whereId \"5\" and message.selected[] == [1] and saveChatValue[\"empathy\",readChatValue[\"empathy\"]+1] and saveChatValue[\"motivation for change\",readChatValue[\"motivation for change\"]+1] and saveChatValue[\"correctness of actions\",readChatValue[\"correctness of actions\"]+1]"
```

### ✅ Modern flow_rules (Clean and clear)
```json
{
  "flow_rules": [
    {
      "type": "user_response",
      "rule_id": "check_response_5_option_1",
      "message_id": 5,
      "expected_options": [1],
      "match_type": "EXACT_MATCH",
      "description": "User selected option 1 in message 5"
    }
  ],
  "hyperparameter_actions": [
    {
      "type": "INCREMENT",
      "parameter": "empathy",
      "value": 1.0,
      "description": "Increase empathy score"
    },
    {
      "type": "INCREMENT", 
      "parameter": "motivation for change",
      "value": 1.0,
      "description": "Increase motivation score"
    },
    {
      "type": "INCREMENT",
      "parameter": "correctness of actions", 
      "value": 1.0,
      "description": "Increase correctness score"
    }
  ]
}
```

## 🎯 **Flow Rules Types**

### 1. User Response Rules
```json
{
  "type": "user_response",
  "rule_id": "unique_rule_id",
  "message_id": 5,
  "expected_options": [1, 3],
  "match_type": "CONTAINS_ANY",
  "description": "User selected option 1 or 3"
}
```

**Match Types:**
- `EXACT_MATCH`: User selected exactly these options
- `CONTAINS_ANY`: User selected at least one of these options  
- `CONTAINS_ALL`: User selected all of these options
- `NOT_CONTAINS`: User did not select any of these options

### 2. Message Count Rules
```json
{
  "type": "message_count",
  "rule_id": "check_message_limit",
  "operator": "GREATER_THAN",
  "threshold": 10,
  "description": "More than 10 messages exchanged"
}
```

### 3. Hyperparameter Rules
```json
{
  "type": "hyperparameter",
  "rule_id": "check_empathy_level", 
  "parameter": "empathy",
  "operator": "GREATER_OR_EQUAL",
  "value": 5.0,
  "description": "Empathy level is at least 5"
}
```

## 📊 **Hyperparameter Actions**

### 1. Set Value
```json
{
  "type": "SET",
  "parameter": "empathy",
  "value": 8.0,
  "description": "Set empathy to 8.0"
}
```

### 2. Increment/Decrement
```json
{
  "type": "INCREMENT",
  "parameter": "engagement", 
  "value": 1.0,
  "description": "Increase engagement by 1"
}
```

### 3. Conditional Actions
```json
{
  "type": "INCREMENT",
  "parameter": "feedback_balance",
  "value": 2.0,
  "condition": {
    "type": "hyperparameter",
    "parameter": "empathy",
    "operator": "GREATER_THAN", 
    "value": 3.0
  },
  "description": "Increase feedback balance if empathy > 3"
}
```

## 🔄 **Migration Examples**

### Example 1: Simple Response Check
**Legacy:**
```java
"message whereId \"60\" and message.selected[] == [3]"
```

**Modern:**
```json
{
  "flow_rules": [
    {
      "type": "user_response",
      "rule_id": "check_message_60_option_3",
      "message_id": 60,
      "expected_options": [3],
      "match_type": "EXACT_MATCH"
    }
  ]
}
```

### Example 2: Complex Hyperparameter Logic
**Legacy:**
```java
"message whereId \"60\" and message.selected[] == [3] and saveChatValue[\"active listening\",readChatValue[\"active listening\"]+1] and saveChatValue[\"joint decision making\",readChatValue[\"joint decision making\"]+1] and saveChatValue[\"engagement\",readChatValue[\"engagement\"]+1]"
```

**Modern:**
```json
{
  "flow_rules": [
    {
      "type": "user_response",
      "rule_id": "positive_feedback_response",
      "message_id": 60,
      "expected_options": [3],
      "match_type": "EXACT_MATCH",
      "description": "User chose positive feedback approach"
    }
  ],
  "hyperparameter_actions": [
    {
      "type": "INCREMENT",
      "parameter": "active listening",
      "value": 1.0,
      "description": "Reward active listening"
    },
    {
      "type": "INCREMENT", 
      "parameter": "joint decision making",
      "value": 1.0,
      "description": "Reward collaborative approach"
    },
    {
      "type": "INCREMENT",
      "parameter": "engagement",
      "value": 1.0,
      "description": "Reward engagement"
    }
  ]
}
```

### Example 3: Multiple Conditions (OR Logic)
**Legacy:**
```java
"message whereId \"140\" and (message.selected[] == [3] or message.selected[] == [4] or message.selected[] == [5])"
```

**Modern:**
```json
{
  "flow_rules": [
    {
      "type": "user_response",
      "rule_id": "constructive_responses",
      "message_id": 140,
      "expected_options": [3, 4, 5], 
      "match_type": "CONTAINS_ANY",
      "description": "User chose any constructive response"
    }
  ]
}
```

## 🔧 **EnhancedFlowNode Schema**

### Complete Node Example
```json
{
  "message_id": 90,
  "previous_message_ids": [65],
  "message_type": "Text",
  "interaction_type": "DISPLAY",
  "text": "Great start: you acknowledge strengths and invite dialogue.",
  "character_id": 1,
  
  "flow_rules": [
    {
      "type": "user_response",
      "rule_id": "positive_approach_check",
      "message_id": 60,
      "expected_options": [3],
      "match_type": "EXACT_MATCH",
      "description": "User took positive approach"
    }
  ],
  
  "hyperparameter_actions": [
    {
      "type": "INCREMENT",
      "parameter": "active listening",
      "value": 1.0
    },
    {
      "type": "INCREMENT", 
      "parameter": "joint decision making",
      "value": 1.0
    },
    {
      "type": "INCREMENT",
      "parameter": "engagement", 
      "value": 1.0
    }
  ],
  
  "show_predicate": null
}
```

## 🚀 **Benefits of Modern System**

### 1. **Transparency**
- Each rule has a clear description
- Easy to understand what each condition does
- No more "black box" logic

### 2. **Testability**
- Individual rules can be unit tested
- Clear input/output relationships
- Mockable components

### 3. **Maintainability**
- JSON schema validation
- Version control friendly
- Easy to modify without breaking syntax

### 4. **Debugging**
- Each rule evaluation is logged
- Clear failure points
- Traceable execution flow

### 5. **Reusability**
- Rules can be shared across simulations
- Template-based rule creation
- Composable rule sets

## 🔄 **Migration Strategy**

### Phase 1: Compatibility Layer (Current)
- Both systems work side-by-side
- Legacy `show_predicate` still supported
- New simulations use modern rules

### Phase 2: Gradual Migration
- Convert high-priority simulations first
- Use automated migration tools where possible
- Test thoroughly before production

### Phase 3: Legacy Deprecation
- Mark `show_predicate` as deprecated
- Provide migration warnings
- Eventually remove legacy system

## 🛠️ **Development Tools**

### Rule Builder Utility (Future)
```java
FlowRuleBuilder.create()
    .userResponse("message_60")
    .expectsOptions(1, 3)
    .withMatchType(CONTAINS_ANY)
    .incrementHyperParameter("empathy", 1.0)
    .incrementHyperParameter("engagement", 1.0)
    .build();
```

### Validation Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "flow_rules": {
      "type": "array",
      "items": { "$ref": "#/definitions/FlowRule" }
    },
    "hyperparameter_actions": {
      "type": "array", 
      "items": { "$ref": "#/definitions/HyperParameterAction" }
    }
  }
}
```

---

This modern system provides the foundation for clean, maintainable, and scalable simulation flows while preserving full backward compatibility with your existing content. 