# Android Command Executor Design

**Version:** 1.0

**Sprint:** 14

**Status:** Design

---

# Purpose

AndroidCommandExecutor is responsible for executing all Android-specific actions requested by khwab-core.

The Brain decides **what** to do.

AndroidCommandExecutor decides **how** to do it on Android.

---

# Architecture

Voice

↓

Sherpa

↓

Speech Text

↓

Brain.process()

↓

Plan

↓

AndroidCommandExecutor

↓

Android Framework

↓

Result

---

# Responsibilities

AndroidCommandExecutor SHALL:

- Execute Android actions
- Never perform AI reasoning
- Never parse user commands
- Never determine intent
- Only execute PlanSteps

---

# Supported Actions

## Applications

- Open App
- Close App
- Switch App
- Recent Apps

---

## Communication

- Call Contact
- Dial Number
- Send SMS
- Open Messages
- Open Email

---

## Media

- Play Music
- Pause Music
- Next Track
- Previous Track
- Increase Volume
- Decrease Volume

---

## Device

- Flashlight
- Wi-Fi
- Bluetooth
- Mobile Data
- Airplane Mode (where permitted)
- Brightness
- Volume

---

## Settings

- Open Wi-Fi Settings
- Open Bluetooth Settings
- Open Accessibility
- Open Display Settings
- Open Security Settings

---

## Navigation

- Open Maps
- Navigate Home
- Navigate to Contact

---

## Accessibility

- Scroll
- Click
- Back
- Home
- Recent Apps
- Notification Panel
- Quick Settings

---

# Android APIs

| Action | Android API |
|---------|-------------|
| Launch App | PackageManager |
| Phone Call | Intent.ACTION_CALL |
| SMS | Intent.ACTION_SENDTO |
| Settings | Intent.ACTION_SETTINGS |
| Media | MediaSession |
| Accessibility | AccessibilityService |
| Notifications | NotificationManager |
| Volume | AudioManager |
| Brightness | Settings.System |

---

# Required Permissions

- RECORD_AUDIO
- CALL_PHONE
- POST_NOTIFICATIONS
- SYSTEM_ALERT_WINDOW
- BIND_ACCESSIBILITY_SERVICE
- QUERY_ALL_PACKAGES
- READ_CONTACTS

Additional permissions may be added as new capabilities are implemented.

---

# Execution Pipeline

User

↓

Brain

↓

Plan

↓

PlanStep

↓

AndroidCommandExecutor

↓

ExecutionResult

---

# Execution Result

Each execution should return:

- Success
- Failure
- Message
- Error (optional)

Example

Success

Message

Opened WhatsApp

---

# Error Handling

Examples

- App not installed
- Permission denied
- Accessibility disabled
- Contact not found
- Android API unavailable

Executor must return structured errors without crashing.

---

# Design Rules

- No AI logic
- No NLP
- No intent detection
- No planning
- No conversation memory

Executor only executes plans.

---

# Future Extensions

- Android Auto
- Wear OS
- Smart Home
- Bluetooth Devices
- Smart TV
- IoT
- Plugin Executors

---

# Integration

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

Android Response

---

# Success Criteria

AndroidCommandExecutor is complete when it can:

- Open applications
- Call contacts
- Send SMS
- Open Android settings
- Execute accessibility actions
- Return ExecutionResult
- Work completely offline

