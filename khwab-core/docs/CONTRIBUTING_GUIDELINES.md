# Contributing Guidelines

Version: 1.0

---

# Purpose

This document defines the development standards for the Khwab project.

Every new feature, bug fix, and module should follow these guidelines.

---

# Development Principles

- Offline First
- Clean Architecture
- Modular Design
- Platform Independence
- Test Before Merge
- Documentation First
- Small Incremental Changes

---

# Coding Standards

## Kotlin

- Use meaningful class names.
- Use descriptive function names.
- Keep functions small and focused.
- Prefer immutable data where possible.
- Avoid duplicate logic.

---

# Architecture Rules

Business logic belongs in:

- khwab-core

Android-specific logic belongs in:

- Khwab3

Never import Android SDK into khwab-core.

---

# Package Structure

khwab-core

- brain
- parser
- planner
- validator
- knowledge
- conversation
- executor
- response
- model

Khwab3

- ui
- service
- accessibility
- overlay
- notification
- speech
- executor

---

# Development Workflow

1. Design
2. Implement
3. Build
4. Test
5. Update Documentation
6. Commit
7. Push

---

# Commit Message Format

Examples

feat: add Conversation Memory

fix: planner null pointer

docs: update architecture

test: add Brain tests

refactor: simplify parser

---

# Pull Request Checklist

- Code builds successfully.
- Tests pass.
- Documentation updated.
- No duplicate logic introduced.
- Architecture rules respected.

---

# Documentation

Every major feature should include:

- Design
- Implementation
- Tests
- Changelog update

---

# Versioning

Major

Architecture changes

Minor

New features

Patch

Bug fixes

---

# Goal

Maintain a clean, scalable, and maintainable offline AI assistant platform.

