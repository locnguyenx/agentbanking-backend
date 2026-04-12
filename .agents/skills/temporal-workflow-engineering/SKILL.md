---
name: temporal-activity-proxy-pattern
description: Use when implementing Temporal workflow activities in Java Spring Boot - activities must be called through Workflow.newActivityStub() proxy, NOT Spring beans
---

# Temporal Activity Proxy Pattern

## Overview

In Temporal workflows with Spring Boot, activities MUST be invoked through Temporal's activity proxy, not Spring bean injection. Otherwise, no activity events are recorded in workflow history.

## The Problem

Activities injected via `@Autowired` are called directly, bypassing Temporal's recording:

```java
// ❌ WRONG: Spring bean injection bypasses Temporal
@Autowired
public WorkflowImpl(ActivityClass activity) {
    this.activity = activity;
}
```

Result: Only 5 events in history (WorkflowStarted, TaskScheduled, TaskStarted, TaskCompleted, WorkflowCompleted)

## The Solution

Use `Workflow.newActivityStub()` in no-arg constructor:

```java
// ✅ CORRECT: Temporal proxy records activities
public WorkflowImpl() {
    this.activity = Workflow.newActivityStub(ActivityClass.class, 
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .build());
}
```

Result: ActivityTaskScheduled/Started/Completed events now appear in history.

## When to Use

- Any `@WorkflowImpl` class in `infrastructure/temporal/WorkflowImpl/`
- Keep both constructors: Spring injection for DI + no-arg for Temporal proxy

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| `@Component` on workflow | Remove, use `@WorkflowImpl` only |
| `@Autowired` on constructor | Remove, use proxy pattern |
| `final` fields | Change to non-final for proxy assignment |

## Quick Reference

```java
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public WorkflowImpl() {
    this.activity = Workflow.newActivityStub(
        ActivityClass.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(X))
            .build()
    );
}
```
