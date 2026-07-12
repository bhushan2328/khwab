# Architecture

Brain
│
▼
Plan
│
▼
ExecutionContext
│
▼
ExecutionEngine
│
▼
StepExecutor
│
▼
ExecutionResult

## Design Principles

- Brain never executes actions.
- ExecutionEngine coordinates workflows.
- StepExecutor performs individual steps.
- Platform implementations remain outside khwab-core.
