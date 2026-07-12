# Khwab3 Integration Plan

**Version:** 1.0
**Status:** Sprint 14
**Target:** Integrate `khwab-core` v0.4.0 into Khwab3

---

# Objective

Merge the reusable AI engine (`khwab-core`) into the Android application (`Khwab3`) without duplicating business logic.

---

# Architecture

                    Khwab3
                       │
      ┌────────────────┴────────────────┐
      │                                 │
      ▼                                 ▼
 Android UI                      Voice Service
      │                                 │
      └──────────────┬──────────────────┘
                     ▼
                  Sherpa
                     │
             Speech Recognition
                     │
                     ▼
            Brain.process(command)
                     │
                     ▼
                BrainResult
                     │
                     ▼
         AndroidCommandExecutor
                     │
                     ▼
          Android Framework APIs

---

# Responsibilities

## khwab-core

Responsible for:

- Brain
- Intent Parser
- Planner
- Rule Engine
- Response Generator
- Knowledge Engine
- Conversation Memory
- Context Resolver
- Reference Resolver

Never depends on Android.

---

## Khwab3

Responsible for:

- Compose UI
- Voice Service
- Sherpa Speech Recognition
- Accessibility Service
- Floating UI
- Notifications
- Permissions
- AndroidCommandExecutor

Never contains AI decision logic.

---

# Module Mapping

| khwab-core | Khwab3 |
|------------|---------|
| Brain | VoiceService |
| IntentParser | Sherpa callback |
| Planner | AndroidCommandExecutor |
| Conversation | Accessibility workflow |
| Knowledge | UI display |

---

# Merge Order

## Phase 1

Import khwab-core

Status:
Pending

---

## Phase 2

Remove duplicate parser logic

Status:
Pending

---

## Phase 3

Connect Sherpa to Brain

Status:
Pending

Pipeline

Sherpa

↓

Brain.process()

↓

BrainResult

---

## Phase 4

Implement AndroidCommandExecutor

Status:
Pending

Responsible for

- Open App
- Call Contact
- Send SMS
- Launch Settings
- Accessibility Actions

---

## Phase 5

Connect UI

Status:
Pending

Compose

↓

Brain

↓

Executor

↓

Response

---

## Phase 6

Conversation Integration

Status:
Pending

ConversationMemory

↓

ReferenceResolver

↓

ContextResolver

---

## Phase 7

Offline End-to-End Demo

Status:
Pending

Voice

↓

Sherpa

↓

Brain

↓

Executor

↓

Android

---

# Validation Checklist

## Build

- [ ] Gradle Build Successful
- [ ] Tests Successful

---

## Voice

- [ ] Speech Recognition

---

## Brain

- [ ] Intent Detection
- [ ] Planning
- [ ] Response Generation

---

## Android

- [ ] Open Apps
- [ ] Calls
- [ ] SMS
- [ ] Settings
- [ ] Accessibility

---

# Risks

Duplicate parser implementation

Mitigation

Use Brain from khwab-core only.

---

Android dependency inside khwab-core

Mitigation

Keep Android code only in AndroidCommandExecutor.

---

Sherpa integration

Mitigation

Keep Sherpa responsible only for Speech-to-Text.

---

# Release Plan

v0.5.0

- Android integration

v0.6.0

- Voice pipeline

v0.7.0

- Accessibility automation

v0.8.0

- Offline assistant

v0.9.0

- Beta

v1.0.0

- Stable Release

---

# Success Criteria

The integration is complete when:

Sherpa

↓

Brain.process()

↓

Planner

↓

AndroidCommandExecutor

↓

Android Action

works completely offline.
