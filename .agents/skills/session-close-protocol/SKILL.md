---
name: session-close-protocol
description: Use when finishing a session. The Brain. Core protocols for writing, and maintaining the session's Memory
---

# Operational Protocols
This skill defines the core operational rules.

## 1. Date Verification (CRITICAL)
Before modifying ANY documentation file (Session Memory, Docs, Skills):
1.  
**Check System Date:**
 Run `date` or check system time tool.
2.  
**Update Metadata:**
 Always update `**Last Update:** [YYYY-MM-DD]` fields.
3.  
**NEVER ASSUME DATES.**


## 2. Session Closing Protocol
When the user says "finish session" or similar:
1.  
**Update `current-state.md`**
: Reflect the latest status.
2.  
**Update `progress.md`**
: Log completed milestones.
3.  
**Update `NOTES_NEXT_SESSION.md`**
: Write clear instructions for the "next you".

If YOU (the Agent) don't know the location of these files, refer to @.agents/skills/session-memory-protocol/SKILL.md

4.  
**Cleanup**
: Remove temp files or logs.
5. Do **Self-Improvement**

# 🚀 Self-Improvement Directive (The "Gardener")

You are responsible for maintaining and evolving your own Skills.
When you discover a new pattern, solution, finding, lesson learned or rule:
1.  
**Identify the relevant Skill:**
 (e.g., `temporal-engineering`, `spring-stack-engineering`, `test-architecture` for a Drizzle pattern).
2.  
**Update the `SKILL.md`:**
 Add the knowledge directly to the file.
3.  
**Refactor:**
 If a Skill becomes too large, propose splitting it.
4.  
**Create:**
 Only create a NEW Skill folder if the knowledge is truly domain-distinct (e.g., Java Spring Development, Workflow Processing, Temporal, Testing, Banking, docker, architecture,...).

**DO NOT create loose files for rules. Curate your `.agents/skills` folder.**