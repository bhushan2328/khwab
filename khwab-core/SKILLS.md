# Khwab Skill Framework

## Overview

Skills are modular capability providers for Khwab.

Each skill:

- Supports one or more intents
- Produces a Plan
- Never executes Android APIs directly

---

## Current Skills

| Skill | Intent | Action |
|--------|--------|--------|
| AppSkill | OPEN_APP | OPEN_APP |
| CallSkill | CALL_CONTACT | CALL_CONTACT |
| MessageSkill | SEND_MESSAGE | SEND_MESSAGE |
| MediaSkill | PLAY_MEDIA | PLAY_MEDIA |
| SearchSkill | SEARCH_WEB | SEARCH_WEB |
| SettingsSkill | CHANGE_SETTING | CHANGE_SETTING |

---

## Architecture

BrainV2

↓

SkillManager

↓

SkillRegistry

↓

Skill

↓

Plan

↓

ActionType

↓

Executor (Sprint 18)

---

## Adding a New Skill

1. Create Skill class
2. Implement Skill interface
3. Register in SkillRegistry
4. Add tests
5. Build
6. Run tests

