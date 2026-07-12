# KHWAB3 Class Mapping

**Version:** 1.0
**Sprint:** 14
**Purpose:** Map khwab-core components to Khwab3 components for safe integration.

---

# Integration Strategy

Goal:

Move all reusable intelligence into **khwab-core** while keeping all Android-specific functionality inside **Khwab3**.

---

# Module Mapping

| Khwab3 Component | Action | Replacement / Destination |
|------------------|--------|---------------------------|
| VoiceService | KEEP | Android voice service |
| SherpaManager | KEEP | Speech recognition |
| SherpaEngine | KEEP | Offline STT |
| AccessibilityService | KEEP | Android accessibility |
| FloatingWindow | KEEP | Android overlay |
| FloatingMic | KEEP | Android overlay |
| NotificationHelper | KEEP | Android notification |
| PermissionManager | KEEP | Android permissions |
| IntentParser | REMOVE | khwab-core.parser.IntentParser |
| Brain | REMOVE | khwab-core.brain.Brain |
| Planner | REMOVE | khwab-core.planner.Planner |
| RuleEngine | REMOVE | khwab-core.planner.RuleEngine |
| ResponseGenerator | REMOVE | khwab-core.response.ResponseGenerator |
| ConversationMemory | REMOVE | khwab-core.conversation.ConversationMemory |
| ContextResolver | REMOVE | khwab-core.conversation.ContextResolver |
| ReferenceResolver | REMOVE | khwab-core.conversation.ReferenceResolver |
| Knowledge Engine | REMOVE | khwab-core.knowledge |
| AndroidCommandExecutor | CREATE | Khwab3 executor layer |

---

# khwab-core Ownership

The following modules belong ONLY inside khwab-core.

- Brain
- Intent Parser
- Planner
- Rule Engine
- Validator
- Response Generator
- Knowledge Engine
- Conversation Memory
- Context Resolver
- Reference Resolver
- Command Executor Interface
- MockCommandExecutor

---

# Khwab3 Ownership

The following modules belong ONLY inside Khwab3.

- Compose UI
- VoiceService
- Sherpa
- Accessibility Service
- Floating UI
- Notification System
- Android Permissions
- AndroidCommandExecutor

---

# AndroidCommandExecutor Responsibilities

Responsible for executing Android actions.

Examples:

- Open App
- Close App
- Call Contact
- Send SMS
- Open Settings
- Toggle Wi-Fi
- Toggle Bluetooth
- Flashlight
- Volume Control
- Accessibility Actions

---

# Final Runtime Architecture

User

↓

Sherpa

↓

Voice Text

↓

Brain.process()

↓

BrainResult

↓

Plan

↓

AndroidCommandExecutor

↓

Android Framework

↓

Application Response

---

# Merge Checklist

## Phase 1

- [ ] Import khwab-core

## Phase 2

- [ ] Remove duplicate Brain
- [ ] Remove duplicate Parser
- [ ] Remove duplicate Planner
- [ ] Remove duplicate Rule Engine

## Phase 3

- [ ] Connect Sherpa → Brain

## Phase 4

- [ ] Create AndroidCommandExecutor

## Phase 5

- [ ] Connect Compose UI

## Phase 6

- [ ] Verify Conversation Memory

## Phase 7

- [ ] Verify Reference Resolution

## Phase 8

- [ ] Run Automated Tests

## Phase 9

- [ ] Build APK

## Phase 10

- [ ] End-to-End Offline Voice Test

---

# Success Criteria

The migration is complete when:

Sherpa

↓

Brain.process()

↓

Planner

↓

AndroidCommandExecutor

↓

Android Framework

↓

Android Action

works completely offline.

---

# Expected Benefits

- Single AI engine
- No duplicated logic
- Easier maintenance
- Faster feature development
- Cleaner architecture
- Better testing
- Reusable core library
- Platform independence

